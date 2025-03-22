package org.fileservice.model

import kotlinx.serialization.Serializable

@Serializable
data class FileMetadata(
  val user_id: String? = null,
  val private: Boolean,
  val mime_type: String,
  val file_name: String,
  val size: Long,
  val temp: Boolean? = null,
  val ecosystem: String? = null,
  val folder: String? = null
)