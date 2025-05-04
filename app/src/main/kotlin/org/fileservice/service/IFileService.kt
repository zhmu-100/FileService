package org.fileservice.service

import org.fileservice.model.FileMetadata
import org.fileservice.model.FixFileMetadata
import org.fileservice.model.GetFileUrlResponse
import org.fileservice.model.UploadResponse

/**
 * Интерфейс для работы с файлами через MinIO.
 *
 * Определяет базовые операции:
 * - загрузка нового файла,
 * - фиксированная загрузка (замена) существующего файла,
 * - получение файла (метаданные и содержимое),
 * - получение временного URL для доступа к файлу.
 */
interface IFileService {

  /**
   * Загружает новый файл в MinIO.
   *
   * @param metadata Метаданные файла (например, пользователь, MIME-тип, имя файла, размер и т.д.).
   * @param file Содержимое файла в виде массива байтов.
   * @return [UploadResponse] с идентификатором загруженного файла (полный объектный ключ).
   */
  suspend fun upload(metadata: FileMetadata, file: ByteArray): UploadResponse

  /**
   * Выполняет фиксированную загрузку файла (замену существующего файла) в MinIO.
   *
   * Если новое содержимое не передано (file == null), то производится обновление только метаданных,
   * сохраняя существующее содержимое. При этом обновляются только поля: [mime_type], [file_name] и
   * [size], остальные метаданные остаются без изменений.
   *
   * @param fixMetadata Метаданные для фиксированной загрузки, включая file_id (полный ключ
   * объекта),
   * ```
   *                    новый MIME-тип, имя файла и размер.
   * @param file
   * ```
   * Новое содержимое файла в виде массива байтов, или null если содержимое не меняется.
   * @return [UploadResponse] с идентификатором (ключом) обновленного файла.
   */
  suspend fun fixUpload(fixMetadata: FixFileMetadata, file: ByteArray?): UploadResponse

  /**
   * Получает файл из MinIO по его идентификатору.
   *
   * Возвращает пару, где первый элемент — метаданные файла, а второй — содержимое файла в виде
   * массива байтов.
   *
   * @param fileId Идентификатор файла (полный ключ объекта).
   * @return Пара: [FileMetadata] и содержимое файла ([ByteArray]).
   */
  suspend fun getFile(fileId: String): Pair<FileMetadata, ByteArray>

  /**
   * Генерирует временный URL для доступа к файлу.
   *
   * URL действителен в течение определённого времени (например, 1 часа).
   *
   * @param fileId Идентификатор файла (полный ключ объекта).
   * @return [GetFileUrlResponse] с временным URL.
   */
  suspend fun getFileUrl(fileId: String): GetFileUrlResponse
}
