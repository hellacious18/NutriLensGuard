import os
import logging
from dotenv import load_dotenv
from typing import Dict, Any

from google.cloud import bigquery
from google.genai import types
from google.adk import Agent
from google.adk.agents import SequentialAgent
from google.adk.models import Gemini
from google.adk.tools.tool_context import ToolContext
from google.adk.apps.app import App

load_dotenv()
logging.basicConfig(level=logging.INFO)

RETRY_OPTIONS = types.HttpRetryOptions(
    initial_delay=3,
    max_delay=30,
    attempts=5
)
MODEL_NAME = os.getenv("MODEL", "gemini-3.5-flash-lite")

# Built-in packaged food knowledge fallback
COMMON_FOOD_FALLBACK = {
    "hide & seek": {
        "product_name": "Parle Hide & Seek Chocolate Chip Cookies",
        "brand": "Parle",
        "ingredients": "Refined Wheat Flour (Maida 55%), Chocolate Chips (20%) [Sugar, Hydrogenated Vegetable Fat, Cocoa Solids, Emulsifier (Soy Lecithin 322)], Sugar, Edible Vegetable Oil (Palm Oil), Invert Sugar Syrup, Leavening Agents (503(ii), 500(ii)), Cocoa Solids, Iodised Salt, Artificial Flavoring Substances (Vanilla, Chocolate)."
    },
    "maggi": {
        "product_name": "Maggi 2-Minute Instant Noodles",
        "brand": "Nestlé",
        "ingredients": "Refined Wheat Flour (Maida), Palm Oil, Iodised Salt, Wheat Gluten, Thickeners (508, 412), Acidity Regulators (501(i), 500(i)), Humectant (451(i)). Tastemaker: Mixed Spices (25.6%), Sugar, Iodised Salt, Flavor Enhancer (635), Palm Oil."
    },
    "lays": {
        "product_name": "Lay's Classic Salted Potato Chips",
        "brand": "PepsiCo",
        "ingredients": "Potato (89%), Edible Vegetable Oil (Palmolein), Iodised Salt (1%)."
    }
}

_bq_client = None

def get_bq_client() -> bigquery.Client:
    global _bq_client
    if _bq_client is None:
        project_id = os.getenv("GOOGLE_CLOUD_PROJECT") or os.getenv("GCP_PROJECT")
        _bq_client = bigquery.Client(project=project_id)
    return _bq_client


def log_query_to_model(callback_context, **kwargs):
    logging.info(f"[ADK Call] Querying model for agent: {callback_context.agent_name}")

def log_model_response(callback_context, **kwargs):
    logging.info(f"[ADK Call] Model responded for agent: {callback_context.agent_name}")


def fetch_product_from_bq_tool(
    tool_context: ToolContext,
    product_query: str
) -> Dict[str, str]:
    """Queries BigQuery for product details and stores product_name, brand, and ingredients in session state."""
    
    # Set safe defaults
    product_name = product_query
    brand = "Unknown Brand"
    ingredients = f"Packaged food ingredients for {product_query}"

    # 1. Attempt BigQuery lookup
    try:
        bq_client = get_bq_client()
        query = """
            SELECT *
            FROM `nutrilens_db.packaged_foods`
            WHERE LOWER(CAST(`Item name` AS STRING)) LIKE CONCAT('%', LOWER(@query), '%')
               OR LOWER(CAST(`Brand_Name` AS STRING)) LIKE CONCAT('%', LOWER(@query), '%')
            LIMIT 1
        """
        job_config = bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter("query", "STRING", product_query)
            ]
        )
        query_job = bq_client.query(query, job_config=job_config)
        results = list(query_job.result())

        if results:
            row = dict(results[0].items())
            row_lower = {k.lower(): v for k, v in row.items()}
            product_name = row_lower.get("item name") or row_lower.get("item_name") or product_name
            brand = row_lower.get("brand_name") or row_lower.get("brand") or brand
            ingredients = row_lower.get("ingredients") or ingredients
    except Exception as e:
        logging.warning(f"BigQuery fetch bypassed/offline ({e}). Using catalog knowledge.")
        
        # 2. Check built-in fallback catalog
        q = product_query.lower()
        for k, item in COMMON_FOOD_FALLBACK.items():
            if k in q:
                product_name = item["product_name"]
                brand = item["brand"]
                ingredients = item["ingredients"]
                break

    tool_context.state["product_name"] = product_name
    tool_context.state["brand"] = brand
    tool_context.state["ingredients"] = ingredients
    tool_context.state["deception_report"] = "No deception analysis available."
    tool_context.state["health_risks"] = "No health risks identified."
    tool_context.state["swap_recommendations"] = "No swap recommendations available."

    return {
        "status": "success",
        "product_name": product_name,
        "brand": brand,
        "message": f"Loaded product info for '{product_name}'"
    }


