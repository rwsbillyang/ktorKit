package com.github.rwsbillyang.ktorKit.util



import io.ktor.http.content.*
import io.ktor.utils.io.jvm.javaio.copyTo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

object UploadUtil {

    /**
     * 处理上传上来的base64编码数据， 成功返回文件路径，失败返回false
     * @param path 存储路径 后面不含"/"
     * @param filename 存储的文件名，不包括扩展名
     * @param base64 payload数据
     * @param md5 校验值， 为空的话则不校验
     * */
    suspend fun handleBase64(path: String, filename: String, base64: String, md5: String?): String?{
        if(md5 != null){
            val md5_ = md5Hex(base64)
            if(md5 != md5_)
            {
                return null
            }
        }

        //https://stackoverflow.com/questions/46240495/base64-encoding-illegal-base64-character-3c
        val b: ByteArray = Base64.getMimeDecoder().decode(base64.split(",")[1])
        for (i in b.indices) {
            var v: Int = b[i].toInt()
            if (v < 0) {//调整异常数据
                v += 256
                b[i] = v.toByte()
            }
        }

        val uploadDir = File(path)
        if (!uploadDir.exists()  && !uploadDir.mkdirs()) {
            throw IOException("Failed to create directory ${uploadDir.absolutePath}")
        }

        val imgFilePath = "$path/$filename.${extName(base64)}"//新生成的图片
        writeToFile(b.inputStream(), imgFilePath)
        return imgFilePath
    }


    /**
     * 处理上传上来的二进制数据， 成功返回文件路径，失败返回false
     * @param path 存储路径 后面不含"/"
     * @param filename 存储的文件名，不包括扩展名
     * @param ext 扩展名
     * @param binary payload数据
     * */
    suspend fun handleBinary(path: String, filename: String, ext: String, binary: ByteArray): String?{
        val uploadDir = File(path)
        if (!uploadDir.exists()  && !uploadDir.mkdirs()) {
            throw IOException("Failed to create directory ${uploadDir.absolutePath}")
        }

        val imgFilePath = "$path/$filename.$ext"//新生成的图片
        writeToFile(binary.inputStream(), imgFilePath)
        return imgFilePath
    }



    /**
     * @param path 存储于何处 后面不含"/"
     * @param filename 文件名称，不包含扩展名
     * @return 返回上传的文件名称，若为空，则表示失败
     *
     * https://ktor.io/docs/uploads.html#receiving-files-using-multipart
     * https://github.com/ktorio/ktor-samples/blob/1.3.0/app/youkube/src/Upload.kt
     * */
    suspend fun handleMultipart(path: String, filename: String, multiPartData: MultiPartData):String?
    {
        val uploadDir = File(path)
        if (!uploadDir.exists()  && !uploadDir.mkdirs()) {
            throw IOException("Failed to create directory ${uploadDir.absolutePath}")
        }

        var fileName: String? = null
        multiPartData.forEachPart { part ->
            when (part) {
                is PartData.FileItem -> {
                    val ext = part.originalFileName?.let { File(it).extension } ?: "jpg" // or webp?
                    fileName = "$path/$filename.$ext"
//                    part.streamProvider().use { input ->
//                        writeToFile(input, fileName)
//                    }
                    val channel = part.provider() // 返回 ByteReadChannel
                    File(fileName).outputStream().buffered().use { output ->
                        channel.copyTo(output)
                    }
                }
                else -> {
                    println("not support part type")
                }
            }
            part.release()
        }

        return fileName
    }



    /**
     * 根据传来的数据'data:image/jpeg;base64,' 获取扩展名 如jpeg, webp, png等，
     * 不符合规则“data:image/jpeg;” 返回默认jpg
     */
   private fun extName(base64: String): String? {
        val end = base64.indexOf(";")
        val ext = "jpg"
        val len = "data:image/".length
        return if (base64.startsWith("data:image/") && end > len) {
            base64.substring(len, end)
        } else ext
    }


    /**
     * 将数据写入文件
     * @param input 数据来源
     * @param outputFilename 输出文件完整名称，包含路径
     * */
    private suspend fun writeToFile(input: InputStream, outputFilename: String){
        val file = File(outputFilename)
        file.outputStream().buffered().use { output ->
            input.copyToSuspend(
                output
            )
        }
    }

    private suspend fun InputStream.copyToSuspend(
        out: OutputStream,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        yieldSize: Int = 4 * 1024 * 1024,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Long {
        return withContext(dispatcher) {
            val buffer = ByteArray(bufferSize)
            var bytesCopied = 0L
            var bytesAfterYield = 0L
            while (true) {
                val bytes = read(buffer).takeIf { it >= 0 } ?: break
                out.write(buffer, 0, bytes)
                if (bytesAfterYield >= yieldSize) {
                    yield()
                    bytesAfterYield %= yieldSize
                }
                bytesCopied += bytes
                bytesAfterYield += bytes
            }
            return@withContext bytesCopied
        }
    }
}