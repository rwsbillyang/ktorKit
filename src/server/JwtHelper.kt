/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-08-15 22:36
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

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
import com.auth0.jwt.interfaces.Payload
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*

import org.apache.commons.lang3.RandomStringUtils
import org.koin.core.component.KoinComponent

import org.slf4j.LoggerFactory
import java.util.*

/**
 * webapp登录时收到token之后，亦需将uId设置到header "X-Auth-uId"之中
 * user模块中的Account._id或企业微信中的成员id(userId)
 * */
val ApplicationCall.uId
    get() = this.request.headers["X-Auth-uId"] // app account Id
val ApplicationCall.sysId
    get() = this.request.headers["X-Auth-sysId"]
val ApplicationCall.appId
    get() = this.request.headers["X-Auth-appId"]
val ApplicationCall.openId
    get() = this.request.headers["X-Auth-openId"]
val ApplicationCall.unionId
    get() = this.request.headers["X-Auth-unionId"]

val ApplicationCall.token
    get() = this.request.headers["Authorization"]?.substringAfter("Bearer")?.trim()

//openId

val ApplicationCall.roles
    get() = this.authentication.principal<JWTPrincipal>()?.payload?.getClaim(AuthUserInfo.KEY_ROLE)?.asString()?.split(",")

fun ApplicationCall.isFromAdmin(): Boolean{
    val roles = this.roles
    if(roles.isNullOrEmpty()) return false
    return roles.contains("admin") || roles.contains("root")
}

//"X-Auth-uId","X-Auth-UserId", "X-Auth-ExternalUserId","X-Auth-oId", "X-Auth-unId","X-Auth-CorpId","Authorization"
fun ApplicationCall.authHeaders(headers: List<String>): String {
    val auths = headers.joinToString("\n") { "$it:${request.header(it)}" }
    return "\nroles: ${this.roles}\n"+auths
}



/**
 * 只可访问一次
 * UserInfoJwtHelper中被设置
 * */
//var ApplicationCall.authInfo: AuthUserInfo
//    get() = this.attributes.take(AuthUserInfoKey)
//    set(value) = this.attributes.put(AuthUserInfoKey,value)
//
//// Declared as a global property
//private val AuthUserInfoKey = AttributeKey<AuthUserInfo>("AuthUserInfo")




/**
 *
 * */
enum class Role { guest, user, admin, root }

///**
// * 暂只有读写两样权限
// * */
//enum class Action { READ, WRITE, ALL }



interface IAuthUserInfo
{
    val uId: String
    companion object {
        const val KEY_UID = "uId"
    }
}

/**
 * 从token解码出来的用户认证信息
 * */
class AuthUserInfo(
    override val uId: String,
    val level: Int?,
    val role: List<String>?,
): IAuthUserInfo
{
    //最终结果，为null表示未定
    //var isAllow: Boolean? = null

    companion object {
        const val KEY_LEVEL = "level"
        const val KEY_ROLE = "role"
    }
}


//fun JWTAuthenticationProvider.config(jwtHelper: AbstractJwtHelper) {
//    verifier(jwtHelper.getVerifier()) //Configure a token verifier
//    this.realm = jwtHelper.realm
//    validate { credential -> jwtHelper.validate(credential) } // Validate JWT payload
//}


/**
 *
 * @param secretKey 秘钥，如RSA秘钥，用于签名的任意秘钥，与域名无关
 * @param issuer 填写自己的域名
 * @param realm 比如xxxServer
 * @param audience 默认webapp
 * @param subject 默认server
 * @param expireDays 有效期，默认90天
 * */
