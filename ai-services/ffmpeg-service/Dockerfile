FROM python:3.12-slim

# 安装 FFmpeg + Pillow 系统依赖
RUN apt-get update && apt-get install -y ffmpeg fonts-noto-cjk && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY ffmpeg-service/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

COPY common/ ./common/
COPY ffmpeg-service/ ./ffmpeg_service/

EXPOSE 8006

CMD ["uvicorn", "ffmpeg_service.main:app", "--host", "0.0.0.0", "--port", "8006"]