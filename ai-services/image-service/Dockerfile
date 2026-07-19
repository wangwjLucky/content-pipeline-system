FROM python:3.12-slim

WORKDIR /app

COPY image-service/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

COPY common/ ./common/
COPY image-service/ ./image_service/

EXPOSE 8007

CMD ["uvicorn", "image_service.main:app", "--host", "0.0.0.0", "--port", "8007"]