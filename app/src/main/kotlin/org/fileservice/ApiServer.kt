package org.fileservice

import com.mad.client.LoggerClient
import com.mad.model.LogLevel
import io.github.cdimascio.dotenv.dotenv
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.fileservice.connection.MinioConfig
import org.fileservice.router.registerFileRoutes

/** Глобальный клиент логирования для использования во всем приложении */
val loggerClient: LoggerClient by lazy {
  val dotenv = dotenv()

  val redisHost = dotenv["REDIS_HOST"] ?: "localhost"
  val redisPort = dotenv["REDIS_PORT"]?.toIntOrNull() ?: 6379
  val redisPassword = dotenv["REDIS_PASSWORD"] ?: ""

  LoggerClient(host = redisHost, port = redisPort, password = redisPassword)
}

/**
 * Точка входа в приложение FileService.
 *
 * Данный файл выполняет следующие задачи:
 * 1. Загружает конфигурацию приложения из переменных окружения с помощью [MinioConfig.load()].
 * 2. Создает встроенный сервер Ktor с использованием Netty, на указанном хосте и порту из
 * конфигурации.
 * 3. Настраивает сервер для работы с JSON через плагин ContentNegotiation.
 * 4. Регистрирует маршруты приложения, используя функцию [registerFileRoutes()], которая определяет
 */
fun main() {
  val config = MinioConfig.load()

  // Логируем запуск сервиса
  loggerClient.logActivity(
      event = "FileService started",
      additionalData =
          mapOf(
              "apiHost" to config.apiHost,
              "apiPort" to config.apiPort.toString(),
              "minioEndpoint" to config.minioEndpoint,
              "minioBucket" to config.minioBucketName))

  try {
    embeddedServer(Netty, host = config.apiHost, port = config.apiPort) {
          install(ContentNegotiation) { json() }
          registerFileRoutes()
        }
        .start(wait = true)
  } catch (e: Exception) {
    // Логируем ошибку при запуске сервиса
    loggerClient.logError(
        event = "FileService startup failed",
        errorMessage = e.message ?: "Unknown error",
        stackTrace = e.stackTraceToString())
    throw e
  } finally {
    // Закрываем соединение с логгером при завершении работы
    Runtime.getRuntime()
        .addShutdownHook(
            Thread {
              loggerClient.logActivity("FileService shutdown", level = LogLevel.INFO)
              loggerClient.close()
            })
  }
}
