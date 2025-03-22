package org.minio.connection

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
      val endpoint = System.getenv("MINIO_ENDPOINT") ?: "http://127.0.0.1:9000"
      val accessKey = System.getenv("MINIO_ACCESS_KEY") ?: ""
      val secretKey = System.getenv("MINIO_SECRET_KEY") ?: ""
      val bucket = System.getenv("MINIO_BUCKET_NAME") ?: ""
      val host = System.getenv("API_HOST") ?: "0.0.0.0"
      val port = System.getenv("API_PORT")?.toIntOrNull() ?: 8080
      return MinioConfig(endpoint, accessKey, secretKey, bucket, host, port)
    }
  }
}
