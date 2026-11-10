package com.github.rwsbillyang.ktorKit.util

import java.nio.ByteBuffer
import java.util.Base64

object LongBase64Util {

    /**
     * 1. Long 转 Base64 字节串（8字节二进制）
     */
    fun longToBase64(value: Long): String {
        val buffer = ByteArray(8)
        ByteBuffer.wrap(buffer).putLong(value)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer)
    }

    /**
     * 2. Base64 还原为 Long
     */
    fun base64ToLong(base64Str: String): Long {
        val bytes = Base64.getUrlDecoder().decode(base64Str)
        // 在 Kotlin 中可以直接访问 long 属性，等同于 Java 的 getLong()
        return ByteBuffer.wrap(bytes).long
    }
}

//fun main() {
//    val originalId = 1738492038492018392L
//
//    // 转换
//    val base64 = LongBase64Util.longToBase64(originalId)
//    println("Base64 结果: $base64") // 输出类似：FXXc5gAAAAA=
//
//    // 还原
//    val restoredId = LongBase64Util.base64ToLong(base64)
//    println("还原后的 Long: $restoredId") // 输出：1738492038492018392
//}