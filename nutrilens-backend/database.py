import hashlib
import json
import os
import sqlite3
from datetime import datetime
from typing import Optional, Dict, Any

DB_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "product_cache.db")


def get_connection() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    with get_connection() as conn:
        cursor = conn.cursor()
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS product_scans (
                cache_key TEXT PRIMARY KEY,
                query TEXT NOT NULL,
                product_name TEXT,
                brand TEXT,
                ingredients TEXT,
                deception_report TEXT,
                health_risks TEXT,
                swap_recommendations TEXT,
                analysis TEXT NOT NULL,
                health_profile_json TEXT,
                hit_count INTEGER DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_query ON product_scans(query)")
        conn.commit()


def generate_cache_key(query: str, health_profile: Dict[str, Any]) -> str:
    # Normalize query string (trim, lowercase, normalize spaces)
    normalized_query = " ".join(query.strip().lower().split())
    profile_str = json.dumps(health_profile, sort_keys=True)
    raw_key = f"{normalized_query}:{profile_str}"
    return hashlib.sha256(raw_key.encode("utf-8")).hexdigest()


def get_cached_analysis(query: str, health_profile: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    cache_key = generate_cache_key(query, health_profile)
    with get_connection() as conn:
        cursor = conn.cursor()
        cursor.execute(
            "SELECT * FROM product_scans WHERE cache_key = ?",
            (cache_key,)
        )
        row = cursor.fetchone()
        if row:
            # Increment hit counter
            cursor.execute(
                """
                UPDATE product_scans 
                SET hit_count = hit_count + 1, updated_at = CURRENT_TIMESTAMP 
                WHERE cache_key = ?
                """,
                (cache_key,)
            )
            conn.commit()

            return {
                "query": row["query"],
                "analysis": row["analysis"],
                "product_name": row["product_name"],
                "brand": row["brand"],
                "health_profile": json.loads(row["health_profile_json"]) if row["health_profile_json"] else health_profile,
                "deception_report": row["deception_report"],
                "health_risks": row["health_risks"],
                "swap_recommendations": row["swap_recommendations"],
                "cached": True,
                "hit_count": row["hit_count"] + 1
            }
    return None


def save_analysis(query: str, health_profile: Dict[str, Any], result: Dict[str, Any]):
    cache_key = generate_cache_key(query, health_profile)
    with get_connection() as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            INSERT INTO product_scans (
                cache_key,
                query,
                product_name,
                brand,
                ingredients,
                deception_report,
                health_risks,
                swap_recommendations,
                analysis,
                health_profile_json,
                hit_count,
                created_at,
                updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT(cache_key) DO UPDATE SET
                product_name = excluded.product_name,
                brand = excluded.brand,
                ingredients = excluded.ingredients,
                deception_report = excluded.deception_report,
                health_risks = excluded.health_risks,
                swap_recommendations = excluded.swap_recommendations,
                analysis = excluded.analysis,
                health_profile_json = excluded.health_profile_json,
                updated_at = CURRENT_TIMESTAMP
            """,
            (
                cache_key,
                query.strip(),
                result.get("product_name"),
                result.get("brand"),
                result.get("ingredients", ""),
                result.get("deception_report"),
                result.get("health_risks"),
                result.get("swap_recommendations"),
                result.get("analysis"),
                json.dumps(health_profile),
            )
        )
        conn.commit()
