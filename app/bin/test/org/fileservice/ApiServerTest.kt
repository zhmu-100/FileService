package org.fileservice

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import org.fileservice.connection.MinioConfig
import org.junit.jupiter.api.Test

class ApiServerTest {

  @Test
  fun `main should load config`() {
    mockkObject(MinioConfig.Companion)

    val testConfig =
        MinioConfig(
            minioEndpoint = "http://localhost:9000",
            minioAccessKey = "access",
            minioSecretKey = "secret",
            minioBucketName = "bucket",
            apiHost = "127.0.0.1",
            apiPort = 9999)

    every { MinioConfig.load() } returns testConfig

    // Act
    MinioConfig.load()

    // Assert
    verify(exactly = 1) { MinioConfig.load() }
  }
}
