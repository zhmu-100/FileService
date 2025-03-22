package org.fileservice.service

import org.fileservice.model.FileMetadata
import org.fileservice.model.FixFileMetadata
import org.fileservice.model.GetFileUrlResponse
import org.fileservice.model.UploadResponse

interface IFileService {

  suspend fun upload(metadata: FileMetadata, file: ByteArray): UploadResponse

  suspend fun fixUpload(fixMetadata: FixFileMetadata, file: ByteArray?): UploadResponse

  suspend fun getFile(fileId: String): Pair<FileMetadata, ByteArray>

  suspend fun getFileUrl(fileId: String): GetFileUrlResponse
}