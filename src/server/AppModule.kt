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


import com.github.rwsbillyang.ktorKit.ApiJson
import com.github.rwsbillyang.ktorKit.cache.CaffeineCache
import com.github.rwsbillyang.ktorKit.cache.ICache
import com.github.rwsbillyang.ktorKit.db.DatabaseType
import com.github.rwsbillyang.ktorKit.db.DbConfig

//import com.github.rwsbillyang.ktorKit.db.MongoDataSource
import com.github.rwsbillyang.ktorKit.db.SqlDataSource

import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.conditionalheaders.ConditionalHeaders
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.forwardedheaders.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout


import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.seconds


/**
 * @param modules 需要注入的实例的模块列表
 * @param dbName 数据库名称，模块默认提供，为null，则不注入DataSource；installAppModule若提供时将覆盖它
 * @param routing route api
 * */
class AppModule(
    val modules: List<Module>?,
    var dbName: String?,
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
 * @param dbType DbType.NOSQL, DbType.SQL
 * @param userName 连接数据的用户名，mysql通常需要赋值
 * @param pwd 连接数据的密码，mysql通常需要赋值
 * @param dbName 数据库名称，不指定则使用AppModule中的默认名称
 * @param host 数据库host 默认127.0.0.1
 * @param port 数据库port 0: 对于NOSQL MongoDB，默认27017， SQL之MySQL为3306
 * */
fun Application.installModule(
    app: AppModule,
    dbName: String? = null,
    dbType: DatabaseType = DatabaseType.SQL_MYSQL,
    userName: String? = null,
    pwd: String? = null,
    host: String = "127.0.0.1",
    port: Int = 0
) {
    dbName?.let { app.dbName = it }
    app.dbName?.let {
        _dbConfigSet.add(DbConfig(it, dbType, host, port, userName, pwd))
    }

    app.modules?.let { _MyKoinModules.plusAssign(it) }
    app.routing?.let { _MyRoutings.plusAssign(it) }
}


/**
 * @param enableJwt 为false时只适合于route中无authentication时的情况
 * @param enableJsonApi 是否打开api接口json序列化
 * @param jsonBuilderAction 添加额外的自定义json配置，通常用于添加自己的json contextual
 * @param enableWebSocket 是否开启websocket
 * @param logHeaders 需要输出哪些请求头，用于调试
 * @param cache 自动注入 CaffeineCache，如不需要可使用VoidCache代替
 * */
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.defaultInstall(
    enableJwt: Boolean = true,
    enableJsonApi: Boolean = true,
    jsonBuilderAction: (JsonBuilder.() -> Unit)? = null,
    enableWebSocket: Boolean = false,
    logHeaders: List<String>? = null, //"X-Auth-uId","X-Auth-UserId", "X-Auth-ExternalUserId", "X-Auth-oId", "X-Auth-unId","X-Auth-CorpId","Authorization"
    cache: ICache = CaffeineCache()
) {
    val module = module {
        single<ICache> { cache }
        _dbConfigSet.forEach {
            val config = it

            when(val dbType = it.dbType){
                DatabaseType.NOSQL ->  TODO("update MongoDataSource")//single(named(it.dbName)) { MongoDataSource(config.dbName, config.host, config.port) }
                else -> single(named(it.dbName)) { SqlDataSource(dbType, config.dbName, config.userName, config.pwd, config.host, config.port) }
            }
        }
        _dbConfigSet.clear()
    }

    _MyKoinModules.add(0, module)
    install(Koin) {
        modules(_MyKoinModules)
    }
    _MyKoinModules.clear()
    //log.info("_MyKoinModules.size=${_MyKoinModules.size}")

    install(AutoHeadResponse)
    install(ForwardedHeaders)
    install(XForwardedHeaders)
    install(PartialContent)
    install(ConditionalHeaders)
    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }

    install(CachingHeaders) {
        options { call, outgoingContent ->
            when (outgoingContent.contentType?.withoutParameters()) {
                ContentType.Text.CSS,ContentType.Text.JavaScript  -> io.ktor.http.content.CachingOptions(
                    CacheControl.MaxAge(maxAgeSeconds = 30 * 24 * 60 * 60)
                )
                else -> null
            }
        }
    }
    
    install(CallLogging) {
        level = Level.INFO
        //filter { call -> call.request.path().startsWith("/") }
        if (!logHeaders.isNullOrEmpty()) {
            format { call ->
                "${call.request.httpMethod.value} ${call.request.uri}  ${call.authHeaders(logHeaders)} -> ${call.response.status()}"
            }
        }else{
            format { call ->
                "${call.request.httpMethod.value} ${call.request.uri}  -> ${call.response.status()}"
            }
        }
    }


    if(enableJsonApi){
        //https://ktor.io/servers/features/content-negotiation/serialization-converter.html
        //https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/custom_serializers.md
        install(ContentNegotiation) {
            json(
                json = if (jsonBuilderAction == null) ApiJson.serverSerializeJson() else Json(ApiJson.serverSerializeJson(), jsonBuilderAction),
                contentType = ContentType.Application.Json
            )
        }
    }



    //install(Locations)
    install(Resources)

    if(enableJwt)
    {
        val jwtHelper: AbstractJwtHelper by inject()
        install(Authentication) {
            jwt {
                verifier(jwtHelper.getVerifier()) //Configure a token verifier
                this.realm = jwtHelper.realm
                validate { credential -> jwtHelper.validate(credential) } // Validate JWT payload
            }
        }
    }

    if(enableWebSocket){
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
//            extensions {
//                install(WebSocketDeflateExtension) {
//                    //Compression level to use for [java.util.zip.Deflater].
//                    compressionLevel = Deflater.DEFAULT_COMPRESSION
//
//                    //Prevent to compress small outgoing frames.
//                    compressIfBiggerThan(bytes = 4 * 1024)
//                }
//            }

            pingPeriod = 15.seconds
            timeout = 200.seconds
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

    }

    _MyRoutings.add {
        get("/ok") {
            call.respondText("OK", contentType = ContentType.Text.Plain)
        }
    }
//    _MyRoutings.add {
//        exceptionPage(application)
//    }
    log.info("_MyRoutings.size=${_MyRoutings.size}")

    _MyRoutings.forEach {
        routing {
            it()
        }
    }
    _MyRoutings.clear()
}


