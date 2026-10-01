/*
 * Copyright © 2020 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-11-02 15:09
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

package com.github.rwsbillyang.ktorKit.client



import com.github.rwsbillyang.ktorKit.ApiJson.apiJsonBuilder
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.accept
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File


val DefaultClient: HttpClient by lazy {
    HttpClient(CIO) {
        install(ContentNegotiation) {
            val json = Json { apiJsonBuilder()}
            json(json, ContentType.Application.Json)
            json(json, ContentType.Text.Plain) //fix NoTransformationFoundException when response is  Content-Type: text/plain
        }
        defaultRequest { // this: HttpRequestBuilder ->
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            accept(ContentType.Text.Plain)
        }

        install(HttpCache)
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 20000
        }


        install(Logging){
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        //https://ktor.io/docs/http-client-engines.html#jvm-and-android
        engine {
            maxConnectionsCount = 20480

            endpoint {
                /**
                 * Maximum number of requests for a specific endpoint route.
                 */
                maxConnectionsPerRoute = 10240

                /**
                 * Max size of scheduled requests per connection(pipeline queue size).
                 */
                pipelineMaxSize = 20

                /**
                 * Max number of milliseconds to keep idle connection alive.
                 */
                keepAliveTime = 5000

                /**
                 * Number of milliseconds to wait trying to connect to the server.
                 */
                connectTimeout = 8000

                /**
                 * Maximum number of attempts for retrying a connection.
                 */
                connectAttempts = 3
            }
        }
    }
}


/**
 * 多数情况下使用默认的client即可,亦可定制自己的client
 * */
//open val client = DefaultClient
//fun getByRaw(url: String): HttpResponse = runBlocking {
//    withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
//        DefaultClient.get(url)
//    }
//}

//inline fun <reified R> get(url: String): R = runBlocking {
//    withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
//        DefaultClient.get(url).body()
//    }
//}

/**
 * 返回R泛型类型结果
 * */
//inline fun <reified T, reified R> post(url: String, data: T? = null): R = runBlocking {
//    withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
//        DefaultClient.post(url) {
//            data?.let { setBody(data) }
//        }.body()
//    }
//}
//
//inline fun <reified T> postByRaw(url: String, data: T? = null): HttpResponse = runBlocking {
//    withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
//        DefaultClient.post(url) {
//            data?.let { setBody(data) }
//        }
//    }
//}

inline fun <reified R> doUpload(
    url: String,
    filePath: String,
    formData: Map<String, String>? = null,
    fileKey: String = "media",
    contentType: String = "image/*",
    boundary: String = "fileBoundary"
): R = runBlocking {
    //https://ktor.io/docs/request.html#upload_file
    withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
        DefaultClient.post(url) {
            val file = File(filePath)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        formData?.forEach { append(it.key, it.value) }
                        append(fileKey, file.readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, contentType)
                            append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                        })
                    },
                    boundary = boundary
                )
            )
            onUpload { bytesSentTotal, contentLength ->
                println("Sent $bytesSentTotal bytes from $contentLength")
                //if(onUploadBlock != null) onUploadBlock(bytesSentTotal, contentLength)
            }
        }.body()
    }
}

inline fun <reified R> doUpload(
    url: String,
    filePath: String,
    formData: Map<String, String>? = null,
    fileKey: String = "media",
    contentType: String = "image/*"
): R = runBlocking {
    //https://ktor.io/docs/request.html#upload_file
    withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
        val file = File(filePath)
        DefaultClient.submitFormWithBinaryData(
            url = url,
            formData = formData {
                formData?.forEach { append(it.key, it.value) }
                append(fileKey, file.readBytes(), Headers.build {
                    append(HttpHeaders.ContentType, contentType)
                    append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                })
            }
        ).body()
    }
}

fun doUploadRaw(
    url: String,
    filePath: String,
    formData: Map<String, String>? = null,
    fileKey: String = "media",
    contentType: String = "image/*"
) = runBlocking {
    //https://ktor.io/docs/request.html#upload_file
    withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
        val file = File(filePath)
        DefaultClient.submitFormWithBinaryData(
            url = url,
            formData = formData {
                formData?.forEach { append(it.key, it.value) }
                append(fileKey, file.readBytes(), Headers.build {
                    append(HttpHeaders.ContentType, contentType)
                    append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                })
            }
        )
    }
}


/**
 * 根据url下载文件，保存到filepath中
 *
 * @param url
 * @param filepath such as: ./qrcode/${appId}
 * @param filename: such as: abc.jpg
 * @return
 */
fun doDownload(url: String, filepath: String, filename: String) = runBlocking {
    val response: HttpResponse = DefaultClient.get(url)
    if (response.status.isSuccess()) {
        val content = ByteArrayContent(response.readRawBytes())
        val path = File(filepath)
        if (!path.exists()) {
            path.mkdirs()
        }
        val relative = "$filepath/$filename"
        val file = File(relative)
        file.writeBytes(content.bytes())
        relative
    } else {
        println("fail to download from $url, status=${response.status.value}")
        null
    }
}

/**
 * 根据url下载文件，保存到filepath中
 * bug: cannot return, timeout
 * @param url
 * @param filepath such as: ./qrcode/${appId}
 * @param filename: such as: abc.jpg
 * @return 成功返回true，否则返回失败
 */
//fun doDownload(url: String, filepath: String, filename: String): Boolean = runBlocking {
//    DefaultClient.prepareGet(url).execute { httpResponse ->
//        if (httpResponse.status.isSuccess()) {
//            try {
//                val channel: ByteReadChannel = httpResponse.body()
//
//                val directory = File(filepath)
//                if (!directory.exists()) {
//                    if (directory.mkdirs()) {
//                        println("WARN: create directory fail: $filepath")
//                        return@execute false
//                    }
//                }
//
//                val file = File("$filepath/$filename")
//                while (!channel.isClosedForRead) {
//                    val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
//                    while (!packet.isEmpty) {
//                        val bytes = packet.readBytes()
//                        file.appendBytes(bytes)
//                        //println("Received ${file.length()} bytes from ${httpResponse.contentLength()}")
//                    }
//                }
//                //println("A file saved to ${file.path}")
//                return@execute true
//            } catch (e: Exception) {
//                println("save $filename fail! Exception: ${e.message}")
//                return@execute false
//            }
//        }
//        return@execute false
//    }
//}


