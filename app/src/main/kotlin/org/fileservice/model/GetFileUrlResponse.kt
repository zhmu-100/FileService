package org.fileservice.model

import kotlinx.serialization.Serializable

/**
 * Ответ на запрос получения временного URL для скачивания файла.
 *
 * @property url Преподписанный урл, действует 1 час.
 */
@Serializable data class GetFileUrlResponse(val url: String)
