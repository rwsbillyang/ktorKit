package com.github.rwsbillyang.ktorKit.test


import com.github.rwsbillyang.ktorKit.ApiJson
import com.github.rwsbillyang.ktorKit.apiBox.DataBox
import com.github.rwsbillyang.ktorKit.client.DefaultClient
import com.github.rwsbillyang.ktorKit.server.respondBox
import com.github.rwsbillyang.ktorKit.server.simpleTestableModule

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals


class ApplicationTest {
    @Test
    fun testRoot() = testApplication{
        application {
            simpleTestableModule()
        }
        val response = client.get("/ok")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }

    @Test
    fun testJsonSerialize() = testApplication{
        @Serializable
        class Box(val id: Int,val msg: String)

        val box1 = Box(1,"OK")

        application {
            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                json(ApiJson.serverSerializeJson())
            }
            routing {
                get("/ok") {
                    call.respond(box1)
                }
            }
        }
        val client = createClient {
            //this@testApplication.
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(ApiJson.myJson())
            }
        }


        val box2 = client.get("/ok").body<Box>()
        assertEquals(box1.id, box2.id)
        assertEquals(box1.msg, box2.msg)
    }

    @Test
    fun testRootGenericDataBox1() = testApplication{
        val box = DataBox.ok("OK")
        application {
            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                json(ApiJson.serverSerializeJson())
            }
            routing {
                get("/databox") {
                    call.respondBox(box)
                }
            }
        }
        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(ApiJson.myJson())
            }
        }

        val response = client.get("/databox")
        assertEquals(HttpStatusCode.OK, response.status)

        val boxRes:DataBox<String> = response.body()
        assertEquals(box.data, boxRes.data)
    }

    @Test
    fun testRootGenericDataBox2() = testApplication{
        val box = DataBox.ok("OK")
        application {
            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                json(ApiJson.myJson())
            }
            routing {
                get("/ok") {
                    call.respond(box)
                }
            }
        }
        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(ApiJson.myJson())
            }
        }
        val response = client.get("/ok")
        assertEquals(HttpStatusCode.OK, response.status)

        val boxRes:DataBox<String> = response.body()
        assertEquals(box.data, boxRes.data)
    }

    //@Test
    fun testRemote() = testApplication{
        @Serializable
        class Box(val code: Int, val message:String)

        val response = DefaultClient.get("https://api.apiopen.top/api/getHaoKanVideo?page=0&size=2")
        assertEquals(HttpStatusCode.OK, response.status)

        val boxRes:Box = response.body()
        assertEquals(200, boxRes.code)
        assertEquals("成功!", boxRes.message)
    }
}