abstract class AbstractJwtHelper(
    private val issuer: String,
    secretKey: String,
    val realm: String = "Server",
    private val audience: String = "webapp",
    private val subject: String = "server",
    private val expireDays: Int = 90
): KoinComponent
{
    protected val log = LoggerFactory.getLogger("AbstractJwtHelper")

    private val algorithm: Algorithm = Algorithm.HMAC256(secretKey)

    /**
     * 对JWTCredential.Payload进行校验
     * */
    abstract fun validate(payload: Payload):Boolean
    /**
     * 判断对某个call请求操作是否有权限
     * @return 具备操作权限返回true，否则返回false; 返回null时表示TBD待决定(适合于数据需要owner时的情况)
     *
     * @param call 操作请求
     * @param needAnyRole 需要具备任何一个该列表里的role才有权限,若为空，则表示无要求
     * @param needLevel 若为空，则表示无要求；否则大于等于该needLevel才有权限
     * */
    abstract fun isAuthorized(call: ApplicationCall, needAnyRole: List<String>? = null, needLevel: Int? = null): Boolean

    open fun validate(credential: JWTCredential): JWTPrincipal? {
        return if(validate(credential.payload)) {
            //log.info("validate jwt done!")
            JWTPrincipal(credential.payload)
        }else{
            log.warn("validate fail in subclass")
            null
        }

    }

    /**
     * install Authentication 需要
     * */
    fun getVerifier(): JWTVerifier {
        val v = JWT.require(algorithm)
            .withIssuer(issuer)
            .withSubject(subject)
            .withAudience(audience)

        //claims.forEach{ v.withClaim(it.key,it.value) }

        return v.build() //Reusable verifier instance
    }



    /**
     * @param jti: jwt的唯一身份标识，主要用来作为一次性token,从而回避重放攻击
     * @param claims 需要添加的键值对
     * */
    fun generateToken(jti: String, claims: HashMap<String, String>? = null)
    : String? {
        return try {
            val builder = jwtBuilder(jti)

            claims?.forEach { builder.withClaim(it.key, it.value) }

            builder.sign(algorithm)
        } catch (exception: JWTCreationException) {
            log.warn("Invalid Signing configuration / Couldn't convert Claims? JWTCreationException: ${exception.message}")
            null
        }
    }

    /**
     * 使用jwtBuilder之后，再withClaim之后，最后生成token
     * ```kotlin
     *     val jti = RandomStringUtils.randomAlphanumeric(8)
     *
     *     val token = jwtHelper.generateToken(jti){
     *          withClaim(JwtHelper.KeyUID, user._id!!.toHexString())
     *          withClaim(JwtHelper.KeyLevel, user.level)
     *     }
     * ```
     * */
    fun generateToken(jti: String, func: JWTCreator.Builder.() -> JWTCreator.Builder = { this }): String =
        jwtBuilder(jti).func().sign(algorithm)


    /**
     * 用于构建一个JWT，然后用于withClaim
     * <code>
     *     val jti = RandomStringUtils.randomAlphanumeric(8)
     *     val uId = user._id!!.toHexString()
     *     val token = jwtHelper
     *     .createToken(jwtHelper.jwtBuilder(jti)
     *     .withClaim(JwtHelper.KeyUID, uId)
     *     .withClaim(JwtHelper.KeyLevel, user.level))
     * </code>
     * */
    private fun jwtBuilder(jti: String): JWTCreator.Builder {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withIssuer(issuer)
            .withSubject(subject)
            .withAudience(audience)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + expireDays * 24 * 3600000L))
            .withJWTId(jti)
    }
}

abstract class UIdJwtHelper(issuer: String,
                     secretKey: String,
                     realm: String = "Server",
                     audience: String = "webapp",
                     subject: String = "server",
                     expireDays: Int = 90)
    : AbstractJwtHelper(secretKey, issuer, realm, audience, subject, expireDays)
{
    /**
     * 验证payLoad：主要是验证payload中的userId信息
     * */
    override fun validate(payload: Payload): Boolean {
        val claim = payload.getClaim(IAuthUserInfo.KEY_UID)
        if (claim.isNull) {
            log.info("no claim for ${IAuthUserInfo.KEY_UID} in jwtAuthentication")
            return false
        }

        val uId = claim.asString()
        if (!isValidUser(uId, payload)) {
            log.info("no user or uId($uId) is disabled")
            return false
        }

        return true
    }

    /**
     * 判断是否是真正的用户，根据用户Id判断用于是否是真正的用户
     *
     * @param uId  用户id
     * @return 真正的用户返回true，否则返回false
     *
     * Example code:
     * ```koltin
     * if(!ObjectId.isValid(uId)){ return false }
     * val user = userService.findById(uId)
     * return user != null && user.status != User.StatusDisabled
     * ```
     * */
    abstract fun isValidUser(uId: String, payload: Payload): Boolean
}
/**
 * 写入了uId/level/role等信息的jwt token helper类
 *
 * @param secretKey 秘钥，如RSA秘钥
 * @param issuer 填写自己的域名
 * @param audience 默认webapp
 * @param subject 默认server
 * @param expireDays 有效期，默认90天
 * */
