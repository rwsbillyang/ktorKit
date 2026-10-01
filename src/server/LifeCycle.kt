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


import io.ktor.server.application.*
import org.koin.core.KoinApplication
import org.koin.ktor.plugin.KoinApplicationStarted


/**
 * convenience for application subscription
 * */
open class LifeCycle(val application: Application) {
    companion object{
        //private var onStartedHandlersAtOnce = mutableSetOf<(KoinApplication) -> Unit>()
        var onStartedHandlers = mutableSetOf<(KoinApplication) -> Unit>()
        var onStoppingHandlers = mutableSetOf<(Application) -> Unit>()
    }

    /**
     * 指定启动后的执行代码块
     * */
    fun onStarted(block: (KoinApplication) -> Unit){
        onStartedHandlers.add(block)
        application.monitor.subscribe(KoinApplicationStarted, block)
        prepareReleaseSelf()
    }


    /**
     * 指定关闭时的执行代码块
     * */
    fun onStopping(block: (Application) -> Unit){
        onStoppingHandlers.add(block)
        // https://start.insert-koin.io/#/getting-started/koin-for-ktor
        application.monitor.subscribe(ApplicationStopping, block)
        prepareReleaseSelf()
    }


    private var onStoppedHandler: ((Application) -> Unit)? = null
    private var hasSubscriped= false

    /**
     * 指定完启动后和关闭时的代码块，需要调用subscribeEvent()进行订阅
     * */
    private fun prepareReleaseSelf() {
        if(hasSubscriped) return

        hasSubscriped = true

        val monitor = application.monitor

        //停止后取消订阅的动作
        onStoppedHandler = {
            if (onStoppedHandler != null) {
                onStartedHandlers.forEach {
                    monitor.unsubscribe(KoinApplicationStarted,it)
                }
                onStoppingHandlers.forEach {
                    monitor.unsubscribe(ApplicationStopping, it)
                }
                monitor.unsubscribe(ApplicationStopped, onStoppedHandler!!)
            }
        }

        if (onStoppedHandler != null) {
            //订阅停止后的动作
            monitor.subscribe(ApplicationStopped, onStoppedHandler!!)
        }

    }
}