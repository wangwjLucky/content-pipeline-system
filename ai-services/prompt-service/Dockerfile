FROM python:3.12-slim

WORKDIR /app

COPY prompt-service/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

COPY common/ ./common/
COPY prompt-service/ ./prompt_service/

EXPOSE 8003

CMD ["uvicorn", "prompt_service.main:app", "--host", "0.0.0.0", "--port", "8003"]