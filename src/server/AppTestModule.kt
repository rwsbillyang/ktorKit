package com.github.rwsbillyang.ktorKit.server

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.koin.dsl.module


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