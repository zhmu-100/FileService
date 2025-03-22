package org.fileservice.connection

import io.minio.MinioClient
import org.minio.connection.MinioConfig

object MinioClientManager {
  private val config = MinioConfig.load()

  val client: MinioClient = MinioClient.builder()
    .endpoint(config.minioEndpoint)
    .credentials(config.minioAccessKey, config.minioSecretKey)
    .build()
}
