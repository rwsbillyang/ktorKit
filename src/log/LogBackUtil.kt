/*
 * Copyright © 2022 rwsbillyang@qq.com
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2022-12-14 20:19
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

package com.github.rwsbillyang.ktorKit.log

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.LevelFilter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.core.spi.FilterReply
import ch.qos.logback.core.util.FileSize
import ch.qos.logback.core.util.OptionHelper
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.util.*


object LogBackUtil {
    private const val maxFileSize = "50MB"
    private const val totalSizeCap = "10GB"
    private const val MaxHistory = 30
    private val myPattern =  "%d{YYYY-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

    //https://logback.qos.ch/manual/configuration.html
    fun setupForConsole(logLevel:Level = Level.INFO) {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext

        val patternLayoutEncoder = PatternLayoutEncoder().apply {
            pattern = myPattern
            context = loggerContext
            start()
        }

        val consoleAppender = ConsoleAppender<ILoggingEvent>().apply {
            name = "stdout"
            setEncoder(patternLayoutEncoder)
            context = loggerContext
            start()
        }

        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        with(root){
            detachAndStopAllAppenders()
            level = logLevel
            addAppender(consoleAppender)
        }
    }
    fun setupForFile(logLevel:Level = Level.INFO) {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext

        val patternLayoutEncoder = PatternLayoutEncoder().apply {
            pattern = myPattern
            context = loggerContext
            start()
        }

        //https://logbackcn.gitbook.io/logback/04-di-si-zhang-appenders
        val appender = FileAppender<ILoggingEvent>().apply {
            name = "file"
            file = "log/ktor.log"
            context = loggerContext
            isAppend = true
            isPrudent = false
            setEncoder(patternLayoutEncoder)
        }
        appender.start()

        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        with(root){
            detachAndStopAllAppenders()
            level = logLevel
            addAppender(appender)
        }
    }

    fun setupForRollingFile(logLevel:Level = Level.INFO) {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext

        val patternLayoutEncoder = PatternLayoutEncoder().apply {
            pattern = myPattern
            context = loggerContext
            start()
        }

        //https://logbackcn.gitbook.io/logback/04-di-si-zhang-appenders
        val appender = RollingFileAppender<ILoggingEvent>().apply {
            name = "rollingFile"
            file = "log/ktor.log"
            context = loggerContext
            isAppend = true
            isPrudent = false
            setEncoder(patternLayoutEncoder)
        }

        val rollingPolicy = TimeBasedRollingPolicy<ILoggingEvent>().apply {
            setParent(appender)
            context = loggerContext
            fileNamePattern = OptionHelper.substVars("log/ktor-%d{yyyy-MM-dd}_%i.log", context)// 设置文件名模式
            timeBasedFileNamingAndTriggeringPolicy = SizeAndTimeBasedFNATP<ILoggingEvent>().apply{setMaxFileSize(FileSize.valueOf(maxFileSize))} // 最大日志文件大小
            maxHistory = MaxHistory// 设置最大历史记录为30条
            setTotalSizeCap(FileSize.valueOf(totalSizeCap)) // 总大小限制
        }
        appender.rollingPolicy = rollingPolicy
        appender.start()

        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        with(root){
            detachAndStopAllAppenders()
            level = logLevel
            isAdditive = false
            addAppender(appender)
        }
    }



    /**
     * description: 创建日志文件的file appender
     *
     * @param name
     * @param level
     * @return ch.qos.logback.core.rolling.RollingFileAppender
     */
    private fun createAppender(filename: String, level: Level, loggerContext: LoggerContext): RollingFileAppender<*>? {
        val appender = RollingFileAppender<ILoggingEvent>().apply {
            addFilter(createLevelFilter(level))
            context = loggerContext
            isAppend = true
            isPrudent = false
            name = filename.uppercase(Locale.getDefault()) + "-" + level.levelStr.uppercase(Locale.getDefault())
            file = OptionHelper.substVars("\${logPath}-" + filename + "-" + level.levelStr.lowercase(Locale.getDefault()) + ".log", loggerContext)

            rollingPolicy = createRollingPolicy(filename, level, loggerContext, this)
        }

        appender.setEncoder(createEncoder(loggerContext))
        appender.start()
        return appender
    }


    /**
     * description: 创建窗口输入的appender
     *
     * @param
     * @return ch.qos.logback.core.ConsoleAppender
     */
    private fun createConsoleAppender(loggerContext: LoggerContext): ConsoleAppender<ILoggingEvent> {
        val appender = ConsoleAppender<ILoggingEvent>().apply {
            context = loggerContext
            name = "server"
        }

        val f = createLevelFilter(Level.INFO)
        val e = createEncoder(loggerContext)
        appender.addFilter(f)
        appender.encoder = e
        appender.start()
        return appender
    }



    /**
     * description: 设置日志的滚动策略
     *
     * @param name
     * @param level
     * @param mycontext
     * @param appender
     * @return ch.qos.logback.core.rolling.TimeBasedRollingPolicy
     */
    private fun  createRollingPolicy(
        name: String,
        level: Level,
        mycontext: LoggerContext,
        appender: FileAppender<*>
    ): TimeBasedRollingPolicy<ILoggingEvent> {
        val rollingPolicyBase = TimeBasedRollingPolicy<ILoggingEvent>().apply {
             // 设置上下文，每个logger都关联到logger上下文，默认上下文名称为default。
            // 但可以使用<scope="context">设置成其他名字，用于区分不同应用程序的记录。一旦设置，不能修改。
            context = mycontext
            setParent(appender)// 设置父节点是appender
            fileNamePattern = OptionHelper.substVars("log/\${LOG_NAME_PREFIX}-$name-${level.levelStr.lowercase(Locale.getDefault())}%d{yyyy-MM-dd}_%i.log", context)// 设置文件名模式
            timeBasedFileNamingAndTriggeringPolicy = SizeAndTimeBasedFNATP<ILoggingEvent>().apply{setMaxFileSize(FileSize.valueOf(maxFileSize))} // 最大日志文件大小
            maxHistory = MaxHistory// 设置最大历史记录为30条
            setTotalSizeCap(FileSize.valueOf(totalSizeCap)) // 总大小限制
        }

        rollingPolicyBase.start()
        return rollingPolicyBase
    }


    /**
     * description: 设置日志的输出格式
     *
     * @param myContext
     * @return ch.qos.logback.classic.encoder.PatternLayoutEncoder
     */
    private fun createEncoder(myContext: LoggerContext): PatternLayoutEncoder {
        val encoder = PatternLayoutEncoder().apply {
            // 设置上下文，每个logger都关联到logger上下文，默认上下文名称为default。
            // 但可以使用<scope="context">设置成其他名字，用于区分不同应用程序的记录。一旦设置，不能修改。
            context = myContext
            OptionHelper.substVars("\${pattern}", myContext)
            //pattern = pattern
            charset = Charset.forName("utf-8")
        }

        encoder.start()
        return encoder
    }


    /**
     * description: 设置打印日志的级别
     *
     * @param level
     * @return ch.qos.logback.core.filter.Filter
     */
    private fun createLevelFilter(level: Level): LevelFilter {
        val levelFilter = LevelFilter().apply {
            setLevel(level)
            onMatch = FilterReply.ACCEPT
            onMismatch = FilterReply.DENY
        }
        levelFilter.start()
        return levelFilter
    }

}