package com.github.rwsbillyang.ktorKit.log

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import org.slf4j.LoggerFactory

/**
 * 控制台输出 (Console Appender)
 * 基础文件输出 (File Appender)
 *  按时间/大小滚动的日志文件 (RollingFileAppender)：
 *  时间滚动策略（TimeBasedRollingPolicy，例如每日切分）
 *  大小与时间混合滚动策略（SizeAndTimeBasedRollingPolicy，例如每日切分且单个文件超 100MB 切分）
 *  按文件大小滚动策略（SizeBasedTriggeringPolicy + FixedWindowRollingPolicy）
 *
 * 在 Logback 编程式 API 中，配置组件（Encoder、RollingPolicy、TriggeringPolicy、Appender）必须先赋值 context，最后显式调用 .start()，否则组件不会正常生效或引发 NullPointer。
 * SizeAndTimeBasedRollingPolicy 的 fileNamePattern： 必须同时包含 %d{...}（时间占位符）和 %i（递增索引占位符），结尾加上 .gz 或 .zip 会让 Logback 在滚动时自动进行后台异步压缩。
 * setParent(rfa) 绑定： 在使用 RollingPolicy 时，必须调用 policy.setParent(rollingFileAppender) 将策略与 Appender 进行双向关联绑定。
 *
 *  <code>
 * //同时开启控制台 + 按时间和大小滚动的日志文件
 * fun main() {
 *     // 初始化日志配置
 *     LogBackUtil.init(defaultLevel = Level.INFO) {
 *         // 1. 输出到控制台
 *         addConsole()
 *
 *         // 2. 输出到文件，按时间和大小双重切分（自动压缩 .gz）
 *         addSizeAndTimeBasedRollingFile(
 *             activeFileName = "logs/ktor-app.log",
 *             fileNamePattern = "logs/archived/ktor-app-%d{yyyy-MM-dd}.%i.log.gz",
 *             maxFileSize = "50MB",   // 单个日志文件最大 50MB
 *             maxHistory = 30,         // 保留最近 30 天/30 批次
 *             totalSizeCap = "10GB"    // 所有日志文件最大上限总量 10GB
 *         )
 *     }
 *
 *     // 细粒度控制第三方库级别
 *     LogBackUtil.setLogLevel("io.netty", Level.WARN)
 *     LogBackUtil.setLogLevel("com.zaxxer.hikari", Level.INFO)
 *
 *     embeddedServer(Netty, port = 8080) {
 *         // ... Ktor 应用逻辑
 *     }.start(wait = true)
 * }
 *
 *
 * //固定文件大小切分（适合日志空间极度受限环境）
 * LogBackUtil.init(defaultLevel = Level.INFO) {
 *     addConsole()
 *
 *     // 只保留最多 5 个 10MB 的历史文件（app-1.log ~ app-5.log）
 *     addFixedWindowSizeRollingFile(
 *         activeFileName = "logs/app.log",
 *         fileNamePattern = "logs/app-%i.log",
 *         maxFileSize = "10MB",
 *         minIndex = 1,
 *         maxIndex = 5
 *     )
 * }
 *  </code>
 * */
object LogBackUtil {

    //彩色日志输出（Console Color）：如果希望控制台日志带有颜色，可以将 pattern 中的 %-5level 改为 %highlight(%-5level)，%logger{36} 改为 %cyan(%logger{36}) 等。
    private const val DEFAULT_PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

    /**
     * 获取 Logback 上下文并重置
     */
    fun resetContext(): LoggerContext? {
        val loggerContext = LoggerFactory.getILoggerFactory() as? LoggerContext ?: return null
        loggerContext.reset()
        return loggerContext
    }

    /**
     * 基础初始化配置 Builder 模式或一体化配置
     */
    fun init(
        defaultLevel: Level = Level.INFO,
        pattern: String = DEFAULT_PATTERN,
        block: LogConfigBuilder.() -> Unit = {}
    ) {
        val loggerContext = resetContext() ?: return
        val builder = LogConfigBuilder(loggerContext, pattern)
        builder.block()

        val rootLogger = loggerContext.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME)
        rootLogger.level = defaultLevel

