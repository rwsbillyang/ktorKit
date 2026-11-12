package com.github.rwsbillyang.ktorKit.server

import com.github.rwsbillyang.ktorKit.ApiJson
import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.respondText
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation

import kotlinx.serialization.SerialName
//import com.fasterxml.jackson.annotation.JsonProperty
import java.util.concurrent.ConcurrentHashMap

suspend inline fun <reified T> ApplicationCall.respondBox(box: DataBox<T>) =
    respondText(ApiJson.serverSerializeJson().encodeToString(box), ContentType.Application.Json, HttpStatusCode.OK)
suspend inline fun <reified T> ApplicationCall.respondBoxOK(data: T) =
    respondText(ApiJson.serverSerializeJson().encodeToString(DataBox.ok(data)), ContentType.Application.Json, HttpStatusCode.OK)

suspend inline fun ApplicationCall.respondBoxKO(msg: String) =
    respondText(ApiJson.serverSerializeJson().encodeToString(DataBox.ko<Unit>(msg)), ContentType.Application.Json, HttpStatusCode.OK)

suspend inline fun ApplicationCall.respondBoxJsonText(json: String) =
    respondText(json, ContentType.Application.Json, HttpStatusCode.OK)


/**
 * routing {
 *     get("/articles") {
 *         // 核心：直接一行代码将 Query/Path 参数映射为第三方类的实例！
 *         // 即使 ThirdPartyArticles 内部有 50 个字段，这里也能自动完成注入。
 *         val articles = call.bindParameters<ThirdPartyArticles>()
 *
 *         // 直接调用第三方对象的业务逻辑
 *         val result = articles.someThirdPartyMethod()
 *
 *         call.respond(result)
 *     }
 * }
 * */
inline fun <reified T : Any> ApplicationCall.bindParameters(): T {
    // 1. O(1) Get cached metadata, avoiding runtime reflection
    val metadata = ParameterBindingCache.getMetadata(T::class)
    val parameterValues = mutableMapOf<KParameter, Any?>()

    for ((param, meta) in metadata.parameterMappings) {
        val type = meta.type
        val classifier = type.classifier

        // 2. Handle collection types
        if (classifier == List::class || classifier == Collection::class) {
            val rawValues = parameters.getAll(meta.queryParamName)
            if (rawValues.isNullOrEmpty()) {
                if (meta.isOptional || meta.isNullable) continue
                else throw ParameterBindingException("Missing required collection parameter: ${meta.queryParamName}")
            }

            val innerType = type.arguments.firstOrNull()?.type
            parameterValues[param] = rawValues.map { convertValue(meta.queryParamName, it, innerType) }
            continue
        }

        // 3. Handle single-value types
        val rawValue = parameters[meta.queryParamName]
        if (rawValue == null) {
            if (meta.isOptional || meta.isNullable) continue
            else throw ParameterBindingException("Missing required request parameter: ${meta.queryParamName}")
        }

        parameterValues[param] = convertValue(meta.queryParamName, rawValue, type)
    }

    // 4. Instantiate directly via cached constructor reference
    return metadata.constructor.callBy(parameterValues)
}

/**
 * Core type conversion dispatcher
 */
