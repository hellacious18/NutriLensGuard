# NutriLens Guard: Autonomous Multi-Agent Food Safety Auditor

NutriLens Guard is a real-time, AI-powered food safety mobile application designed to help users make informed and healthy dietary choices. It utilizes a Multi-Agent Swarm architecture to audit packaged foods, detect hidden sugars and additives, evaluate risks based on personal health constraints, and suggest cleaner alternatives.

## Project Architecture

This project is built using a modern, scalable tech stack split into a mobile client and a cloud-based AI backend.

### 1. Mobile Client (Android)
- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose
- **Architecture:** Unidirectional Data Flow (UDF) with MVVM (`ScanViewModel`, `ScanScreen`, `ScanContract`).
- **Networking:** Retrofit with OkHttp for API communication.
- **On-Device ML:** Google ML Kit Text Recognition (OCR) and CameraX for scanning physical ingredient labels.
- **Markdown Rendering:** `compose-markdown` library for rendering rich AI analysis results natively.

### 2. Backend API (Google Cloud)
- **Framework:** FastAPI (Python)
- **Deployment:** Google Cloud Run / Cloud Shell with tunneling (Localtunnel).
- **Functionality:** Serves the `/api/v1/scan` endpoint which receives user constraints and product text, passing them to the AI Swarm.

### 3. AI & Data Engine
- **LLM Engine:** Google AI Studio (Gemini API) using the `gemini-3.6-flash` model.
- **Multi-Agent Swarm:** 
  - **Deception Detection Agent:** Identifies hidden sugars, palm oils, and harmful additives.
  - **Health Profile Matcher Agent:** Evaluates specific risks for the user's constraints (e.g., Diabetic, Hypertension).
  - **Clean Swap Agent:** Recommends healthier, natural alternatives.
- **Data Source:** Google Cloud BigQuery (`nutrilens_db.packaged_foods`), populated from the Indian Packaged Foods Nutritional Dataset 2026 (Kaggle).

## Google Cloud Services Used
* **Google Cloud BigQuery:** Serves as the primary data warehouse storing our product catalog and nutritional data.
* **Google AI Studio (Gemini API):** Powers the core Multi-Agent Swarm with the `gemini-3.6-flash` model.
* **Google Cloud Shell:** Used as the primary development environment and currently hosts our FastAPI backend via tunneling.
* **Google Cloud Run:** The planned serverless deployment platform for our production FastAPI backend.
* **Google ML Kit:** Utilized within the Android client for on-device Text Recognition (OCR) of ingredient labels.

---

## Setup & Execution Guide

### 1. Environment & Backend Setup

1. **Navigate to the backend directory and activate the virtual environment:**
   ```bash
   cd nutrilens-backend
   python3 -m venv venv
   source venv/bin/activate
   pip install -r requirements.txt
   ```

2. **Configure your Gemini API Key in `.env`:**
   ```bash
   # In nutrilens-backend/.env
   GEMINI_API_KEY="your_api_key_here"
   MODEL="gemini-3.5-flash-lite"
   ```

---

### 2. Running the Servers

You can run the ADK Web UI for interactive debugging, the FastAPI server for mobile app requests, or both simultaneously:

#### A. Run Google ADK Web UI (Visual Multi-Agent Dashboard)
Provides an interactive visual agent graph, live trace inspector, and chat playground:
```bash
cd nutrilens-backend
source venv/bin/activate
adk web . --port 8000
```
👉 Access the Web UI in your browser at: **[http://127.0.0.1:8000](http://127.0.0.1:8000)**

#### B. Run the FastAPI Backend (For Mobile App API)
Serves the `/api/v1/scan` multi-agent endpoint consumed by the Android client:
```bash
cd nutrilens-backend
source venv/bin/activate
python main.py
```
👉 Server listens on: **`http://localhost:8080`**

---

### 3. Connecting the Android Mobile App

Update `local.properties` in the project root according to your testing setup:

* **Physical Android Device via USB (Recommended):**
  1. Forward device ports over USB:
     ```bash
     adb reverse tcp:8080 tcp:8080
     ```
  2. Set `local.properties`:
     ```properties
     BASE_URL="http://localhost:8080/"
     ```

* **Android Studio Virtual Emulator (AVD):**
  Set `local.properties`:
  ```properties
  BASE_URL="http://10.0.2.2:8080/"
  ```

* **Public Tunneling (Localtunnel / Cloud Shell):**
  1. Expose port 8080:
     ```bash
     npx localtunnel --port 8080
     ```
  2. Set `local.properties`:
     ```properties
     BASE_URL="https://your-localtunnel-url.loca.lt/"
     ```

Then build and run the app from Android Studio or terminal:
```bash
./gradlew installDebug
```

---

### 4. How to Stop / Kill the Servers

If a port is already in use (`[Errno 48] Address already in use`) or you need to terminate running background processes:

* **Kill ADK Web Server (Port 8000):**
  ```bash
  lsof -ti :8000 | xargs kill -9
  ```

* **Kill FastAPI Backend Server (Port 8080):**
  ```bash
  lsof -ti :8080 | xargs kill -9
  ```

* **Kill Both Servers at Once:**
  ```bash
  lsof -ti :8000,8080 | xargs kill -9
  ```

* **Clear ADB Port Forwards:**
  ```bash
  adb reverse --remove-all
  ```

---

## Patchamomma 2026 Build Phase Alignment

This project was conceived and developed as part of Google Cloud's Patchamomma 2026 Hackathon. It perfectly aligns with the requirements by providing an industry-grade, data-driven application utilizing the Google Cloud Tech Stack (BigQuery, Gemini AI, Cloud Run) to solve real-world problems.

## Other Technologies Used
- **Python 3.12:** Primary backend language.
- **Uvicorn:** ASGI web server implementation for Python used to run FastAPI.
- **OkHttp & Gson:** Used in the Android client for network interceptors, timeouts, and JSON serialization.
- **Localtunnel:** Exposes the local Cloud Shell development environment to the public internet so the Android app can communicate with the backend.

## Feature Implementation Status

- [x] **Camera & OCR Vision Engine:** Real-time CameraX preview with custom viewfinder reticle, Google ML Kit OCR text recognition, live flash/torch toggle, camera lens switching (front/back), snapshot photo capture with preview thumbnail, and ingredient candidate parsing.
- [x] **Intelligent Multi-Agent Swarm:** Gemini 3.5/3.6 multi-agent swarm architecture (Deception Detector, Health Profile Matcher, Clean Swap Suggester).
- [x] **Hybrid Data Layer & Caching:** Google Cloud BigQuery dataset (`nutrilens_db.packaged_foods`), local SQLite fast-lookup cache (`product_cache.db`), and fallback e-commerce web scraper.
- [x] **Android MVVM + UDF Architecture:** Jetpack Compose UI, unidirectional data flow with `ScanContract`, Retrofit network client with custom interceptors, and rich markdown rendering.
- [x] **Health Constraints Engine:** Dynamic allergen & health condition toggles (Diabetic, Hypertension, Celiac, Vegan, Nut Allergies, etc.).
- [ ] **Final Latency Optimization & UI Micro-Animations:** Fine-tuning swarm response times, cartoon mascot visual states, and final production Cloud Run deployment.

## Checklist and Project Timeline

| Status | Phase / Milestone | Description | Completion Date / Target |
| :---: | :--- | :--- | :--- |
| ✅ | **Phase 1: Backend & AI Engine** | Initialize BigQuery dataset, configure Gemini API, and build FastAPI Python server. | Aug 19, 2026 |
| ✅ | **Phase 2: Mobile App Foundation** | Build Android UDF Architecture with Jetpack Compose, Retrofit Networking, and Markdown rendering. | Aug 20, 2026 (Touchpoint 1) |
| ✅ | **Phase 3: CameraX & ML Kit Vision** | Integrated live CameraX scanning, ML Kit OCR label extraction, torch toggle, camera switcher, and snapshot capture. | Aug 28, 2026 (Completed) |
| ✅ | **Phase 4: Data Layer & Hybrid Caching** | Built SQLite caching engine, BigQuery integration, and e-commerce fallback scraper. | Sep 2, 2026 (Completed) |
| ✅ | **Touchpoint 2: Application Readiness** | Deliver almost-ready app with full data layer, camera OCR scanner, and live multi-agent AI integrations. | **Sep 3, 2026 (Completed)** |
| ⏳ | **Phase 5: Polish & Latency Optimization** | Swarm latency tuning, UI transitions/cartoon graphics polish, resilience handling, and end-to-end testing. | Sep 4 – Sep 7, 2026 |
| ⏳ | **Touchpoint 3: Final Submission** | Fully completed, deployed product release, polished documentation, and demo video submission. | **Sep 9, 2026 (Final Deadline)** |
