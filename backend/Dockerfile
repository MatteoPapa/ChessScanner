# Base image
FROM python:3.10-slim

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

RUN apt-get update && apt-get install -y \
    libglib2.0-0 libsm6 libxext6 libxrender-dev \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first
COPY requirements.txt .

RUN pip install --upgrade pip && pip install -r requirements.txt

# Copy API code
COPY api/ /app/api/

WORKDIR /app/api

EXPOSE 8000

CMD ["python", "app.py"]
