package org.fileservice.service

import io.minio.GetObjectArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.http.Method
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fileservice.connection.MinioClientManager
import org.fileservice.model.FileMetadata
import org.fileservice.model.FixFileMetadata
import org.fileservice.model.GetFileUrlResponse
import org.fileservice.model.UploadResponse
import org.minio.connection.MinioConfig
import java.io.ByteArrayInputStream
import java.util.UUID

class FileService : IFileService {

  private val client: MinioClient = MinioClientManager.client
  private val bucketName: String = MinioConfig.load().minioBucketName

  override suspend fun upload(metadata: FileMetadata, file: ByteArray): UploadResponse {
    return withContext(Dispatchers.IO) {
      val fileId = UUID.randomUUID().toString()
      val objectName = fileId
      val inputStream = ByteArrayInputStream(file)

      client.putObject(
        PutObjectArgs.builder()
          .bucket(bucketName)
          .`object`(objectName)
          .stream(inputStream, file.size.toLong(), -1)
          .contentType(metadata.mime_type)
          .build()
      )
      UploadResponse(id = fileId)
    }
  }

  override suspend fun fixUpload(fixMetadata: FixFileMetadata, file: ByteArray?): UploadResponse {
    return withContext(Dispatchers.IO) {
      val objectName = fixMetadata.file_id
      if (file != null) {
        val inputStream = ByteArrayInputStream(file)

        client.putObject(
          PutObjectArgs.builder()
            .bucket(bucketName)
            .`object`(objectName)
            .stream(inputStream, file.size.toLong(), -1)
            .contentType(fixMetadata.mime_type)
            .build()
        )
      }
      UploadResponse(id = objectName)
    }
  }

  override suspend fun getFile(fileId: String): Pair<FileMetadata, ByteArray> {
    return withContext(Dispatchers.IO) {
      val objectName = fileId

      val inputStream = client.getObject(
        GetObjectArgs.builder()
          .bucket(bucketName)
          .`object`(objectName)
          .build()
      )
      val bytes = inputStream.readBytes()
      inputStream.close()
      val metadate = FileMetadata(
        user_id = null,
        private = false,
        mime_type = "application/octet-stream",
        file_name = objectName,
        size = bytes.size.toLong()
      )
      Pair(metadate, bytes)
    }
  }

  override suspend fun getFileUrl(fileId: String): GetFileUrlResponse {
    return withContext(Dispatchers.IO) {
      val objectName = fileId

      val url = client.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
          .method(Method.GET)
          .bucket(bucketName)
          .`object`(objectName)
          .expiry(60 * 60) // 60 seconds * 60 mins = 1 hour
          .build()
      )
      GetFileUrlResponse(url = url)
    }
  }
}