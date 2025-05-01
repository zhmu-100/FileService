package org.fileservice.model

import kotlinx.serialization.Serializable

/**
 * Метаданные файла, сохраняемого в бакет.
 *
 * @property user_id Идентификатор пользователя.
 * @property private Флаг приватности файла.
 * @property mime_type MIME-тип файла.
 * @property file_name Оригинальное имя файла.
 * @property size Размер файла в байтах.
 * @property temp Флаг, указывающий, является ли файл временным.
 * @property folder Папка в бакете, куда сохранён файл.
 */
@Serializable
data class FileMetadata(
    val user_id: String? = null,
    val private: Boolean,
    val mime_type: String,
    val file_name: String,
    val size: Long,
    val temp: Boolean? = null,
    val folder: String? = null
)
