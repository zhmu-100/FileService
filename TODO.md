Requirements are based on GRPC:
```proto
syntax = "proto3";

package file.v1;

service FileService {
  rpc Upload(stream UploadRequest) returns(UploadResponse);

  // FixUpload: replace existing file with a new one
  rpc FixUpload(stream FixUploadRequest) returns (UploadResponse);

  rpc GetFile(GetFileRequest) returns (stream GetFileResponse);
  rpc GetFileUrl(GetFileUrlRequest) returns (GetFileUrlResponse);
}

message FileMetadata {
  optional string user_id = 1;
  bool private = 2;
  string mime_type = 3;
  string file_name = 4;
  int64 size = 5;
  optional bool temporary = 6;
  optional string ecosystem_id = 7;
  optional string folder = 8;
}

message FixFileMetadata {
  string file_id = 1;
  string mime_type = 2;
  string file_name = 3;
  int64 size = 4;
}

message FixUploadRequest {
  oneof data {
    FixFileMetadata meta = 1;
    bytes chunk = 2;
  }
}

message UploadRequest {
  oneof data {
    FileMetadata meta = 1;
    bytes chunk = 2;
  }
}

message UploadResponse {
  string id = 1;
}

message GetFileRequest {
  string id = 1;
}

message GetFileResponse {
  oneof data {
    FileMetadata meta = 1;
    bytes chunk = 2;
  }
}

message GetFileUrlRequest {
  string id = 1;
}

message GetFileUrlResponse {
  string url = 1;
}
```