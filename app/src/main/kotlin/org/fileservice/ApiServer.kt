package org.fileservice

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.minio.connection.MinioConfig
import org.minio.route.registerFileRoutes

fun main() {
  val config = MinioConfig.load()

  embeddedServer(Netty, host = config.apiHost, port = config.apiPort) {
    install(ContentNegotiation) {
      json()
    }
    registerFileRoutes()
  }.start(wait = true)
}
