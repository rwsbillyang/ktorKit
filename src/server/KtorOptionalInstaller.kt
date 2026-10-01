package com.github.rwsbillyang.ktorKit.server


import io.ktor.server.application.*
import org.slf4j.LoggerFactory


/**
 *     若添加了依赖库，自动install下面这些plugin
 *     install(Resources)
 *     install(AutoHeadResponse)
 *     install(ForwardedHeaders)
 *     install(XForwardedHeaders)
 *     install(PartialContent)
 *     install(ConditionalHeaders)
 *     install(DefaultHeaders)
 * */
fun Application.installOptionalKtorPlugins() {
    val plugins = listOf(
        "AutoHeadResponse" to "io.ktor.server.plugins.autohead.AutoHeadResponse",
        "ForwardedHeaders" to "io.ktor.server.plugins.forwardedheaders.ForwardedHeaders",
        "XForwardedHeaders" to "io.ktor.server.plugins.forwardedheaders.XForwardedHeaders",
        "PartialContent" to "io.ktor.server.plugins.partialcontent.PartialContent",
        "ConditionalHeaders" to "io.ktor.server.plugins.conditionalheaders.ConditionalHeaders",
        "DefaultHeaders" to "io.ktor.server.plugins.defaultheaders.DefaultHeaders",
        //"CachingHeaders" to "io.ktor.server.plugins.cachingheaders.CachingHeaders",
        //"CallLogging" to "io.ktor.server.plugins.calllogging.CallLogging",
        //"WebSockets" to "io.ktor.server.websocket.WebSockets",
        "Resources" to "io.ktor.server.resources.Resources",
        //"Koin" to "org.koin.ktor.plugin.Koin"
    )

    plugins.forEach { (name, className) ->
        installPluginIfPresent(className, name)
    }

}

private val optionalPluginLogger = LoggerFactory.getLogger("OptionalPluginInstaller")

/**
 * 如果 classpath 中存在指定的 Ktor 插件（Kotlin object），则自动 install。
 * @param className 插件的全限定类名（如 "io.ktor.server.plugins.autohead.AutoHeadResponse"）
 * @param pluginName 用于日志打印的插件名称
 * @return 是否成功安装
 */
private fun Application.installPluginIfPresent(className: String, pluginName: String? = null): Boolean {
    return try {
        // 1. 检查类是否存在
        val clazz = Class.forName(className)

        // 2. 获取 Kotlin object 的 INSTANCE（Ktor 插件都是单例对象）
        val pluginInstance = clazz.getField("INSTANCE").get(null)

        // 3. 确认是 Ktor Plugin 类型并安装
        if (pluginInstance is Plugin<*, *, *>) {
            @Suppress("UNCHECKED_CAST")
            install(pluginInstance as Plugin<Application, Any, Any>)
            optionalPluginLogger.info("✅ Auto-installed plugin: ${pluginName ?: className}")
            true
        } else {
            optionalPluginLogger.warn("⚠️ Class $className is not a Ktor Plugin, skipped.")
            false
        }
    } catch (e: ClassNotFoundException) {
        // 依赖库不存在，静默跳过（这是预期行为）
        optionalPluginLogger.debug("Plugin not in classpath, skipped: $className")
        false
    } catch (e: NoSuchFieldException) {
        optionalPluginLogger.error("Plugin class $className is not a singleton object.")
        false
    } catch (e: Exception) {
        optionalPluginLogger.error("Failed to install plugin $className", e)
        false
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
