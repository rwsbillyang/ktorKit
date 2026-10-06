/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-08-15 22:31
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rwsbillyang.ktorKit.server


//import com.github.rwsbillyang.ktorKit.db.MongoDataSource


import com.github.rwsbillyang.ktorKit.cache.CaffeineCache
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.DatabaseType
import com.github.rwsbillyang.ktorKit.db.DbConfig
import com.github.rwsbillyang.ktorKit.db.SqlDataSource
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.JsonBuilder
import org.koin.core.context.GlobalContext.loadKoinModules
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger


/**
 * @param modules 需要注入的实例的业务模块列表
 * @param dbConfig 若为空，则不使用数据库
 * @param routing route api
 * */
class AppModule(
    val modules: List<Module>,
    var dbConfig: DbConfig?,
    val routing: (Routing.() -> Unit)? = null
)



private val _dbConfigSet = mutableSetOf<DbConfig>()
private val _MyKoinModules = mutableListOf<Module>()
private val _MyRoutings = mutableListOf<Routing.() -> Unit>()

/**
 * 安装appModule，实际完成功能是
 * （1）将待注入的koin module添加到一个私有全局列表，便于defaultInstall中进行 install(Koin)
 * （2）将routing配置加入私有全局列表，便于后面执行，添加endpoint
 * （3）自动注入了DataSource（以数据库名称作为qualifier）
 * @param app 待安装的module
 * */
fun Application.installModule(app: AppModule)
{
    app.dbConfig?.let{_dbConfigSet.add(it)}
    app.routing?.let { _MyRoutings.add(it) }

    _MyKoinModules.plusAssign(app.modules)

    //loadKoinModules(app.modules)
    //app.routing?.let { routing { it() } }
}


/**
 * 必须在所有的installModule之后调用
 *
 * 去掉了enableJwt，改为根据依赖自动添加。 为false时只适合于route中无authentication时的情况
 * 去掉了enableJsonApi，改为根据依赖自动添加。 是否打开api接口json序列化
 * @param autoInstallPlugins    若为true，自动安装一些常用plugin（若添加了依赖）；付哦为false需自行安装
 * @param logHeaders 需要输出哪些请求头，用于调试
 * @param cache 自动注入 CaffeineCache，如不需要可使用VoidCache代替
 * @param jsonBuilderAction 添加额外的自定义json配置，通常用于添加自己的json contextual
 * */
//@Suppress("unused") // Referenced in application.conf
//@kotlin.jvm.JvmOverloads
fun Application.lastInstall(
    autoInstallPlugins: Boolean,
    logHeaders: List<String>? = null, //"X-Auth-uId","X-Auth-UserId", "X-Auth-ExternalUserId", "X-Auth-oId", "X-Auth-unId","X-Auth-CorpId","Authorization"
    cache: ICache = CaffeineCache(),
    jsonBuilderAction: (JsonBuilder.() -> Unit)? = null
) {
    val module = module {
        single<ICache> { cache }
        _dbConfigSet.forEach {
            val config = it
            when(val dbType = it.dbType){
                DatabaseType.NOSQL ->  TODO("update MongoDataSource")//single(named(it.dbName)) { MongoDataSource(config.dbName, config.host, config.port) }
                //在添加到_dbConfigSet时，区别了nam额， dbType, host, port, 依赖注入标识它只用了dbName
                else -> single(named(it.dbName)) { SqlDataSource(config) }
            }
        }
        _dbConfigSet.clear()
    }

    _MyKoinModules.add(0, module)
    install(Koin) {
        slf4jLogger()
        modules(_MyKoinModules)
    }
    log.info("_MyKoinModules.size=${_MyKoinModules.size}")
    _MyKoinModules.clear()//依赖注入后清除

    if(autoInstallPlugins)
        installOptionalKtorPlugins(logHeaders, jsonBuilderAction)

    _MyRoutings.add {
        get("/ok") {
            call.respondText("OK", contentType = ContentType.Text.Plain)
        }
    }

    log.info("_MyRoutings.size=${_MyRoutings.size}")
    _MyRoutings.forEach {
        routing {
            it()
        }
    }
    _MyRoutings.clear()
}


