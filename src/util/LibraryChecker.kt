package com.github.rwsbillyang.ktorKit.util

import java.util.ServiceLoader


/**
 * 判断一个库是否存在
 * @param className 如org.apache.commons.lang3.StringUtils
 * */
fun isLibraryAvailable(className: String,): Boolean = runCatching {
    Class.forName(className)
}.isSuccess




/**
 * 判断指定类是否存在于 classpath 中，通常用于判断一个依赖库是否存在
 *
 * @param className 类的全限定名，例如 "com.google.gson.Gson"
 * @param initialize 是否初始化类（一般设为 false 避免执行静态代码块）
 * @param classLoader 使用的类加载器，默认使用线程上下文类加载器
 *
 * 使用示例
 * ```kotlin

 * fun main() {
 *     if (isClassPresent("com.google.gson.Gson")) {
 *         println("Gson 库已存在")
 *     } else {
 *         println("Gson 库未找到")
 *     }
 * }
 * ```
 */
fun isClassPresent(
    className: String,
    initialize: Boolean = false,
    classLoader: ClassLoader? = null
): Boolean {
    return try {
        Class.forName(
            className,
            initialize,
            classLoader ?: Thread.currentThread().contextClassLoader
        )
        true
    } catch (e: ClassNotFoundException) {
        // 类不在 classpath 中
        false
    } catch (e: LinkageError) {
        // 类存在，但它依赖的其他类不存在或版本冲突（罕见，按需处理）
        // 如果只想判断“是否存在”，也可以返回 true
        false
    }
}




/**
 * 如果依赖库是通过 Java 的 SPI 机制（如 JDBC 驱动、java.util.ServiceLoader）提供的，你可以直接通过 ServiceLoader 检查是否有实现：
 *  示例：检查是否有 JDBC 驱动（通常不需要手动检查，DriverManager 会自动加载）
 *  if (isServicePresent(java.sql.Driver::class.java)) { ... }
 *  */
fun <T : Any> isServicePresent(service: Class<T>): Boolean {
    return ServiceLoader.load(service).iterator().hasNext()
}