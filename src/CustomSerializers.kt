/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-08-15 22:35
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

package com.github.rwsbillyang.ktorKit

import com.github.rwsbillyang.ktorKit.util.toUtc
import com.github.rwsbillyang.ktorKit.util.utcToLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.types.ObjectId
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


@Deprecated("use ObjectIdBase64Serializer instead")
object ObjectIdHexStringSerializer : KSerializer<ObjectId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ObjectIdHexStringSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ObjectId) {
        encoder.encodeString(value.toHexString())
    }

    override fun deserialize(decoder: Decoder): ObjectId {
        return ObjectId(decoder.decodeString())
    }
}

/**
 * based on Base64 URL Encoder and Decoder
 * */
object ObjectIdBase64Serializer : KSerializer<ObjectId> {
    private val base64Decoder = Base64.getUrlDecoder()
    private val base64Encoder = Base64.getUrlEncoder()
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ObjectIdBase64Serializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ObjectId) {
        encoder.encodeString(base64Encoder.encodeToString(value.toByteArray()))
    }

    override fun deserialize(decoder: Decoder): ObjectId {
        return ObjectId(base64Decoder.decode(decoder.decodeString()))
    }
}

fun ObjectId.to64String() = Base64.getUrlEncoder().encodeToString(toByteArray())
fun String.toObjectId() = ObjectId(Base64.getUrlDecoder().decode(this))



object LocalDateTimeAsStringSerializer : KSerializer<LocalDateTime> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTimeAsStringSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(formatter.format(value))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(
            decoder.decodeString(),
            formatter
        )
    }
}


object LocalDateTimeAsLongSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTimeAsLongSerializer", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeLong(value.toUtc())
    override fun deserialize(decoder: Decoder): LocalDateTime = decoder.decodeLong().utcToLocalDateTime()
}
