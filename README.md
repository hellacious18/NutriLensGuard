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

## Future Roadmap
- **Live Camera Integration:** Finalizing CameraX and ML Kit integration for live barcode and text scanning.
- **Latency Optimization:** Minimizing swarm execution time.
- **Cloud Run Deployment:** Moving from Cloud Shell tunnels to a permanent, scalable Google Cloud Run deployment.
