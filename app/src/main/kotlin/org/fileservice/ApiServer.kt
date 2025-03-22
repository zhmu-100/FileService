package org.fileservice

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import org.fileservice.route.registerFileRoutes

fun main() {
  val host = System.getenv("HOST") ?: "0.0.0.0"
  val port = System.getenv("PORT")?.toInt() ?: 8080

  embeddedServer(Netty, host = host, port = port) {
    module()
  }.start(wait = true)
}

fun Application.module() {
  install(ContentNegotiation){
    json()
  }

  registerFileRoutes()
}