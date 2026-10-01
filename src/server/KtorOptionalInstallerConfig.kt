package com.github.rwsbillyang.ktorKit.server

import com.github.rwsbillyang.ktorKit.ApiJson
import com.github.rwsbillyang.ktorKit.util.isLibraryAvailable
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt

import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.WebSockets

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import org.koin.ktor.ext.inject
import org.slf4j.event.Level
import kotlin.getValue
import kotlin.time.Duration.Companion.seconds

/**
 * 带配置可选安装：CallLogging，CachingHeaders，ContentNegotiation，WebSockets，jwt，CORS
 * */
fun Application.installOptionalKtorPluginsWithConfig(
    logHeaders: List<String>? = null, //"X-Auth-uId","X-Auth-UserId", "X-Auth-ExternalUserId", "X-Auth-oId", "X-Auth-unId","X-Auth-CorpId","Authorization"
    jsonBuilderAction: (JsonBuilder.() -> Unit)? = null,
    backOfNginx: Boolean
    )
{
    if(isLibraryAvailable("io.ktor.server.plugins.calllogging.CallLogging"))
    {
        installCallLogging(logHeaders)
    }

    if(isLibraryAvailable("io.ktor.server.plugins.cachingheaders.CachingHeaders"))
    {
        installCachingHeaders()
    }


    if(isLibraryAvailable("io.ktor.server.plugins.contentnegotiation.ContentNegotiation"))
    {
        installContentNegotiation(jsonBuilderAction)
    }

    if(isLibraryAvailable("io.ktor.server.websocket.WebSockets"))
    {
        installWebsockets()
    }


    if(isLibraryAvailable("io.ktor.server.auth.jwt.jwt"))
    {
        installJwt()
     }

    if(isLibraryAvailable("io.ktor.server.plugins.cors.routing.CORS")){
        installCORS(backOfNginx)
    }

}


/**
 * @param logHeaders: eg. "X-Auth-uId","X-Auth-UserId", "X-Auth-ExternalUserId", "X-Auth-oId", "X-Auth-unId","X-Auth-CorpId","Authorization"
 * */
private  fun Application.installCallLogging(logHeaders: List<String>? = null,){
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

private  fun Application.installCachingHeaders(){
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

private fun Application.installContentNegotiation(jsonBuilderAction: (JsonBuilder.() -> Unit)? = null){
    //https://ktor.io/servers/features/content-negotiation/serialization-converter.html
    //https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/custom_serializers.md
    install(ContentNegotiation) {
        json(
            json = if (jsonBuilderAction == null) ApiJson.serverSerializeJson() else Json(ApiJson.serverSerializeJson(), jsonBuilderAction),
            contentType = ContentType.Application.Json
        )
    }
}

private fun Application.installWebsockets(){
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

private fun Application.installJwt(){
    val jwtHelper: AbstractJwtHelper by inject()
    install(Authentication) {
        jwt {
            verifier(jwtHelper.getVerifier()) //Configure a token verifier
            this.realm = jwtHelper.realm
            validate { credential -> jwtHelper.validate(credential) } // Validate JWT payload
        }
    }
}

/**
 * @param backOfNginx true if ktor server is back of nginx
 * */
private fun Application.installCORS(backOfNginx: Boolean) {
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
