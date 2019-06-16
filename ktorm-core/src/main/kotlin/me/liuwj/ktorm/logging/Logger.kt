/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.liuwj.ktorm.logging

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.logging.LogLevel.*
import kotlin.reflect.jvm.jvmName

/**
 * A simple logging interface abstracting third-party logging frameworks.
 *
 * The logging levels used by Ktorm is defined in the enum class [LogLevel] in a certain order. While [TRACE] is
 * the least serious and [ERROR] is the most serious. The mapping of these log levels to the concepts used by the
 * underlying logging system is implementation dependent. The implementation should ensure, though, that this ordering
 * behaves are expected.
 *
 * By default, Ktorm uses the [ConsoleLogger] with a threshold of [INFO], that means logs are printed to the console
 * and those whose level is less than [INFO] are ignored. If you want to output logs using a specific logging framework,
 * you can choose an adapter implementation of this interface and set the [Database.logger] property.
 *
 * Ktorm prints logs at different levels:
 *
 * - Generated SQLs and their execution arguments are printed at [DEBUG] level, so if you want to see the SQLs, you
 * should configure your logging framework to enable the [DEBUG] level.
 *
 * - Detailed data of every returned entity object are printed at [TRACE] level, if you want to see them, you should
 * configure your framework to enable [TRACE].
 *
 * - Besides, start-up messages are printed at [INFO] level, warnings are printed at [WARN] level, and exceptions are
 * printed at [ERROR] level. These levels should be enabled by default.
 */
interface Logger {

    /**
     * Check if the logger instance enabled for the TRACE level.
     */
    fun isTraceEnabled(): Boolean

    /**
     * Log a message at the TRACE level.
     */
    fun trace(msg: String, e: Throwable? = null)

    /**
     * Check if the logger instance enabled for the DEBUG level.
     */
    fun isDebugEnabled(): Boolean

    /**
     * Log a message at the DEBUG level.
     */
    fun debug(msg: String, e: Throwable? = null)

    /**
     * Check if the logger instance enabled for the INFO level.
     */
    fun isInfoEnabled(): Boolean

    /**
     * Log a message at the INFO level.
     */
    fun info(msg: String, e: Throwable? = null)

    /**
     * Check if the logger instance enabled for the WARN level.
     */
    fun isWarnEnabled(): Boolean

    /**
     * Log a message at the WARN level.
     */
    fun warn(msg: String, e: Throwable? = null)

    /**
     * Check if the logger instance enabled for the ERROR level.
     */
    fun isErrorEnabled(): Boolean

    /**
     * Log a message at the ERROR level.
     */
    fun error(msg: String, e: Throwable? = null)
}

/**
 * Enum class defining logging levels in a certain order. While [TRACE] is the least serious
 * and [ERROR] is the most serials.
 */
enum class LogLevel {

    /**
     * TRACE is a log level providing the most detailed tracing information.
     */
    TRACE,

    /**
     * DEBUG is a log level providing tracing information such as generated SQLs.
     */
    DEBUG,

    /**
     * INFO is a log level for informational messages.
     */
    INFO,

    /**
     * WARN is a log level indicating a potential problem.
     */
    WARN,

    /**
     * ERROR is a log level indicating a serious failure.
     */
    ERROR
}

/**
 * Auto detect a logger implementation.
 */
internal fun detectLoggerImplementation(): Logger {
    val loggerName = Database::class.jvmName
    var result: Logger? = null

    @Suppress("SwallowedException")
    fun tryImplement(init: () -> Logger) {
        if (result == null) {
            try {
                result = init()
            } catch (ignored: ClassNotFoundException) {
            }
        }
    }

    tryImplement {
        val logger = org.slf4j.LoggerFactory.getLogger(loggerName)
        Slf4jLoggerAdapter(logger)
    }

    tryImplement {
        val logger = org.apache.commons.logging.LogFactory.getLog(loggerName)
        CommonsLoggerAdapter(logger)
    }

    tryImplement {
        Class.forName("android.util.Log")
        AndroidLoggerAdapter(loggerName)
    }

    tryImplement {
        val logger = java.util.logging.Logger.getLogger(loggerName)
        JdkLoggerAdapter(logger)
    }

    return result ?: ConsoleLogger(threshold = INFO)
}
