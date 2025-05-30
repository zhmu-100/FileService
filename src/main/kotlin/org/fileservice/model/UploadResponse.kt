package org.fileservice.model

import kotlinx.serialization.Serializable

/**
 * Ответ на запрос загрузки или исправления файла.
 *
 * @property id Id файла, сохранённого в хранилище.
 */
@Serializable data class UploadResponse(val id: String)