/**
 * @param backOfNginx true if ktor server is back of nginx
 * */
fun Application.installCORS(backOfNginx: Boolean) {
    if(backOfNginx){
        install(CORS){
            anyHost()
            allowMethod(HttpMethod.Options)

            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)

            allowNonSimpleContentTypes = true
            allowHeadersPrefixed("X-")
            allowCredentials = true
            maxAgeInSeconds = 3600
        }
    }else{
        install(CORS){
            anyHost()

            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Patch)
            allowMethod(HttpMethod.Delete)

            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.Accept)
            allowHeader(HttpHeaders.AcceptLanguage)
            allowHeader(HttpHeaders.AcceptEncoding)
            //allowHeader(HttpHeaders.AcceptCharset)
            allowHeader(HttpHeaders.Connection)

            allowNonSimpleContentTypes = true
            allowHeadersPrefixed("X-")
            allowHeadersPrefixed("Access-Control")
            allowHeadersPrefixed("Sec-Fetch")

            allowCredentials = true
            maxAgeInSeconds = 3600


            //anyHost() // @TODO: Don't do this in production if possible. Try to limit it.

//            exposeHeader("Access-Control-Allow-Origin *")
//            exposeHeader("Access-Control-Allow-Methods GET,POST,OPTIONS,PUT,DELETE")
//            exposeHeader("Access-Control-Allow-Credentials true")
//            exposeHeader("Access-Control-Allow-Headers DNT,accessToken,uuid,Authorization,Accept,Accept-Language,Content-Language,Last-Event-ID,Origin,Keep-Alive,User-Agent,X-Mx-ReqToken,X-Data-Type,X-Auth-Token,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range")

        }
    }
}

fun Application.testModule(module: AppModule) {
    val app = this
    installModule(AppModule(
        listOf(module(createdAtStart = true) {
            single<UserInfoJwtHelper> { TestJwtHelper() }
            single<AbstractJwtHelper> { DevJwtHelper() }
            single<Application> { app }
        }), null))
    installModule(module)
    defaultInstall(true)
}

@Suppress("unused") // Referenced in application.conf
fun Application.simpleTestableModule() {
    routing {
        get("/ok") {
            call.respondText("OK", contentType = ContentType.Text.Plain)
        }
    }
}
