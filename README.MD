# File Service with MinIO

MiniO fileservice:

- Загружать новые файлы с кастомными метаданными;
- Обновлять (фиксировать) существующие файлы, обновляя только выбранные метаданные (mime_type, file_name и size), при этом сохраняя остальные метаданные;
- Получать файлы (с метаданными в заголовках ответа);
- Генерировать временные URL для доступа к файлам.

### Env
```env
MINIO_ENDPOINT=http://192.168.31.209:9000
MINIO_ACCESS_KEY=yEQiIkgvZIltX4P0ILID
MINIO_SECRET_KEY=V28Y73MJne6Mc6Ag3CoS7yMy5kCox6ejhKZKQZ28
MINIO_BUCKET_NAME=test-bucket
API_HOST=0.0.0.0
API_PORT=8080
```

### Run

```bash
./gradlew run
```

## Описание эндпоинтов

### 1. POST `/files/upload`

Загружает новый файл с кастомными метаданными.

- **Тип запроса:** `multipart/form-data`
- **Поля:**
    - `meta` (Text): JSON-строка, десериализуемая в [FileMetadata]. Пример:
      ```json
      {
        "user_id": "user123",
        "private": true,
        "mime_type": "image/jpeg",
        "file_name": "2.jpg",
        "size": 204800,
        "temp": true,
        "folder": "profile_photos"
      }
      ```
    - `file` (File): Файл для загрузки.
- **Ответ:** JSON с полем `id` – полным ключом объекта (например, `"profile_photos/ab49f64d-..."`).

### 2. POST `/files/fixupload`

Выполняет обновление (фиксированную загрузку) файла. Обновляются только поля: `mime_type`, `file_name` и `size`, остальные метаданные остаются неизменными.

- **Тип запроса:** `multipart/form-data`
- **Поля:**
    - `meta` (Text): JSON-строка, десериализуемая в [FixFileMetadata]. Пример:
      ```json
      {
        "file_id": "profile_photos/ab49f64d-54ff-4e70-831b-040d4896f278",
        "mime_type": "image/jpeg",
        "file_name": "3.jpg",
        "size": 102400
      }
      ```
    - `file` (File): Новый файл для обновления. Если файл не передан, содержимое остаётся прежним.
- **Ответ:** JSON с полем `id` – полным ключом обновлённого объекта.

### 3. GET `/files/{id...}`

Получает файл по его идентификатору.

- **Путь:** Идентификатор может состоять из нескольких сегментов (например, `profile_photos/ab49f64d-54ff-4e70-831b-040d4896f278`).
- **Ответ:** Файл передаётся с установленными HTTP-заголовками:
    - `X-Meta-File-Name`, `X-Meta-Folder`, `X-Meta-Mime-Type`, `X-Meta-Private`, `X-Meta-Size`, `X-Meta-Temp`, `X-Meta-User-Id`
    - Заголовок `Content-Disposition` для скачивания.
    - Тело ответа – бинарное содержимое файла.

### 4. GET `/files/url/{id...}`

Возвращает временный URL для доступа к файлу.

- **Путь:** Идентификатор файла, как и в предыдущем эндпоинте.
- **Ответ:** JSON с полем `url` – временным URL (действителен 1 час).

