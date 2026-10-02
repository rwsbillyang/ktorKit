package com.github.rwsbillyang.ktorKit.server


import com.github.rwsbillyang.ktorKit.ApiJson
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.conditionalheaders.ConditionalHeaders
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.resources.Resources
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import org.koin.ktor.ext.inject
import org.slf4j.event.Level
import kotlin.getValue
import kotlin.time.Duration.Companion.seconds


/**
 * 安全安装单个插件（编译时已知类型）
 */
private fun Application.safeInstall(pluginName: String, installer: () -> Unit) {
    try {
        installer()
        log.info("✅ Auto-installed plugin: $pluginName")
    } catch (t: Throwable) {
        // 捕获 Throwable 以覆盖 ClassNotFoundException / NoClassDefFoundError / IllegalStateException 等
        log.debug("Plugin $pluginName skipped")
    }
}

/**
 *     若添加了依赖库，自动install下面这些plugin
 *     install(Resources)
 *     install(AutoHeadResponse)
 *     install(ForwardedHeaders)
 *     install(XForwardedHeaders)
 *     install(PartialContent)
 *     install(ConditionalHeaders)
 *     install(DefaultHeaders)
 *     CallLogging, CachingHeaders,Negotiation,Websockets,JWT
 * */
internal fun Application.installOptionalKtorPlugins(
    logHeaders: List<String>? = null, //"X-Auth-uId","X-Auth-UserId", "X-Auth-ExternalUserId", "X-Auth-oId", "X-Auth-unId","X-Auth-CorpId","Authorization"
    jsonBuilderAction: (JsonBuilder.() -> Unit)? = null
) {
    safeInstall("AutoHeadResponse") { install(AutoHeadResponse) }
    safeInstall("ForwardedHeaders") { install(ForwardedHeaders) }
    safeInstall("XForwardedHeaders") { install(XForwardedHeaders) }
    safeInstall("PartialContent") { install(PartialContent) }
    safeInstall("ConditionalHeaders") { install(ConditionalHeaders) }
    safeInstall("DefaultHeaders") { install(DefaultHeaders) }
    safeInstall("Resources") { install(Resources) }

    // 如果需要配置，也可以直接写：
    // safeInstall("ContentNegotiation") {
    //     install(ContentNegotiation) { json() }
    // }
    safeInstall("CallLogging") { installCallLogging(logHeaders) }
    safeInstall("CachingHeaders") { installCachingHeaders()}
    safeInstall("Negotiation") { installContentNegotiation(jsonBuilderAction) }
    safeInstall("Websockets") { installWebsockets() }
    safeInstall("JWT") { installJwt() }
}


/**
 * @param logHeaders: eg. "X-Auth-uId","X-Auth-UserId", "X-Auth-ExternalUserId", "X-Auth-oId", "X-Auth-unId","X-Auth-CorpId","Authorization"
 * */
fun Application.installCallLogging(logHeaders: List<String>? = null,){
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
}

fun Application.installCachingHeaders(){
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
}

fun Application.installContentNegotiation(jsonBuilderAction: (JsonBuilder.() -> Unit)? = null){
    //https://ktor.io/servers/features/content-negotiation/serialization-converter.html
    //https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/custom_serializers.md
    install(ContentNegotiation) {
        json(
            json = if (jsonBuilderAction == null) ApiJson.serverSerializeJson() else Json(ApiJson.serverSerializeJson(), jsonBuilderAction),
            contentType = ContentType.Application.Json
        )
    }
}

fun Application.installWebsockets(){
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

fun Application.installJwt(){
    val jwtHelper: AbstractJwtHelper by inject()
    install(Authentication) {
        jwt {
            verifier(jwtHelper.getVerifier()) //Configure a token verifier
            this.realm = jwtHelper.realm
            validate { credential -> jwtHelper.validate(credential) } // Validate JWT payload
        }
    }
}



// 只支持 kotlinx serialization
//fun Application.installContentNegotiationIfPresent() {
//    if (isLibraryAvailable("io.ktor.server.plugins.contentnegotiation.ContentNegotiation")) {
//        try {
//            // 因为 build.gradle 里是 compileOnly，这里可以直接引用类
//            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
//                // 进一步探测用户用了哪种序列化库
//                when {
//                    isLibraryAvailable("io.ktor.serialization.gson.gson") -> {
//                        io.ktor.serialization.gson.gson()
//                        optionalPluginLogger.info("ContentNegotiation configured with Gson")
//                    }
//                    isLibraryAvailable("io.ktor.serialization.jackson.jackson") -> {
//                        io.ktor.serialization.jackson.jackson()
//                        optionalPluginLogger.info("ContentNegotiation configured with Jackson")
//                    }
//                    isLibraryAvailable("io.ktor.serialization.kotlinx.json.json") -> {
//                        io.ktor.serialization.kotlinx.json.json()
//                        optionalPluginLogger.info("ContentNegotiation configured with Kotlinx JSON")
//                    }
//                    else -> optionalPluginLogger.warn("No serialization library found for ContentNegotiation")
//                }
//            }
//        } catch (e: Exception) {
//            optionalPluginLogger.error("Failed to install ContentNegotiation", e)
//        }
//    }
//}
//
