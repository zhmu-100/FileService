package org.fileservice.model

import kotlinx.serialization.Serializable

/**
 * Метаданные файла, обновляемого в хранилище.
 *
 * @property file_id Идентификатор файла в MiniO.
 * @property mime_type MIME-тип файла.
 * @property file_name Имя файла.
 * @property size Размер файла в байтах
 */
@Serializable
data class FixFileMetadata(
    val file_id: String,
    val mime_type: String,
    val file_name: String,
    val size: Long
)
