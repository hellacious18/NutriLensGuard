import traceback
import uuid
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from google.genai import types
from google.adk.runners import Runner
from google.adk.sessions import InMemorySessionService
from nutrilens_swarm.agent import app as adk_app
from database import init_db, get_cached_analysis, save_analysis, get_connection

# Initialize SQLite Database Table & Indexes
init_db()

app = FastAPI(
    title="NutriLens Guard API",
    description="Backend API powered by Google Agent Development Kit (ADK) Multi-Agent Swarm with SQLite Response Cache",
    version="2.1.0"
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
    return {
        "status": "online",
        "system": "NutriLens Guard ADK Multi-Agent Engine",
        "cache": "SQLite Active"
    }


@app.get("/api/v1/cache/stats")
def get_cache_stats():
    """Returns total cached products and top queried items"""
    with get_connection() as conn:
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) as total_cached, SUM(hit_count) as total_hits FROM product_scans")
        summary = cursor.fetchone()
        
        cursor.execute("SELECT product_name, brand, hit_count, updated_at FROM product_scans ORDER BY hit_count DESC LIMIT 10")
        top_products = [dict(row) for row in cursor.fetchall()]
        
        return {
            "total_cached_products": summary["total_cached"] or 0,
            "total_cache_hits": summary["total_hits"] or 0,
            "popular_products": top_products
        }


@app.post("/api/v1/scan")
async def analyze_product(request: ScanRequest):
    health_profile = {
        "diabetic": request.diabetic,
        "hypertension": request.hypertension,
        "peanut_allergy": request.peanut_allergy,
        "dairy_allergy": request.dairy_allergy,
        "gluten_intolerance": request.gluten_intolerance
    }

    # 1. Check SQLite Database Cache First (Fast Path: < 20ms)
    cached_result = get_cached_analysis(request.product_name, health_profile)
    if cached_result:
        print(f"⚡ [CACHE HIT] Found '{request.product_name}' in SQLite (Total hits: {cached_result['hit_count']})")
        return cached_result

    print(f"🔍 [CACHE MISS] '{request.product_name}' not in SQLite. Invoking ADK Swarm...")

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

        response_data = {
            "query": request.product_name,
            "analysis": analysis_markdown.strip(),
            "product_name": product_name,
            "brand": brand,
            "health_profile": health_profile,
            "deception_report": deception,
            "health_risks": health_risks,
            "swap_recommendations": swaps,
            "cached": False,
            "hit_count": 1
        }

        # 2. Persist result in SQLite Database for future queries
        save_analysis(request.product_name, health_profile, response_data)
        print(f"💾 [SAVED TO DB] Cached '{product_name}' into SQLite database.")

        return response_data

    except Exception as e:
        print("\n❌ BACKEND EXCEPTION ENCOUNTERED:")
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)