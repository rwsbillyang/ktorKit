package com.github.rwsbillyang.appModule



import com.github.rwsbillyang.data.DataSource
import com.github.rwsbillyang.apiJson.ApiJson
import com.github.rwsbillyang.kcache.CaffeineCache
import com.github.rwsbillyang.kcache.ICache
import com.github.rwsbillyang.ktorExt.JwtHelper
import com.github.rwsbillyang.ktorExt.TestJwtHelper
import com.github.rwsbillyang.ktorExt.config
import com.github.rwsbillyang.ktorExt.exceptionPage
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import org.slf4j.event.Level

/**
 * @param modules 需要注入的实例的模块列表
 * @param dbName 数据库名称，模块默认提供，installAppModule提供时将覆盖它
 * @param routing route api
 * */
class AppModule(
    val modules: List<Module>?,
    var dbName: String,
    val routing: (Routing.() -> Unit)? = null
)


private val _MyKoinModules = mutableListOf<Module>()
private val _MyRoutings = mutableListOf<Routing.() -> Unit>()

/**
 * 安装appModule，实际完成功能是
 * （1）将待注入的koin module添加到一个私有全局列表，便于defaultInstall中进行 install(Koin)
 * （2）将routing配置加入私有全局列表，便于后面执行，添加endpoint
 * （3）自动注入了DataSource（以数据库名称作为qualifier）和 CaffeineCache，
 * @param app 待安装的module
 * @param dbName 数据库名称，不指定则使用AppModule中的默认名称
 * @param host 数据库host 默认127.0.0.1
 * @param port 数据库port 默认27017
 * */
fun Application.installModule(
    app: AppModule,
    dbName: String? = null,
    host: String = "127.0.0.1",
    port: Int = 27017
): Application {
    dbName?.let { app.dbName = it }
    val module = module {
        single(named(app.dbName)) { DataSource(app.dbName, host, port) }
        single<ICache> { CaffeineCache() }
    }

    _MyKoinModules.add(module)

    app.modules?.let { _MyKoinModules.plus(it) }
    app.routing?.let { _MyRoutings.plus(it) }

    return this
}


/**
 * @param jsonBuilderAction 添加额外的自定义json配置，通常用于添加自己的json contextual
 * */
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.defaultInstall(
    jwtHelper: JwtHelper,
    testing: Boolean = false,
    jsonBuilderAction: (JsonBuilder.() -> Unit)? = null
) {
    install(ForwardedHeaderSupport) // WARNING: for security, do not include this if not behind a reverse proxy
    install(XForwardedHeaderSupport) // WARNING: for security, do not include this if not behind a reverse proxy

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    //https://ktor.io/servers/features/content-negotiation/serialization-converter.html
    //https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/custom_serializers.md
    install(ContentNegotiation) {
        json(
            json = if (jsonBuilderAction == null) ApiJson.json2 else Json(ApiJson.json2, jsonBuilderAction),
            contentType = ContentType.Application.Json
        )
    }


    install(Locations)

    install(Koin) {
        modules(_MyKoinModules)
    }

    //val jwtHelper: MyJwtHelper by inject()
    install(Authentication) {
        jwt {
            config(jwtHelper)
        }
    }

    if (testing) {
        _MyRoutings.add{
            get("/") {
                call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
            }
        }
    }
    _MyRoutings.add {
        exceptionPage()
    }

    _MyRoutings.forEach {
        routing(it)
    }
}


fun Application.testModule(module: AppModule) {
    installModule(module)
    defaultInstall(TestJwtHelper(), true)
}