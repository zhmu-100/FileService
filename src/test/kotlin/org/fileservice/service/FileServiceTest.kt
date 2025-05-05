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
}
