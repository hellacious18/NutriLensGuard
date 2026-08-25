import traceback
import uuid
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from google.genai import types
from google.adk.runners import Runner
from google.adk.sessions import InMemorySessionService
from nutrilens_swarm.agent import app as adk_app

app = FastAPI(
    title="NutriLens Guard API",
    description="Backend API powered by Google Agent Development Kit (ADK) Multi-Agent Swarm",
    version="2.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize Session Service and ADK Runner
session_service = InMemorySessionService()
runner = Runner(app=adk_app, session_service=session_service)


class ScanRequest(BaseModel):
    product_name: str
    diabetic: bool = True
    hypertension: bool = True
    peanut_allergy: bool = False
    dairy_allergy: bool = False
    gluten_intolerance: bool = False


@app.get("/")
def read_root():
    return {"status": "online", "system": "NutriLens Guard ADK Multi-Agent Engine"}


@app.post("/api/v1/scan")
async def analyze_product(request: ScanRequest):
    health_profile = {
        "diabetic": request.diabetic,
        "hypertension": request.hypertension,
        "peanut_allergy": request.peanut_allergy,
        "dairy_allergy": request.dairy_allergy,
        "gluten_intolerance": request.gluten_intolerance
    }

    user_id = f"user_{uuid.uuid4().hex[:8]}"

    try:
        # Create ADK Session with initial health profile state
        session = await session_service.create_session(
            app_name="nutrilens_swarm",
            user_id=user_id,
            state={"health_profile": health_profile}
        )

        # Build message Content for ADK Runner
        user_msg = types.Content(
            role="user",
            parts=[types.Part.from_text(text=f"Analyze product: {request.product_name}")]
        )

        # Run multi-agent pipeline
        async for _ in runner.run_async(
            user_id=user_id,
            session_id=session.id,
            new_message=user_msg
        ):
            pass

        # Fetch updated session state populated by agents
        updated_session = await session_service.get_session(
            app_name="nutrilens_swarm",
            user_id=user_id,
            session_id=session.id
        )

        state = updated_session.state if updated_session else {}
        product_name = state.get("product_name", request.product_name)
        brand = state.get("brand", "Unknown Brand")
        ingredients = state.get("ingredients", "Not available")
        deception = state.get("deception_report", "No deceptive practices detected.")
        health_risks = state.get("health_risks", "No significant health risks identified.")
        swaps = state.get("swap_recommendations", "No swap recommendations available.")

        # Build clean Markdown analysis for Android Compose UI
        analysis_markdown = f"""### 🏷️ **{product_name}** ({brand})

**📋 Ingredients:**
{ingredients}

---

### 🔍 **Deception Analysis**
{deception}

---

### ⚠️ **Health Risks & Personal Profile Match**
{health_risks}

---

### 🥗 **Clean Swap Recommendations**
{swaps}
"""

        # Return both the combined `analysis` and granular fields
        return {
            "query": request.product_name,
            "analysis": analysis_markdown.strip(),
            "product_name": product_name,
            "brand": brand,
            "health_profile": health_profile,
            "deception_report": deception,
            "health_risks": health_risks,
            "swap_recommendations": swaps
        }

    except Exception as e:
        print("\n❌ BACKEND EXCEPTION ENCOUNTERED:")
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)