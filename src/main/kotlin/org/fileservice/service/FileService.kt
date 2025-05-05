package org.fileservice.service

import com.mad.model.LogLevel
import io.minio.GetObjectArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.StatObjectArgs
import io.minio.http.Method
import java.io.ByteArrayInputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fileservice.connection.MinioClientManager
import org.fileservice.connection.MinioConfig
import org.fileservice.loggerClient
import org.fileservice.model.FileMetadata
import org.fileservice.model.FixFileMetadata
import org.fileservice.model.GetFileUrlResponse
import org.fileservice.model.UploadResponse

/**
 * Реализация интерфейса [IFileService] для работы с MinIO.
 *
 * Класс обеспечивает загрузку, обновление, получение файла и генерацию временного URL. При загрузке
 * файла дополнительно передаются кастомные метаданные через HTTP-заголовки. При фиксированной
 * загрузке обновляются только выбранные поля метаданных, остальные сохраняются из текущего объекта.
 */
class FileService : IFileService {

  private val client: MinioClient = MinioClientManager.client
  private val bucketName: String = MinioConfig.load().minioBucketName

  /**
   * Загружает новый файл в MinIO.
   *
   * Генерируется новый уникальный идентификатор, который используется как имя объекта. Если в
   * метаданных указан folder, то объект сохраняется в соответствующем префиксе. Дополнительно
   * передаются кастомные метаданные через заголовки.
   *
   * @param metadata Метаданные файла.
   * @param file Содержимое файла в виде массива байтов.
   * @return [UploadResponse] с id загруженного файла.
   */
  override suspend fun upload(metadata: FileMetadata, file: ByteArray): UploadResponse {
    return withContext(Dispatchers.IO) {
      try {
        val fileId = UUID.randomUUID().toString()
        val objectName =
            if (!metadata.folder.isNullOrBlank()) {
              "${metadata.folder}/$fileId"
            } else {
              fileId
            }
        val inputStream = ByteArrayInputStream(file)

        // Логируем начало загрузки файла
        loggerClient.logActivity(
            event = "Starting file upload to MinIO",
            userId = metadata.user_id,
            additionalData =
                mapOf(
                    "objectName" to objectName,
                    "fileSize" to file.size.toString(),
                    "mimeType" to metadata.mime_type))

        client.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .stream(inputStream, file.size.toLong(), -1)
                .contentType(metadata.mime_type)
                .headers(buildMetadataHeaders(metadata))
                .build())
        inputStream.close()

        // Логируем успешное завершение загрузки
        loggerClient.logActivity(
            event = "File upload to MinIO completed",
            userId = metadata.user_id,
            additionalData = mapOf("objectName" to objectName, "bucket" to bucketName))

        UploadResponse(id = objectName)
      } catch (e: Exception) {
        // Логируем ошибку при загрузке
        loggerClient.logError(
            event = "MinIO upload error",
            errorMessage = e.message ?: "Unknown error",
            userId = metadata.user_id,
            stackTrace = e.stackTraceToString(),
            level = LogLevel.ERROR)
        throw e
      }
    }
  }

  /**
   * Выполняет фиксированную загрузку (обновление) файла.
   *
   * Метод получает текущие метаданные объекта и обновляет только поля [mime_type], [file_name] и
   * [size]. Если новое содержимое не передано (file == null), то содержимое скачивается из
   * существующего объекта.
   *
   * @param fixMetadata Метаданные для фиксированной загрузки, включающие file_id,
   * @param file
   *
   * Новое содержимое файла, или null, если содержимое остается прежним.
   * @return [UploadResponse] с id обновленного файла (при этом это тот же id, что указан в
   * fixMetadata.
   */
  override suspend fun fixUpload(fixMetadata: FixFileMetadata, file: ByteArray?): UploadResponse {
    return withContext(Dispatchers.IO) {
      try {
        // file_id содержит полный ключ объекта (в том числе папку)
        val objectName = fixMetadata.file_id

        // Логируем начало обновления файла
        loggerClient.logActivity(
            event = "Starting file update in MinIO",
            additionalData =
                mapOf("objectName" to objectName, "hasNewContent" to (file != null).toString()))

        val newFileBytes: ByteArray =
            if (file == null) {
              val inputStream =
                  client.getObject(
                      GetObjectArgs.builder().bucket(bucketName).`object`(objectName).build())
              val bytes = inputStream.readBytes()
              inputStream.close()
              bytes
            } else {
              file
            }

        val statResponse =
            client.statObject(
                StatObjectArgs.builder().bucket(bucketName).`object`(objectName).build())
        val currentHeaders: Map<String, String> =
            statResponse
                .headers()
                .toMultimap()
                .filterKeys { it.lowercase().startsWith("x-amz-meta-") }
                .mapValues { it.value.joinToString(",") }

        val mergedHeaders = currentHeaders.toMutableMap()
        mergedHeaders["x-amz-meta-file_name"] = fixMetadata.file_name
        mergedHeaders["x-amz-meta-size"] = fixMetadata.size.toString()
        mergedHeaders["x-amz-meta-mime_type"] = fixMetadata.mime_type

        val inputStream = ByteArrayInputStream(newFileBytes)
        client.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .stream(inputStream, newFileBytes.size.toLong(), -1)
                .contentType(fixMetadata.mime_type)
                .headers(mergedHeaders)
                .build())
        inputStream.close()

        // Логируем успешное обновление файла
        loggerClient.logActivity(
            event = "File update in MinIO completed",
            additionalData =
                mapOf(
                    "objectName" to objectName,
                    "bucket" to bucketName,
                    "newSize" to fixMetadata.size.toString()))

        UploadResponse(id = objectName)
      } catch (e: Exception) {
        // Логируем ошибку при обновлении
        loggerClient.logError(
            event = "MinIO update error",
            errorMessage = e.message ?: "Unknown error",
            stackTrace = e.stackTraceToString(),
            level = LogLevel.ERROR)
        throw e
      }
    }
  }

  /**
   * Получает файл из MinIO по его идентификатору.
   *
   * Метод скачивает объект и десериализует кастомные метаданные из HTTP-заголовков, возвращая их в
   * виде [FileMetadata].
   *
   * @param fileId Полный ключ объекта в бакете.
   * @return Пара, состоящая из [FileMetadata] и содержимого файла в виде [ByteArray].
   */
  override suspend fun getFile(fileId: String): Pair<FileMetadata, ByteArray> {
    return withContext(Dispatchers.IO) {
      try {
        // Логируем запрос на получение файла
        loggerClient.logActivity(
            event = "Retrieving file from MinIO",
            additionalData = mapOf("fileId" to fileId, "bucket" to bucketName))

        val obj =
            client.getObject(GetObjectArgs.builder().bucket(bucketName).`object`(fileId).build())

        val bytes = obj.readBytes()
        obj.close()

        val stat =
            client.statObject(StatObjectArgs.builder().bucket(bucketName).`object`(fileId).build())

        val headers =
            stat
                .headers()
                .toMultimap()
                .filterKeys { it.lowercase().startsWith("x-amz-meta-") }
                .mapKeys { it.key.lowercase() }

        val metadata =
            FileMetadata(
                user_id = headers["x-amz-meta-user_id"]?.firstOrNull(),
                private = headers["x-amz-meta-private"]?.firstOrNull()?.toBoolean() ?: false,
                mime_type = headers["x-amz-meta-mime_type"]?.firstOrNull()
                        ?: "application/octet-stream",
                file_name = headers["x-amz-meta-file_name"]?.firstOrNull() ?: fileId,
                size = headers["x-amz-meta-size"]?.firstOrNull()?.toLongOrNull()
                        ?: bytes.size.toLong(),
                temp = headers["x-amz-meta-temp"]?.firstOrNull()?.toBoolean(),
                folder = headers["x-amz-meta-folder"]?.firstOrNull())

        // Логируем успешное получение файла
        loggerClient.logActivity(
            event = "File retrieved from MinIO",
            userId = metadata.user_id,
            additionalData =
                mapOf(
                    "fileId" to fileId,
                    "fileName" to metadata.file_name,
                    "fileSize" to metadata.size.toString()))

        Pair(metadata, bytes)
      } catch (e: Exception) {
        // Логируем ошибку при получении файла
        loggerClient.logError(
            event = "MinIO file retrieval error",
            errorMessage = e.message ?: "Unknown error",
            stackTrace = e.stackTraceToString(),
            level = LogLevel.ERROR)
        throw e
      }
    }
  }

  /**
   * Генерирует временный URL для доступа к файлу.
   *
   * URL действителен в течение 7 дней.
   *
   * @param fileId Полный ключ объекта в ведре.
   * @return [GetFileUrlResponse] с временным URL.
   */
  override suspend fun getFileUrl(fileId: String): GetFileUrlResponse {
    return withContext(Dispatchers.IO) {
      try {
        // Логируем запрос на генерацию URL
        loggerClient.logActivity(
            event = "Generating presigned URL",
            additionalData = mapOf("fileId" to fileId, "bucket" to bucketName, "expiryDays" to "7"))

        val url =
            client.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .`object`(fileId)
                    .expiry(7 * 24 * 60 * 60) // 7 days
                    .build())

        // Логируем успешную генерацию URL
        loggerClient.logActivity(
            event = "Presigned URL generated", additionalData = mapOf("fileId" to fileId))

        GetFileUrlResponse(url = url)
      } catch (e: Exception) {
        // Логируем ошибку при генерации URL
        loggerClient.logError(
            event = "MinIO presigned URL generation error",
            errorMessage = e.message ?: "Unknown error",
            stackTrace = e.stackTraceToString(),
            level = LogLevel.ERROR)
        throw e
      }
    }
  }

  /**
   * Формирует HTTP-заголовки для передачи кастомных метаданных при загрузке файла.
   *
   * @param metadata Объект [FileMetadata] с метаданными файла.
   * @return Map с заголовками, начинающимися с "x-amz-meta-".
   */
  private fun buildMetadataHeaders(metadata: FileMetadata): Map<String, String> {
    val headers = mutableMapOf<String, String>()
    metadata.user_id?.let { headers["x-amz-meta-user_id"] = it }
    headers["x-amz-meta-private"] = metadata.private.toString()
    headers["x-amz-meta-file_name"] = metadata.file_name
    headers["x-amz-meta-size"] = metadata.size.toString()
    metadata.temp?.let { headers["x-amz-meta-temp"] = it.toString() }
    metadata.folder?.let { headers["x-amz-meta-folder"] = it }
    headers["x-amz-meta-mime_type"] = metadata.mime_type
    return headers
  }
}
