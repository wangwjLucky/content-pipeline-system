FROM python:3.12-slim

WORKDIR /app

COPY script-service/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

COPY common/ ./common/
COPY script-service/ ./script_service/

EXPOSE 8002

CMD ["uvicorn", "script_service.main:app", "--host", "0.0.0.0", "--port", "8002"]