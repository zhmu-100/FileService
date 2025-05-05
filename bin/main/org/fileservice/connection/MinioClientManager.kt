package org.fileservice.connection

import io.minio.MinioClient

/**
 * Менеджер клиента MinIO.
 *
 * Объект [MinioClientManager] инициализирует клиент [MinioClient] с использованием параметров env.
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
