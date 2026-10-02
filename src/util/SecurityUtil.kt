package com.github.rwsbillyang.ktorKit.util

import java.security.MessageDigest

//使用方式： val md5_ = md5Hex(str)
fun md5Hex(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    val bytes = md.digest(input.toByteArray(Charsets.UTF_8))

    // 注意：Byte 转 Int 时要 & 0xff，否则负数会格式化成 ffffffff
    //return bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }

    val sb = StringBuilder(bytes.size * 2)
    for (b in bytes) {
        sb.append(Character.forDigit((b.toInt() shr 4) and 0xf, 16))
        sb.append(Character.forDigit(b.toInt() and 0xf, 16))
    }
    return sb.toString()
}