def save_to_state(
    tool_context: ToolContext,
    field: str,
    response: str
) -> Dict[str, str]:
    """Saves output text into a specified state key for context sharing."""
    tool_context.state[field] = response
    logging.info(f"[State Saved -> {field}]: {response[:80]}...")
    return {"status": "success"}


# Agent 1: Deception Inspector
deception_inspector = Agent(
    name="deception_inspector",
    model=Gemini(model=MODEL_NAME, retry_options=RETRY_OPTIONS),
    description="Inspects ingredient lists for hidden sugars, palm oil, and deceptive naming.",
    instruction="""
    INSTRUCTIONS:
    - Analyze the following ingredients for hidden sugars (e.g. invert syrup, maltodextrin, high-fructose corn syrup), palm oil/hydrogenated fat, and deceptive claims:
      { ingredients? }
    - Write a concise, bulleted deception report.
    - Use the 'save_to_state' tool to save your findings to the field 'deception_report'.
    """,
    generate_content_config=types.GenerateContentConfig(temperature=0),
    before_model_callback=log_query_to_model,
    after_model_callback=log_model_response,
    tools=[save_to_state],
)

# Agent 2: Health Profile Matcher
health_matcher = Agent(
    name="health_matcher",
    model=Gemini(model=MODEL_NAME, retry_options=RETRY_OPTIONS),
    description="Evaluates health risks based on ingredients, deception report, and user health profile.",
    instruction="""
    INSTRUCTIONS:
    - Evaluate health risks based on:
        - Health Profile: { health_profile? }
        - Ingredients: { ingredients? }
        - Deception Analysis: { deception_report? }
    - Highlight specific risks for conditions enabled in the health profile (Diabetic, Hypertension, Allergies, Gluten).
    - Use the 'save_to_state' tool to save your risk report to the field 'health_risks'.
    """,
    generate_content_config=types.GenerateContentConfig(temperature=0),
    before_model_callback=log_query_to_model,
    after_model_callback=log_model_response,
    tools=[save_to_state],
)

# Agent 3: Clean Swap Recommender
clean_swap_recommender = Agent(
    name="clean_swap_recommender",
    model=Gemini(model=MODEL_NAME, retry_options=RETRY_OPTIONS),
    description="Suggests clean, healthy food alternatives based on identified health risks.",
    instruction="""
    INSTRUCTIONS:
    - Suggest 2-3 cleaner, healthier food alternatives for { product_name? } based on these health risks:
      { health_risks? }
    - Use the 'save_to_state' tool to save your clean alternative suggestions to the field 'swap_recommendations'.
    """,
    generate_content_config=types.GenerateContentConfig(temperature=0),
    before_model_callback=log_query_to_model,
    after_model_callback=log_model_response,
    tools=[save_to_state],
)

# Sequential Workflow
nutrilens_pipeline = SequentialAgent(
    name="nutrilens_pipeline",
    description="Runs deception analysis, health risk matching, and clean recommendations sequentially.",
    sub_agents=[
        deception_inspector,
        health_matcher,
        clean_swap_recommender
    ],
)

# Root Agent
root_agent = Agent(
    name="nutrilens_root",
    model=Gemini(model=MODEL_NAME, retry_options=RETRY_OPTIONS),
    description="Root agent guiding the user through NutriLens Guard scan analysis.",
    instruction="""
    INSTRUCTIONS:
    - Welcome the user to NutriLens Guard.
    - Use the 'fetch_product_from_bq_tool' tool to load ingredient details for the requested product.
    - Transfer execution to the 'nutrilens_pipeline' workflow agent.
    """,
    generate_content_config=types.GenerateContentConfig(temperature=0),
    before_model_callback=log_query_to_model,
    after_model_callback=log_model_response,
    tools=[fetch_product_from_bq_tool],
    sub_agents=[nutrilens_pipeline],
)

# ADK App Export
app = App(
    name="nutrilens_swarm",
    root_agent=root_agent
)
