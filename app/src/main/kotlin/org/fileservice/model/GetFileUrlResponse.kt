package org.fileservice.model

import kotlinx.serialization.Serializable

@Serializable
data class GetFileUrlResponse(
  val url: String
)