abstract class UserInfoJwtHelper(
    issuer: String,
    secretKey: String,
    realm: String = "Server",
    audience: String = "webapp",
    subject: String = "server",
    expireDays: Int = 90
) : UIdJwtHelper(secretKey, issuer, realm, audience, subject, expireDays)
{
    fun generateToken(authUserInfo: AuthUserInfo) = generateToken(authUserInfo.uId, authUserInfo.level,authUserInfo.role)

    fun generateToken(uId: String, level: Int?, role: List<String>?): String {
        val jti = RandomStringUtils.randomAlphanumeric(8)
        return generateToken(jti) {
            withClaim(IAuthUserInfo.KEY_UID, uId)
            if (level != null) withClaim(AuthUserInfo.KEY_LEVEL, level.toString())
            if (!role.isNullOrEmpty()) withClaim(AuthUserInfo.KEY_ROLE, role.joinToString(","))
            this
        }
    }

    /**
     * 判断用户是否对某个请求操作有权限
     * @return 具备操作权限返回true，否则返回false; 返回null时表示TBD待决定(适合于数据需要owner时的情况)
     *
     * @param call 操作请求
     * @param needAnyRole 需要具备任何一个该列表里的role才有权限,若为空，且路径中包含了"/admin/"，则需要"admin"这个角色
     * @param needLevel 若为空，则表示无要求；否则大于等于该needLevel才有权限
     * */
    override fun isAuthorized(call: ApplicationCall, needAnyRole: List<String>?, needLevel: Int?): Boolean{
        val payload = call.authentication.principal<JWTPrincipal>()?.payload
        if (payload == null) {
            log.warn("payload is null")
            return false
        }

        val uIdClaim = payload.getClaim(IAuthUserInfo.KEY_UID)
        if (uIdClaim.isNull) {
            log.warn("uIdClaim is null")
            return false
        }

        val uId: String = uIdClaim.asString()
        //当为app登录时，call.uId为app account id,非空，call.sysId可能空（未绑定）也可能不空（已绑定），token中的uId为app account id
        //当为webAdmin登录时，call.uId为空，call.sysId非空,token中的uId为 sysId
        val idInCall = call.uId?:call.sysId
        if(idInCall != uId){
            log.warn("uId not set in X-Auth-uId? should be same as one in token")
            return false
        }

        val levelClaim = payload.getClaim(AuthUserInfo.KEY_LEVEL)
        val level = if (levelClaim.isNull) {
            null
        }else levelClaim.asInt()

        val roles = payload.getClaim(AuthUserInfo.KEY_ROLE)?.asString()?.split(",")

        //若没指定needAnyRole，且请求api路径中包含了"/admin/", 则校验"admin"，否则使用指定的needAnyRole
        val needRoles = if(needAnyRole.isNullOrEmpty() && call.request.path().contains("/admin/")) listOf("admin") else needAnyRole
        return checkLevel(needLevel, level) && checkRoles(needRoles, roles)
    }

    private fun checkLevel(needLevel: Int?, level: Int?): Boolean{
        return if(needLevel != null){
            when {
                level == null -> {
                    log.warn("needLevel: $needLevel, but level is null")
                    false
                }
                level < needLevel -> {
                    log.warn("needLevel: $needLevel, but level is $level, level is too low")
                    false
                }
                else -> true
            }
        }else
            true
    }
    /**
     * @param needAnyRole 需要具备的role 若为空表示无需任何权限
     * @param roles token中用户已具备的role，若为空则很多访问无权限，若为root表示拥有任何权限
     *
     * 正常情况下，求二者交集，交集非空表示有权限
     * */
    private fun checkRoles(needAnyRole: List<String>?, roles: List<String>?): Boolean{
        //不需要任何权限
        if (needAnyRole.isNullOrEmpty()) return true
        if (roles.isNullOrEmpty()){
            log.warn("no role, but need: $needAnyRole")
            return false
        }
        if(roles.contains("root")) {
            log.info("root user, allow visit any one")
            return true
        }

        return if(roles.intersect(needAnyRole).isEmpty()){
            log.warn("needAnyRole:$needAnyRole intersect roles:$roles isEmpty")
            false
        }else
            true
    }

}

class TestJwtHelper: UserInfoJwtHelper("test secret key","test issuer") {
    override fun isValidUser(uId: String,payload: Payload) = true

    override fun isAuthorized(call: ApplicationCall, needAnyRole: List<String>?, needLevel: Int?) = true
}

//for test
class DevJwtHelper : AbstractJwtHelper("issuer","devSecretKey"){
    override fun isAuthorized(call: ApplicationCall, needAnyRole: List<String>?, needLevel: Int?) = true
    override fun validate(payload: Payload) = true
}

