package com.github.rwsbillyang.ktorKit.server

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install

import io.ktor.server.plugins.cors.routing.CORS


/**
 * @param backOfNginx true if ktor server is back of nginx
 * 是否部署在nginx之后，只有添加了CORS依赖才生效
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
//
///**
// * 带配置可选安装：CallLogging，CachingHeaders，ContentNegotiation，WebSockets，jwt，CORS
// * */
// internal fun Application.installOptionalKtorPluginsWithConfig(
//    logHeaders: List<String>? = null, //"X-Auth-uId","X-Auth-UserId", "X-Auth-ExternalUserId", "X-Auth-oId", "X-Auth-unId","X-Auth-CorpId","Authorization"
//    jsonBuilderAction: (JsonBuilder.() -> Unit)? = null
//    )
//{
//    if(isLibraryAvailable("io.ktor.server.plugins.calllogging.CallLogging"))
//    {
//        optionalPluginLogger.info("install CallLogging")
//        installCallLogging(logHeaders)
//    }
//
//    if(isLibraryAvailable("io.ktor.server.plugins.cachingheaders.CachingHeaders"))
//    {
//        optionalPluginLogger.info("install CachingHeaders")
//        installCachingHeaders()
//    }
//
//
//    if(isLibraryAvailable("io.ktor.server.plugins.contentnegotiation.ContentNegotiation"))
//    {
//        optionalPluginLogger.info("install ContentNegotiation")
//        installContentNegotiation(jsonBuilderAction)
//    }
//
//    if(isLibraryAvailable("io.ktor.server.websocket.WebSockets"))
//    {
//        optionalPluginLogger.info("install WebSockets")
//        installWebsockets()
//    }
//
//
//    if(isLibraryAvailable("io.ktor.server.auth.jwt.jwt"))
//    {
//        optionalPluginLogger.info("install JWT")
//        installJwt()
//     }
//}

