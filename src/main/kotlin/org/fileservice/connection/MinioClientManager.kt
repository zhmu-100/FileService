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
          // .endpoint(config.minioEndpoint)
          .endpoint("http://188.225.77.13:9020")
          .credentials(config.minioAccessKey, config.minioSecretKey)
          .build()
}
