package org.fileservice.service

import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.ObjectWriteResponse
import io.minio.PutObjectArgs
import io.minio.http.Method
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.fileservice.connection.MinioClientManager
import org.fileservice.connection.MinioConfig
import org.fileservice.model.FileMetadata
import org.fileservice.model.UploadResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FileServiceTest {
  private val mockClient = mockk<MinioClient>(relaxed = true)
  private val testBucket = "test-bucket"
  private lateinit var service: FileService

  @BeforeEach
  fun setup() {
    mockkObject(MinioConfig.Companion)
    every { MinioConfig.load() } returns
        MinioConfig(
            minioEndpoint = "http://localhost:9000",
            minioAccessKey = "accessKey",
            minioSecretKey = "secretKey",
            minioBucketName = testBucket,
            apiHost = "localhost",
            apiPort = 8080)
    mockkObject(MinioClientManager)
    every { MinioClientManager.client } returns mockClient

    service = FileService()
  }

  @Test
  fun `upload generates id with folder prefix and puts object`() = runBlocking {
    val metadata =
        FileMetadata(
            user_id = "user1",
            private = true,
            mime_type = "text/plain",
            file_name = "test.txt",
            size = 4,
            temp = false,
            folder = "folder1")
    val data = "data".toByteArray()

    val slot = slot<PutObjectArgs>()
    val mockResponse = mockk<ObjectWriteResponse>()
    every { mockClient.putObject(capture(slot)) } returns mockResponse

    val response: UploadResponse = service.upload(metadata, data)

    val idParts = response.id.split('/')
    assertEquals(2, idParts.size)
    assertEquals("folder1", idParts[0])
    UUID.fromString(idParts[1])

    verify(exactly = 1) { mockClient.putObject(any()) }
    val args = slot.captured
    assertEquals(testBucket, args.bucket())
    assertEquals("text/plain", args.contentType())
    assertEquals(data.size.toLong(), args.objectSize())
  }

  @Test
  fun `getFileUrl requests presigned GET URL`() = runBlocking {
    val fileId = "file1"
    val expectedUrl = "http://localhost/file1"
    every { mockClient.getPresignedObjectUrl(any<GetPresignedObjectUrlArgs>()) } returns expectedUrl

    val urlResponse = service.getFileUrl(fileId)
    assertEquals(expectedUrl, urlResponse.url)

    verify(exactly = 1) {
      mockClient.getPresignedObjectUrl(
          match {
            it.bucket() == testBucket && it.`object`() == fileId && it.method() == Method.GET
          })
    }
  }
}
