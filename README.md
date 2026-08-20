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
- **Deployment:** Google Cloud Run / Cloud Shell with tunneling (Pinggy/Localtunnel).
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

## Setup Instructions

### 1. Backend Setup (FastAPI & BigQuery)

1. Open **Google Cloud Shell** and clone your backend repository.
2. Ensure you have your Gemini API key set:
   ```bash
   export GEMINI_API_KEY="your_api_key_here"
   ```
3. Install dependencies:
   ```bash
   pip install fastapi uvicorn google-cloud-bigquery google-genai
   ```
4. Start the FastAPI server:
   ```bash
   python3 main.py
   ```
5. **Tunneling (Pinggy):** Expose your backend to the public internet so your Android emulator/device can reach it:
   ```bash
   ssh -R 80:localhost:8080 a.pinggy.io
   ```
   *Copy the generated HTTPS URL and update your Android project.*

### 2. Android App Setup

1. Open the project in **Android Studio**.
2. Navigate to `ScanViewModel.kt` or your Retrofit configuration.
3. Update the `baseUrl` with your active Pinggy or Localtunnel URL:
   ```kotlin
   private val api = Retrofit.Builder()
       .baseUrl("https://your-pinggy-url.a.pinggy.online/")
       .addConverterFactory(GsonConverterFactory.create())
       .build()
       .create(NutriLensApi::class.java)
   ```
4. Sync Gradle to ensure all dependencies (Retrofit, CameraX, Compose Markdown) are downloaded.
5. Build and run the app on an Android Emulator or physical device.

---

## Patchamomma 2026 Build Phase Alignment

This project was conceived and developed as part of Google Cloud's Patchamomma 2026 Hackathon. It perfectly aligns with the requirements by providing an industry-grade, data-driven application utilizing the Google Cloud Tech Stack (BigQuery, Gemini AI, Cloud Run) to solve real-world problems.

## Other Technologies Used
- **Python 3.12:** Primary backend language.
- **Uvicorn:** ASGI web server implementation for Python used to run FastAPI.
- **OkHttp & Gson:** Used in the Android client for network interceptors, timeouts, and JSON serialization.
- **Pinggy:** Secure SSH reverse proxy used for exposing the local Cloud Shell development environment to the public internet.

## Checklist and Project Timeline

| Status | Phase | Description | Planned Completion Date |
| :---: | :--- | :--- | :--- |
| ✅ | **Phase 1: Backend & AI Engine** | Initialize BigQuery dataset, configure Gemini API, and build FastAPI Python server. | Aug 19, 2026 |
| 🎯 | **Phase 2: Mobile App Foundation** | Build Android UDF Architecture with Jetpack Compose, Retrofit Networking, and Markdown rendering. | Aug 20, 2026 (Checkpoint 1) |
| ⏳ | **Phase 3: CameraX & ML Kit** | Integrate real-time on-device scanning of physical ingredient labels using CameraX and ML Kit OCR. | Aug 28, 2026 (Checkpoint 2) |
| ⏳ | **Phase 4: Cloud Run Deployment** | Migrate the FastAPI backend from Cloud Shell tunneling to a scalable, production-ready Google Cloud Run environment. | Sep 2, 2026 |
| ⏳ | **Phase 5: Polish & Final Testing** | Optimize swarm latency, polish Compose UI animations, error handling, and complete end-to-end testing. | Sep 5, 2026 (Final Checkpoint) |
| ⏳ | **Submission Lock** | Finalize documentation, record demo video, and submit project. | Sep 7, 2026 |