fun convertValue(paramName: String, rawValue: String, type: KType?): Any? {
    if (type == null) return rawValue
    val classifier = type.classifier

    return when {
        classifier == String::class -> rawValue

        classifier == Int::class -> rawValue.toIntOrNull()
            ?: throw ParameterBindingException("Invalid format for parameter '$paramName', expected Int, current value: '$rawValue'")

        classifier == Long::class -> rawValue.toLongOrNull()
            ?: throw ParameterBindingException("Invalid format for parameter '$paramName', expected Long, current value: '$rawValue'")

        classifier == Boolean::class -> rawValue.toBoolean()

        classifier == Double::class -> rawValue.toDoubleOrNull()
            ?: throw ParameterBindingException("Invalid format for parameter '$paramName', expected Double, current value: '$rawValue'")

        classifier == Float::class -> rawValue.toFloatOrNull()
            ?: throw ParameterBindingException("Invalid format for parameter '$paramName', expected Float, current value: '$rawValue'")

        // UUID support
        classifier == UUID::class -> {
            try {
                UUID.fromString(rawValue)
            } catch (e: Exception) {
                throw ParameterBindingException("Invalid format for parameter '$paramName', expected a valid UUID, current value: '$rawValue'")
            }
        }

        // High-precision numeric types
        classifier == BigDecimal::class -> rawValue.toBigDecimalOrNull()
            ?: throw ParameterBindingException("Invalid format for parameter '$paramName', expected BigDecimal, current value: '$rawValue'")

        classifier == BigInteger::class -> rawValue.toBigIntegerOrNull()
            ?: throw ParameterBindingException("Invalid format for parameter '$paramName', expected BigInteger, current value: '$rawValue'")

        // Java 8 Date & Time API
        classifier == LocalDate::class -> {
            try { LocalDate.parse(rawValue) }
            catch (e: Exception) { throw ParameterBindingException("Invalid date format for parameter '$paramName', expected YYYY-MM-DD, current value: '$rawValue'") }
        }

        classifier == LocalDateTime::class -> {
            try { LocalDateTime.parse(rawValue) }
            catch (e: Exception) { throw ParameterBindingException("Invalid date-time format for parameter '$paramName', expected ISO-8601 format, current value: '$rawValue'") }
        }

        classifier == Instant::class -> {
            try { Instant.parse(rawValue) }
            catch (e: Exception) { throw ParameterBindingException("Invalid timestamp format for parameter '$paramName', expected ISO-8601 Instant format, current value: '$rawValue'") }
        }

        // Enum type support (case-insensitive matching)
        classifier is kotlin.reflect.KClass<*> && classifier.java.isEnum -> {
            @Suppress("UNCHECKED_CAST")
            val enumClass = classifier.java as Class<out Enum<*>>
            enumClass.enumConstants.firstOrNull { it.name.equals(rawValue, ignoreCase = true) }
                ?: throw ParameterBindingException("Invalid value '$rawValue' for parameter '$paramName', valid options: ${enumClass.enumConstants.joinToString { it.name }}")
        }

        else -> rawValue
    }
}


data class ClassBindingMetadata<T : Any>(
    val constructor: KFunction<T>,
    // 映射关系：KParameter -> (URL 参数名, 参数类型, 是否可空/可选)
    val parameterMappings: Map<KParameter, ParameterMeta>
)

data class ParameterMeta(
    val queryParamName: String,
    val type: KType,
    val isOptional: Boolean,
    val isNullable: Boolean
)



//利用 ConcurrentHashMap 配合 Kotlin 的 getOrPut，一个零第三方依赖、线程安全且极具性能的缓存层：
//零运行时反射损耗：反射只在某个类第一次接收到请求时触发一次，后续所有并发请求直接命中内存中的 ClassBindingMetadata，性能与手写取值几乎没有差异
//线程安全：ConcurrentHashMap.getOrPut 保证了在多线程高并发下，即使多个请求同时到达，目标类也只会安全地被解析一次。
object ParameterBindingCache {

    // 全局缓存容器：Class -> 编译好的绑定元数据
    private val cache = ConcurrentHashMap<Class<*>, ClassBindingMetadata<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getMetadata(kClass: kotlin.reflect.KClass<T>): ClassBindingMetadata<T> {
        return cache.getOrPut(kClass.java) {
            // 仅在首次访问时执行反射解析（计算开销仅发生一次）
            val constructor = kClass.primaryConstructor
                ?: throw IllegalArgumentException("${kClass.simpleName} 必须有主构造函数")

            val mappings = mutableMapOf<KParameter, ParameterMeta>()

            for (param in constructor.parameters) {
                // 解析注解或原名字（反射开销被安全隔离在缓存初始化阶段）
                val paramName = param.findAnnotation<SerialName>()?.value
                    //?: param.findAnnotation<JsonProperty>()?.value
                    ?: param.name
                    ?: throw IllegalArgumentException("构造函数参数缺少名称")

                mappings[param] = ParameterMeta(
                    queryParamName = paramName,
                    type = param.type,
                    isOptional = param.isOptional,
                    isNullable = param.type.isMarkedNullable
                )
            }

            ClassBindingMetadata(constructor, mappings)
        } as ClassBindingMetadata<T>
    }
}


// 定义专属异常
class ParameterBindingException(message: String) : IllegalArgumentException(message)

