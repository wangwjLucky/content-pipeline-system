FROM python:3.12-slim

WORKDIR /app

COPY voice-service/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

COPY common/ ./common/
COPY voice-service/ ./voice_service/

EXPOSE 8005

CMD ["uvicorn", "voice_service.main:app", "--host", "0.0.0.0", "--port", "8005"]