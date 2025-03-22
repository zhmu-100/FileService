package org.fileservice

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.fileservice.connection.MinioConfig
import org.fileservice.router.registerFileRoutes

/**
 * Точка входа в приложение FileService.
 *
 * Данный файл выполняет следующие задачи:
 * 1. Загружает конфигурацию приложения из переменных окружения с помощью [MinioConfig.load()].
 * ```
 *    Конфигурация включает параметры подключения к MinIO (MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, MINIO_BUCKET_NAME)
 *    и настройки для REST API (API_HOST, API_PORT).
 * ```
 * 2. Создает встроенный сервер Ktor с использованием Netty, на указанном хосте и порту из
 * конфигурации.
 *
 * 3. Настраивает сервер для работы с JSON через плагин ContentNegotiation.
 *
 * 4. Регистрирует маршруты приложения, используя функцию [registerFileRoutes()], которая определяет
 * REST эндпоинты
 * ```
 *    для загрузки, обновления, получения файла и получения временного URL.
 * ```
 */
fun main() {
  val config = MinioConfig.load()

  embeddedServer(Netty, host = config.apiHost, port = config.apiPort) {
        install(ContentNegotiation) { json() }
        registerFileRoutes()
      }
      .start(wait = true)
}
