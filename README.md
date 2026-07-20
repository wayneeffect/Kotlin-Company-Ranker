# 🚀 Kotlin Company Ranker (MVP)

A lightweight, high-performance, and hardened REST API built with Kotlin and Ktor for tracking, scoring, and ranking companies. Architected for rapid deployment to **Render** via Docker, featuring thread-safe in-memory persistence, rate-limiting, and structured global error handling.

---

## ✨ Features & Architecture

* **Framework:** Kotlin JVM + Ktor Server
* **Deployment Ready:** Multi-stage `Dockerfile` configured with a non-root execution context for secure deployment on Render.
* **Thread-Safe State:** Uses `ConcurrentHashMap` with atomic updates (`compute`) to prevent race conditions under concurrent writes.
* **Robust Error Handling:** Centralized exception mapping using Ktor `StatusPages`—no raw stack traces leaked to clients.
* **Security & Hardening:**
  * Strict Rate Limiting (differentiated read vs. write limits)
  * Dynamic CORS origin controls
  * Standard security headers (`nosniff`, `X-Frame-Options`, `XSS-Protection`)
* **Uptime Monitoring:** Built-in `/health` endpoint for edge proxy checkups.

---

## 📋 API Reference

### Health & Monitoring
* **`GET /health`**
  * Returns system status.
  * **Response:** `200 OK` → `{"status": "UP"}`

### Companies
* **`GET /api/companies`**
  * Fetch ranked companies, ordered by highest score descending.
  * **Query Params:** `category` (optional)
  * **Rate Limit:** 100 requests / minute

* **`POST /api/companies`**
  * Add a new company to the store.
  * **Rate Limit:** 10 requests / minute
  * **Request Body:**
    ```json
    {
      "name": "Acme Corp",
      "score": 4.5,
      "category": "Technology"
    }
    ```

* **`POST /api/companies/{id}/rate`**
  * Submit a new rating for a company. Calculates updated weighted scores automatically.
  * **Rate Limit:** 10 requests / minute
  * **Request Body:**
    ```json
    {
      "score": 5.0
    }
    ```

---

## 🛠️ Local Development

### Prerequisites
* JDK 21+
* Gradle 8.x (or use the included wrapper)
* Docker (optional, for containerized testing)

### Run Locally (Gradle)

```bash
# Build the project
./gradlew build

# Launch the server
./gradlew run
The server will start on http://localhost:8080.

Run via Docker
Bash
# Build the container image
docker build -t kotlin-company-ranker .

# Run the container locally
docker run -p 8080:8080 kotlin-company-ranker
☁️ Deploying to Render
This repository is pre-configured for zero-config Docker deployment on Render:

Create a new Web Service on Render.

Connect this GitHub repository.

Select Docker as the Runtime environment.

Set the following optional environment variables if needed:

PORT: Set dynamically by Render (defaults to 8080 locally).

ALLOWED_ORIGIN: Set your frontend domain (defaults to *).

Click Create Web Service.

🛡️ License
Distributed under the MIT License. See LICENSE for more information.
