package org.fileservice.connection

data class MinioConfig(
  val minioEndpoint: String,
  val minioAccessKey: String,
  val minioSecretKey: String,
  val minioBucketName: String,
  val apiHost: String,
  val apiPort: Int
) {
  companion object {
    fun load(): MinioConfig {
      val dotenv = io.github.cdimascio.dotenv.dotenv()
      val endpoint = dotenv["MINIO_ENDPOINT"] ?: "http://127.0.0.1:9000"
      val accessKey = dotenv["MINIO_ACCESS_KEY"] ?: ""
      val secretKey = dotenv["MINIO_SECRET_KEY"] ?: ""
      val bucket = dotenv["MINIO_BUCKET_NAME"] ?: ""
      val host = dotenv["API_HOST"] ?: "0.0.0.0"
      val port = dotenv["API_PORT"]?.toIntOrNull() ?: 8080
      return MinioConfig(endpoint, accessKey, secretKey, bucket, host, port)
    }
  }
}
