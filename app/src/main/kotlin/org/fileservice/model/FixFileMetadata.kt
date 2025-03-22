package org.fileservice.model

import kotlinx.serialization.Serializable

@Serializable
data class FixFileMetadata(
  val file_id: String,
  val mime_type: String,
  val file_name: String,
  val size: Long
)
