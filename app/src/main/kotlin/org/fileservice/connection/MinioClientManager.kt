package org.fileservice.connection

import io.minio.MinioClient

/**
 * Менеджер клиента MinIO.
 *
 * Объект [MinioClientManager] инициализирует синглтон-клиент [MinioClient] с использованием
 * параметров, полученных из [MinioConfig.load()]. Этот клиент используется для выполнения операций
 * с MinIO, таких, как загрузка файлов, получение объектов и генерация временных URL.
 */
object MinioClientManager {
  private val config = MinioConfig.load()

  /** Экземпляр [MinioClient], инициализированный с помощью билдера. */
  val client: MinioClient =
      MinioClient.builder()
          .endpoint(config.minioEndpoint)
          .credentials(config.minioAccessKey, config.minioSecretKey)
          .build()
}
