package org.fileservice.router

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.ByteArrayOutputStream
import kotlinx.serialization.json.Json
import org.fileservice.model.FileMetadata
import org.fileservice.model.FixFileMetadata
import org.fileservice.model.UploadResponse
import org.fileservice.service.IFileService

fun Routing.registerFileRoutesWith(fileService: IFileService) {
  route("/files") {
    post("/upload") {
      val multipart = call.receiveMultipart()
      var metadata: FileMetadata? = null
      var fileBytes: ByteArray? = null

      multipart.forEachPart { part ->
        when (part) {
          is PartData.FormItem -> {
            if (part.name == "meta") {
              try {
                metadata = Json.decodeFromString(FileMetadata.serializer(), part.value)
              } catch (e: Exception) {
                throw BadRequestException("Invalid meta JSON: ${e.message}")
              }
            }
          }
          is PartData.FileItem -> {
            if (part.name == "file") {
              val baos = ByteArrayOutputStream()
              part.streamProvider().use { input -> input.copyTo(baos) }
              fileBytes = baos.toByteArray()
            }
          }
          else -> {}
        }
        part.dispose()
      }

      if (metadata == null || fileBytes == null) {
        throw BadRequestException("Missing metadata or file")
      }

      val response: UploadResponse = fileService.upload(metadata!!, fileBytes!!)
      call.respond(response)
    }

    post("/fixupload") {
      val multipart = call.receiveMultipart()
      var fixMetadata: FixFileMetadata? = null
      var fileBytes: ByteArray? = null

      multipart.forEachPart { part ->
        when (part) {
          is PartData.FormItem -> {
            if (part.name == "meta") {
              try {
                fixMetadata = Json.decodeFromString(FixFileMetadata.serializer(), part.value)
              } catch (e: Exception) {
                throw BadRequestException("Invalid meta JSON: ${e.message}")
              }
            }
          }
          is PartData.FileItem -> {
            if (part.name == "file") {
              val baos = ByteArrayOutputStream()
              part.streamProvider().use { input -> input.copyTo(baos) }
              fileBytes = baos.toByteArray()
            }
          }
          else -> {}
        }
        part.dispose()
      }

      if (fixMetadata == null) {
        throw BadRequestException("Missing metadata")
      }

      val response = fileService.fixUpload(fixMetadata!!, fileBytes)
      call.respond(response)
    }

    get("{id...}") {
      val idParts = call.parameters.getAll("id")
      val fileId = idParts?.joinToString("/") ?: throw BadRequestException("Missing file id")

      val (metadata, content) = fileService.getFile(fileId)

      call.response.header("X-Meta-File-Name", metadata.file_name)
      call.response.header("X-Meta-Folder", metadata.folder ?: "")
      call.response.header("X-Meta-Mime-Type", metadata.mime_type)
      call.response.header("X-Meta-Private", metadata.private.toString())
      call.response.header("X-Meta-Size", metadata.size.toString())
      metadata.temp?.let { call.response.header("X-Meta-Temp", it.toString()) }
      metadata.user_id?.let { call.response.header("X-Meta-User-Id", it) }

      call.response.header(
          HttpHeaders.ContentDisposition, "attachment; filename=\"${metadata.file_name}\"")

      call.respondBytes(content, ContentType.parse(metadata.mime_type))
    }

    get("/url/{id...}") {
      val idParts = call.parameters.getAll("id")
      val fileId = idParts?.joinToString("/") ?: throw BadRequestException("Missing file id")
      val response = fileService.getFileUrl(fileId)
      call.respond(response)
    }
  }
}
