/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-08-15 22:34
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


import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

object ApiJson {
    //sealed class的子类通过classDiscriminator判断是哪个子类， kotlinx.serialization默认使用type
    //与正常的type字段冲突，kmongo/kbson 默认是___type，修改不生效。
    // 但使用___type, 与spring不兼容，spring中序列化时默认添加_class字段
    const val myClassDiscriminator = "_class"

    /**
     * 忽略null值，忽略未知键等，具体如下：
     * encodeDefaults = true
     * explicitNulls = false
     * ignoreUnknownKeys = true
     * classDiscriminator = "_class" //某些payload中拥有type字段，会冲突
     * allowSpecialFloatingPointValues = true
     * useArrayPolymorphism = false
     * */
    @OptIn(ExperimentalSerializationApi::class)
    fun JsonBuilder.apiJsonBuilder() {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true

        //输出给前端和前端提交的类型，可以为任意字符串，与kmongo/kbson无关，
        // 二者各使用各的classDiscriminator，都会转换成Java 对象实体
        //前端 <---ApiJson.serverSerializeJson---> Java对象实体 <---kmongo/kbson---> MongoDB bson存储
        // kmongo/kbson总是使用___type
        classDiscriminator = myClassDiscriminator
        //isLenient = true
        allowSpecialFloatingPointValues = true
        useArrayPolymorphism = false
    }

    fun myJson() = Json { apiJsonBuilder() }

    /**
     * server侧的serialize，包含了ObjectId和LocalDateTime的自定义序列化
     * */
    fun serverSerializeJson() = Json {
        apiJsonBuilder()
        serializersModule = SerializersModule {
            try{
                Class.forName("org.bson.types.ObjectId", false, javaClass.classLoader)
                contextual(ObjectIdBase64Serializer)
            }catch (e: Exception){
                System.err.println("no bson dependency, ignore ObjectIdBase64Serializer")
            }

            //contextual(LocalDateTimeAsStringSerializer)
            contextual(LocalDateTimeAsLongSerializer)
        }
    }

}