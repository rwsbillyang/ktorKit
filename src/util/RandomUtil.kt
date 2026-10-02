package com.github.rwsbillyang.ktorKit.util

fun randomAlphanumeric(count: Int): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return List(count) { chars.random() }.joinToString("")
}

// 或者写成扩展函数，方便调用
// 使用: val str = 10.randomAlphanumeric()
//fun Int.randomAlphanumeric(): String {
//    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
//    return List(this) { chars.random() }.joinToString("")
//}

