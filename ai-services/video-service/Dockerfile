FROM python:3.12-slim

WORKDIR /app

COPY video-service/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

COPY common/ ./common/
COPY video-service/ ./video_service/

EXPOSE 8004

CMD ["uvicorn", "video_service.main:app", "--host", "0.0.0.0", "--port", "8004"]