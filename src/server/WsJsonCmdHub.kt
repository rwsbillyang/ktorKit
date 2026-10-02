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

package com.github.rwsbillyang.ktorKit.server

import com.github.rwsbillyang.ktorKit.ApiJson
import com.github.rwsbillyang.ktorKit.util.randomAlphanumeric
import io.ktor.websocket.*

import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap


class WsSessions: KoinComponent {
    private val sessionMap = ConcurrentHashMap<String, DefaultWebSocketSession>()
    fun session(id: String) = sessionMap[id]
    fun addSession(session: DefaultWebSocketSession): String{
        val id = randomAlphanumeric(19)
        sessionMap[id]=session
        return id
    }

    suspend fun removeSession(id: String){
        sessionMap[id]?.close(CloseReason(CloseReason.Codes.NORMAL, "Client close"))
        sessionMap.remove(id)
    }
}
@Serializable
class SocketResponse(
    val id: String, //session ID
    val cmd: String, //收到的cmd
    val msg: String, //回复的正文内容
){
    companion object{
        const val MSG_READY = "ready"
        const val MSG_FORMAT_ERROR = "request format error"
        const val MSG_NO_HANDLER = "no handler handles"
    }
}

@Serializable
class SocketRequest(
    val cmd: String, //请求的cmd
    val id: String, //上次回复的id
){
    companion object{
        const val CMD_CONNECT = "connect"
        const val CMD_CLOSE = "close"
        //const val CMD_QRCODE_LOGIN = "qrcodeLogin" //子类自行扩展cmd并进行处理
    }
}

interface CmdHandler {
    /**
     * 若返回null，表示未消耗此cmd，将继续由其它handler处理
     * */
    fun onCmdRequest(request: SocketRequest): SocketResponse?
}
/**
 * 简单定义了与client侧的消息json交互格式, 需installWsHub
 * 前端在开始connect时，只需发送：connect，后端返回SocketResponse包括了（id，cmd，正文）， 关闭连接时只需发送： close
 * 中间交互过程双方均需发送json字符串
 * */
class WsJsonCmdHub : KoinComponent {
    // private val log = LoggerFactory.getLogger("WebSocketHelper")
    private val sessions: WsSessions by inject()
    //private val sessionMap = ConcurrentHashMap<String, DefaultWebSocketSession>()

    private val handlers = mutableListOf<CmdHandler>()

    fun registerHandler(handler: CmdHandler){
        handlers.add(handler)
    }
    fun unregisterHandler(handler: CmdHandler){
        handlers.remove(handler)
    }

    suspend fun handle(session: DefaultWebSocketSession){
        for (frame in session.incoming) {
            when (frame) {
                is Frame.Text -> {
                    val json = ApiJson.serverSerializeJson()
                    val request = json.decodeFromString<SocketRequest>(frame.readText())
                    when(request.cmd)
                    {
                        SocketRequest.CMD_CONNECT -> {
                            val id = sessions.addSession(session)
                            session.send(json.encodeToString(SocketResponse(id, SocketRequest.CMD_CONNECT, SocketResponse.MSG_READY)))
                        }
                        SocketRequest.CMD_CLOSE -> {
                           sessions.removeSession(request.id)
                        }
                        else -> handleJsonCmd(request, session)
                    }
                }
                else -> session.send("not support format")
            }
        }
    }

    suspend fun handleJsonCmd(request: SocketRequest, session: DefaultWebSocketSession){
        var flag = false
        val json = ApiJson.serverSerializeJson()
        for(handler in handlers){
            val response = handler.onCmdRequest(request)
            if(response != null){
                flag = true
                session.send(json.encodeToString(response))
                break
            }
        }
        if(!flag)
            session.send(json.encodeToString(SocketResponse(request.id, request.cmd,SocketResponse.MSG_NO_HANDLER)))
    }
}