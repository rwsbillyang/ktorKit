package com.github.rwsbillyang.ktorKit.server

import io.ktor.server.application.*
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.KParameter


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
    val kClass = T::class
    val constructor = kClass.primaryConstructor
        ?: throw IllegalArgumentException("${kClass.simpleName} 必须有主构造函数")

    val parameterValues = mutableMapOf<KParameter, Any?>()

    for (param in constructor.parameters) {
        val paramName = param.name
            ?: throw IllegalArgumentException("构造函数参数缺少名称（请确保编译时开启了 -Xjsr305 或保留参数名）")

        val rawValue = parameters[paramName]

        if (rawValue == null) {
            // 如果请求中没有传该参数：
            // - 如果是可选参数（有默认值）或可空类型，直接跳过，让 Kotlin 走默认值或赋 null
            if (param.isOptional || param.type.isMarkedNullable) {
                continue
            } else {
                throw IllegalArgumentException("缺少必要的请求参数: $paramName")
            }
        }

        // 基础类型安全转换
        val convertedValue = when (param.type.classifier) {
            String::class -> rawValue
            Int::class -> rawValue.toIntOrNull() ?: throw IllegalArgumentException("参数 $paramName 格式错误，应为 Int")
            Long::class -> rawValue.toLongOrNull() ?: throw IllegalArgumentException("参数 $paramName 格式错误，应为 Long")
            Boolean::class -> rawValue.toBoolean()
            Double::class -> rawValue.toDoubleOrNull() ?: throw IllegalArgumentException("参数 $paramName 格式错误，应为 Double")
            Float::class -> rawValue.toFloatOrNull() ?: throw IllegalArgumentException("参数 $paramName 格式错误，应为 Float")
            else -> rawValue // 其它类型可根据需要继续扩展
        }

        parameterValues[param] = convertedValue
    }

    // 通过反射调用主构造函数实例化对象
    return constructor.callBy(parameterValues)
}