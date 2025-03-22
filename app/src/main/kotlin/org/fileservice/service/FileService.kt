package org.fileservice.service

import io.minio.GetObjectArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.StatObjectArgs
import io.minio.http.Method
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fileservice.connection.MinioClientManager
import org.fileservice.model.FileMetadata
import org.fileservice.model.FixFileMetadata
import org.fileservice.model.GetFileUrlResponse
import org.fileservice.model.UploadResponse
import org.fileservice.connection.MinioConfig
import java.io.ByteArrayInputStream
import java.util.UUID

class FileService : IFileService {

  private val client: MinioClient = MinioClientManager.client
  private val bucketName: String = MinioConfig.load().minioBucketName

  override suspend fun upload(metadata: FileMetadata, file: ByteArray): UploadResponse {
    return withContext(Dispatchers.IO) {
      val fileId = UUID.randomUUID().toString()
      val objectName = if (!metadata.folder.isNullOrBlank()) {
        "${metadata.folder}/$fileId"
      } else {
        fileId
      }
      val inputStream = ByteArrayInputStream(file)

      client.putObject(
        PutObjectArgs.builder()
          .bucket(bucketName)
          .`object`(objectName)
          .stream(inputStream, file.size.toLong(), -1)
          .contentType(metadata.mime_type)
          .headers(buildMetadataHeaders(metadata))
          .build()
      )
      inputStream.close()
      UploadResponse(id = objectName)
    }
  }

  override suspend fun fixUpload(fixMetadata: FixFileMetadata, file: ByteArray?): UploadResponse {
    return withContext(Dispatchers.IO) {
      val objectName = fixMetadata.file_id

      val newFileBytes: ByteArray = if (file == null) {
        val inputStream = client.getObject(
          GetObjectArgs.builder()
            .bucket(bucketName)
            .`object`(objectName)
            .build()
        )
        val bytes = inputStream.readBytes()
        inputStream.close()
        bytes
      } else {
        file
      }

      val statResponse = client.statObject(
        StatObjectArgs.builder()
          .bucket(bucketName)
          .`object`(objectName)
          .build()
      )
      val currentHeaders: Map<String, String> = statResponse.headers().toMultimap()
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
          .build()
      )
      inputStream.close()
      UploadResponse(id = objectName)
    }
  }


  override suspend fun getFile(fileId: String): Pair<FileMetadata, ByteArray> {
    return withContext(Dispatchers.IO) {
      val obj = client.getObject(
        GetObjectArgs.builder()
          .bucket(bucketName)
          .`object`(fileId)
          .build()
      )

      val bytes = obj.readBytes()
      obj.close()

      val stat = client.statObject(
        StatObjectArgs.builder()
          .bucket(bucketName)
          .`object`(fileId)
          .build()
      )

      val headers = stat.headers().toMultimap()
        .filterKeys { it.lowercase().startsWith("x-amz-meta-") }
        .mapKeys { it.key.lowercase() }

      val metadata = FileMetadata(
        user_id = headers["x-amz-meta-user_id"]?.firstOrNull(),
        private = headers["x-amz-meta-private"]?.firstOrNull()?.toBoolean() ?: false,
        mime_type = headers["x-amz-meta-mime_type"]?.firstOrNull() ?: "application/octet-stream",
        file_name = headers["x-amz-meta-file_name"]?.firstOrNull() ?: fileId,
        size = headers["x-amz-meta-size"]?.firstOrNull()?.toLongOrNull() ?: bytes.size.toLong(),
        temp = headers["x-amz-meta-temp"]?.firstOrNull()?.toBoolean(),
        folder = headers["x-amz-meta-folder"]?.firstOrNull()
      )

      Pair(metadata, bytes)
    }
  }

  override suspend fun getFileUrl(fileId: String): GetFileUrlResponse {
    return withContext(Dispatchers.IO) {
      val url = client.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
          .method(Method.GET)
          .bucket(bucketName)
          .`object`(fileId)
          .expiry(60 * 60) // 60 seconds * 60 minutes = 1 hour
          .build()
      )
      GetFileUrlResponse(url = url)
    }
  }

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