        builder.appenders.forEach { appender ->
            rootLogger.addAppender(appender)
        }
    }

    /**
     * 动态修改指定 Package/Class 的日志级别
     */
    fun setLogLevel(loggerName: String, level: Level) {
        val loggerContext = LoggerFactory.getILoggerFactory() as? LoggerContext ?: return
        loggerContext.getLogger(loggerName).level = level
    }

    // =========================================================================
    // 配置 Builder 类
    // =========================================================================
    class LogConfigBuilder(
        private val context: LoggerContext,
        private val pattern: String
    ) {
        internal val appenders = mutableListOf<ch.qos.logback.core.Appender<ILoggingEvent>>()

        private fun createEncoder(): PatternLayoutEncoder {
            return PatternLayoutEncoder().also {
                it.context = context
                it.pattern = pattern
                it.start()
            }
        }

        /**
         * 1. 添加控制台 Console Appender
         */
        fun addConsole(name: String = "STDOUT") {
            val appender = ConsoleAppender<ILoggingEvent>().also {
                it.context = context
                it.name = name
                it.encoder = createEncoder()
                it.start()
            }
            appenders.add(appender)
        }

        /**
         * 2. 添加基础文件 File Appender (单文件不切分)
         */
        fun addFile(
            filePath: String,
            name: String = "FILE",
            append: Boolean = true
        ) {
            val appender = FileAppender<ILoggingEvent>().also {
                it.context = context
                it.name = name
                it.file = filePath
                it.isAppend = append
                it.encoder = createEncoder()
                it.start()
            }
            appenders.add(appender)
        }

        /**
         * 3.1 按【时间和文件大小】双重策略切分 (最常用推荐配置)
         * 例如：每天生成一个文件，如果当天文件超过 100MB 则自动递增编号 app-2026-10-09.0.log
         */
        fun addSizeAndTimeBasedRollingFile(
            activeFileName: String,
            fileNamePattern: String, // 示例: "logs/app-%d{yyyy-MM-dd}.%i.log.gz"
            maxFileSize: String = "100MB",
            maxHistory: Int = 30,
            totalSizeCap: String = "10GB",
            name: String = "SIZE_TIME_ROLLING_FILE"
        ) {
            val appender = RollingFileAppender<ILoggingEvent>().also { rfa ->
                rfa.context = context
                rfa.name = name
                rfa.file = activeFileName
                rfa.encoder = createEncoder()

                val policy = SizeAndTimeBasedRollingPolicy<ILoggingEvent>().also { p ->
                    p.context = context
                    p.setParent(rfa)
                    p.fileNamePattern = fileNamePattern
                    p.setMaxFileSize(FileSize.valueOf(maxFileSize))
                    p.maxHistory = maxHistory
                    p.setTotalSizeCap(FileSize.valueOf(totalSizeCap))
                    p.start()
                }

                rfa.rollingPolicy = policy
                rfa.triggeringPolicy = policy
                rfa.start()
            }
            appenders.add(appender)
        }

        /**
         * 3.2 按【时间周期】策略切分 (例如：每天/每小时切分一次)
         */
        fun addTimeBasedRollingFile(
            activeFileName: String,
            fileNamePattern: String, // 示例: "logs/app-%d{yyyy-MM-dd}.log"
            maxHistory: Int = 30,
            totalSizeCap: String = "5GB",
            name: String = "TIME_ROLLING_FILE"
        ) {
            val appender = RollingFileAppender<ILoggingEvent>().also { rfa ->
                rfa.context = context
                rfa.name = name
                rfa.file = activeFileName
                rfa.encoder = createEncoder()

                val policy = TimeBasedRollingPolicy<ILoggingEvent>().also { p ->
                    p.context = context
                    p.setParent(rfa)
                    p.fileNamePattern = fileNamePattern
                    p.maxHistory = maxHistory
                    p.setTotalSizeCap(FileSize.valueOf(totalSizeCap))
                    p.start()
                }

                rfa.rollingPolicy = policy
                rfa.start()
            }
            appenders.add(appender)
        }

        /**
         * 3.3 按【文件大小与固定窗口】策略切分
         * 例如：单个文件满 10MB 就滚动归档为 app1.log, app2.log ... 最多保留 5 个
         */
        fun addFixedWindowSizeRollingFile(
            activeFileName: String,
            fileNamePattern: String, // 示例: "logs/app-%i.log" (%i 是索引)
            maxFileSize: String = "10MB",
            minIndex: Int = 1,
            maxIndex: Int = 5,
            name: String = "FIXED_WINDOW_ROLLING_FILE"
        ) {
            val appender = RollingFileAppender<ILoggingEvent>().also { rfa ->
                rfa.context = context
                rfa.name = name
                rfa.file = activeFileName
                rfa.encoder = createEncoder()

                // 1. 触发策略：按照文件大小触发
                val triggeringPolicy = SizeBasedTriggeringPolicy<ILoggingEvent>().also { tp ->
                    tp.context = context
                    tp.maxFileSize = FileSize.valueOf(maxFileSize)
                    tp.start()
                }

                // 2. 滚动策略：固定编号窗口
                val rollingPolicy = FixedWindowRollingPolicy().also { rp ->
                    rp.context = context
                    rp.setParent(rfa)
                    rp.fileNamePattern = fileNamePattern
                    rp.minIndex = minIndex
                    rp.maxIndex = maxIndex
                    rp.start()
                }

                rfa.triggeringPolicy = triggeringPolicy
                rfa.rollingPolicy = rollingPolicy
                rfa.start()
            }
            appenders.add(appender)
        }
    }
}