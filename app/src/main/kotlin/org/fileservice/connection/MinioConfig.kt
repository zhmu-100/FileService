package org.fileservice.connection

/**
 * Класс конфигурации для подключения к MinIO и настройки API.
 *
 * Содержит следующие параметры:
 * - [minioEndpoint]: URL для подключения к серверу MinIO.
 * - [minioAccessKey]: ключ доступа для MinIO.
 * - [minioSecretKey]: секретный ключ для MinIO.
 * - [minioBucketName]: имя бакета, используемого для хранения файлов.
 * - [apiHost]: хост, на котором будет запущено REST API.
 * - [apiPort]: порт, на котором будет запущено REST API.
 *
 * Метод [load] загружает конфигурацию из переменных окружения с помощью библиотеки dotenv. Если
 * какая-либо переменная не задана, используется значение по умолчанию.
 */
data class MinioConfig(
    val minioEndpoint: String,
    val minioAccessKey: String,
    val minioSecretKey: String,
    val minioBucketName: String,
    val apiHost: String,
    val apiPort: Int
) {
  companion object {
    /**
     * Загружает конфигурацию из переменных окружения.
     *
     * Использует библиотеку [io.github.cdimascio.dotenv.Dotenv] для получения значений:
     * - MINIO_ENDPOINT (по умолчанию: "http://127.0.0.1:9000")
     * - MINIO_ACCESS_KEY (по умолчанию: пустая строка)
     * - MINIO_SECRET_KEY (по умолчанию: пустая строка)
     * - MINIO_BUCKET_NAME (по умолчанию: пустая строка)
     * - API_HOST (по умолчанию: "0.0.0.0")
     * - API_PORT (по умолчанию: 8080)
     *
     * @return Экземпляр [MinioConfig] с загруженными параметрами.
     */
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
