package org.fileservice.model

import kotlinx.serialization.Serializable

/**
 * Ответ на запрос получения временного URL для скачивания файла.
 *
 * @property url Временный URL для доступа к файлу (действителен ограниченное время).
 */
@Serializable data class GetFileUrlResponse(val url: String)
