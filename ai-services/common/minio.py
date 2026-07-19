"""MinIO 文件存储封装。"""

import io
from common.config import settings
import boto3
from botocore.config import Config


class MinIOClient:
    """MinIO 客户端封装（S3 兼容接口）"""

    def __init__(self):
        self.client = boto3.client(
            "s3",
            endpoint_url=settings.minio_endpoint,
            aws_access_key_id=settings.minio_access_key,
            aws_secret_access_key=settings.minio_secret_key,
            config=Config(signature_version="s3v4"),
        )

    def upload_bytes(self, bucket: str, key: str, data: bytes, content_type: str = "application/octet-stream") -> str:
        """上传字节数据，返回 URL"""
        self.client.put_object(
            Bucket=bucket,
            Key=key,
            Body=data,
            ContentType=content_type,
        )
        return f"{settings.minio_endpoint}/{bucket}/{key}"

    def upload_file(self, bucket: str, key: str, file_path: str) -> str:
        """上传文件，返回 URL"""
        self.client.upload_file(file_path, bucket, key)
        return f"{settings.minio_endpoint}/{bucket}/{key}"

    def download(self, bucket: str, key: str) -> bytes:
        """下载文件"""
        response = self.client.get_object(Bucket=bucket, Key=key)
        return response["Body"].read()

    def list_files(self, bucket: str, prefix: str = "") -> list[str]:
        """列出文件"""
        response = self.client.list_objects_v2(Bucket=bucket, Prefix=prefix)
        return [obj["Key"] for obj in response.get("Contents", [])]

    def delete_file(self, bucket: str, key: str):
        """删除文件"""
        self.client.delete_object(Bucket=bucket, Key=key)