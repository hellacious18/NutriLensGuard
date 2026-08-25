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

# Setup Logging
logging.basicConfig(level=logging.INFO)

# Update RETRY_OPTIONS in nutrilens_swarm/agent.py
RETRY_OPTIONS = types.HttpRetryOptions(
    initial_delay=2,
    max_delay=10,
    attempts=5
)
MODEL_NAME = os.getenv("MODEL", "gemini-3.7-flash")

# Initialize BigQuery Client Cache
_bq_client = None

def get_bq_client() -> bigquery.Client:
    """Lazily initializes and returns the BigQuery client with the active project ID."""
    global _bq_client
    if _bq_client is None:
        project_id = os.getenv("GOOGLE_CLOUD_PROJECT") or os.getenv("GCP_PROJECT")
        _bq_client = bigquery.Client(project=project_id)
    return _bq_client


# --- Inline Callbacks ---

def log_query_to_model(callback_context, **kwargs):
    logging.info(f"[ADK Call] Querying model for agent: {callback_context.agent_name}")

def log_model_response(callback_context, **kwargs):
    logging.info(f"[ADK Call] Model responded for agent: {callback_context.agent_name}")


# --- ADK Tools ---

def fetch_product_from_bq_tool(
    tool_context: ToolContext,
    product_query: str
) -> Dict[str, str]:
    """Queries BigQuery for product details and stores product_name, brand, and ingredients in session state."""
    
    bq_client = get_bq_client()

    # Ensure default state keys are non-empty strings before passing to agents
tool_context.state["ingredients"] = ingredients or "No ingredients listed."
tool_context.state["deception_report"] = "No deception analysis available."
tool_context.state["health_risks"] = "No health risks identified."
    
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

        product_name = (
            row_lower.get("item name")
            or row_lower.get("item_name")
            or row_lower.get("product_name")
            or "Unknown Product"
        )
        brand = (
            row_lower.get("brand_name")
            or row_lower.get("brand")
            or "Unknown Brand"
        )
        ingredients = row_lower.get("ingredients") or "No ingredients listed"

        tool_context.state["product_name"] = product_name
        tool_context.state["brand"] = brand
        tool_context.state["ingredients"] = ingredients
        return {"status": "success", "message": f"Product '{product_name}' loaded into state."}

    tool_context.state["product_name"] = product_query
    tool_context.state["brand"] = "Unknown"
    tool_context.state["ingredients"] = "Product not found in database."
    return {"status": "not_found", "message": f"Product matching '{product_query}' not found."}

def save_to_state(
    tool_context: ToolContext,
    field: str,
    response: str
) -> Dict[str, str]:
    """Saves output text into a specified state key for context sharing."""
    tool_context.state[field] = response
    logging.info(f"[State Saved -> {field}]: {response}")
    return {"status": "success"}


# --- ADK Agents ---

# Agent 1: Deception Inspector
deception_inspector = Agent(
    name="deception_inspector",
    model=Gemini(model=MODEL_NAME, retry_options=RETRY_OPTIONS),
    description="Inspects ingredient lists for hidden sugars, palm oil, and deceptive naming.",
    instruction="""
    INSTRUCTIONS:
    - Analyze the following ingredients for hidden sugars (e.g. invert syrup, dextrose), palm oil, and deceptive naming:
      { ingredients? }
    - Use the 'save_to_state' tool to save your detailed findings to the field 'deception_report'.
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
    - Highlight risks related to glycemic spikes, sodium, or inflammation.
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
    - Suggest cleaner, healthier food alternatives for { product_name? } based on these health risks:
      { health_risks? }
    - Use the 'save_to_state' tool to save your clean alternative suggestions to the field 'swap_recommendations'.
    """,
    generate_content_config=types.GenerateContentConfig(temperature=0),
    before_model_callback=log_query_to_model,
    after_model_callback=log_model_response,
    tools=[save_to_state],
)

# Sequential Workflow Agent
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
    - Use the 'fetch_product_from_bq_tool' tool to load ingredient details for the user's scan request into state.
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