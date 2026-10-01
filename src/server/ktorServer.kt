/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-08-15 22:34
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


import com.auth0.jwt.exceptions.AlgorithmMismatchException
import com.auth0.jwt.exceptions.InvalidClaimException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.github.rwsbillyang.ktorKit.ApiJson

import com.github.rwsbillyang.ktorKit.apiBox.DataBox


import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*



class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()

class BizException(
    val code: String, val msg: String? = null,
    val type: Int? = null, val tId: String? = null, val host: String? = null,
    cause: Throwable? = null
) : RuntimeException(cause)
{
    companion object{
        fun ko(msg: String, type: Int? = DataBox.TYPE_WARN_MESSAGE, tId: String? = null, host: String? = null) = BizException(
            DataBox.CODE_KO,
            msg,
            type,
            tId,
            host
        )
    }
}

fun Routing.exceptionPage(application: Application)
{
    application.install(StatusPages) {
        val headers = listOf("X-Auth-UserId", "X-Auth-ExternalUserId", "X-Auth-uId", "X-Auth-oId", "X-Auth-unId","X-Auth-CorpId","Authorization")
        exception<AuthenticationException> { call, cause ->
            application.environment.log.error("AuthenticationException: cause=$cause, ${call.request.httpMethod.value} ${call.request.path()} ${call.authHeaders(headers)}")
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> { call, cause ->
            application.environment.log.error("AuthorizationException: cause=$cause, ${call.request.httpMethod.value} ${call.request.path()} ${call.authHeaders(headers)}")
            call.respond(HttpStatusCode.Forbidden)
        }

        exception<TokenExpiredException> { call, cause ->
            application.environment.log.error("TokenExpiredException: cause=$cause, ${call.request.httpMethod.value} ${call.request.path()} ${call.authHeaders(headers)}")
            call.respondBox(DataBox<Unit>(DataBox.CODE_TokenExpired,"登录过期，请点击右上角，退出登录后，重新进入"))
        }
        exception<InvalidClaimException> { call, cause ->
            application.environment.log.error("InvalidClaimException: cause=$cause, ${call.request.httpMethod.value} ${call.request.path()} ${call.authHeaders(headers)}")
            call.respondBox(DataBox.ko<Unit>("无效的token claim，请重新登录"))
        }
        exception<AlgorithmMismatchException> { call, cause ->
            application.environment.log.error("AlgorithmMismatchException: cause=$cause, ${call.request.httpMethod.value} ${call.request.path()} ${call.authHeaders(headers)}")
            call.respondBox(DataBox.ko<Unit>("AlgorithmMismatch"))
        }
        exception<AlgorithmMismatchException> { call, cause ->
            application.environment.log.error("JWTVerificationException: cause=$cause, ${call.request.httpMethod.value} ${call.request.path()} ${call.authHeaders(headers)}")
            call.respondBox(DataBox.ko<Unit>("JWTVerificationException"))
        }
        exception<BizException> { call, cause->
            application.environment.log.error("BizException: cause=$cause, ${call.request.httpMethod.value} ${call.request.path()} ${call.authHeaders(headers)}")
            call.respond(DataBox(cause.code, cause.msg, cause.type, null,cause.tId, cause.host))
        }
    }
}


suspend inline fun <reified T> ApplicationCall.respondBox(box: DataBox<T>) =
    respondText(ApiJson.serverSerializeJson().encodeToString(box), ContentType.Application.Json, HttpStatusCode.OK)
suspend inline fun <reified T> ApplicationCall.respondBoxOK(data: T) =
    respondText(ApiJson.serverSerializeJson().encodeToString(DataBox.ok(data)), ContentType.Application.Json, HttpStatusCode.OK)

suspend inline fun ApplicationCall.respondBoxKO(msg: String) =
    respondText(ApiJson.serverSerializeJson().encodeToString(DataBox.ko<Unit>(msg)), ContentType.Application.Json, HttpStatusCode.OK)

suspend inline fun ApplicationCall.respondBoxJsonText(json: String) =
    respondText(json, ContentType.Application.Json, HttpStatusCode.OK)


