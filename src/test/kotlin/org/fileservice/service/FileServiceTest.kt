package org.fileservice.service

import io.minio.MinioClient
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*

class FileServiceTest {
  private val mockClient = mockk<MinioClient>(relaxed = true)
  private val testBucket = "test-bucket"
  private lateinit var service: FileService
}
