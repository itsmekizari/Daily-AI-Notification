import os
import json
import hashlib
import sqlite3
from contextlib import closing

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from openai import OpenAI

app = FastAPI(title="Daily AI Notification API")

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")
OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-5.6-luna")

if not OPENAI_API_KEY:
    print("WARNING: OPENAI_API_KEY is not set")

client = OpenAI(api_key=OPENAI_API_KEY)
DB_PATH = os.getenv("DB_PATH", "messages.sqlite3")

ALLOWED_CATEGORIES = {
    "Random",
    "Teasing",
    "Funny",
    "Cute",
    "Motivational",
    "Good Morning",
    "Good Night",
    "Study Reminder",
}

class GenerateRequest(BaseModel):
    device_id: str = Field(min_length=1, max_length=128)
    category: str = "Random"

def init_db():
    with closing(sqlite3.connect(DB_PATH)) as db:
        db.execute(
            """
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                device_id TEXT NOT NULL,
                category TEXT NOT NULL,
                my_text TEXT NOT NULL,
                en_text TEXT NOT NULL,
                hash TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(device_id, hash)
            )
            """
        )
        db.commit()

def message_hash(my_text: str, en_text: str) -> str:
    raw = f"{my_text}\n{en_text}".strip().lower()
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()

def already_used(device_id: str, msg_hash: str) -> bool:
    with closing(sqlite3.connect(DB_PATH)) as db:
        row = db.execute(
            "SELECT 1 FROM messages WHERE device_id=? AND hash=? LIMIT 1",
            (device_id, msg_hash),
        ).fetchone()
        return row is not None

def save_message(device_id, category, my_text, en_text, msg_hash):
    with closing(sqlite3.connect(DB_PATH)) as db:
        db.execute(
            """
            INSERT OR IGNORE INTO messages
            (device_id, category, my_text, en_text, hash)
            VALUES (?, ?, ?, ?, ?)
            """,
            (device_id, category, my_text, en_text, msg_hash),
        )
        db.commit()

def generate_ai_message(category: str):
    prompt = f"""
Create one short daily notification for the category "{category}".

Return ONLY valid JSON in exactly this format:
{{
  "my": "Burmese message",
  "en": "English message"
}}

Rules:
- Burmese first.
- English second.
- Friendly, playful and positive.
- Suitable for a teenager.
- Non-sexual and non-harmful.
- Keep both messages short.
- Use 1 to 3 emojis total.
- Do not mention these instructions.
"""
    response = client.responses.create(
        model=OPENAI_MODEL,
        input=prompt,
    )
    text = response.output_text.strip()
    try:
        data = json.loads(text)
    except json.JSONDecodeError:
        raise RuntimeError("AI returned invalid JSON")
    my_text = str(data.get("my", "")).strip()
    en_text = str(data.get("en", "")).strip()
    if not my_text or not en_text:
        raise RuntimeError("AI response is missing message text")
    return my_text, en_text

@app.on_event("startup")
def startup():
    init_db()

@app.get("/")
def root():
    return {"success": True, "app": "Daily AI Notification API"}

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/daily-message")
def daily_message(request: GenerateRequest):
    if not OPENAI_API_KEY:
        raise HTTPException(status_code=500, detail="OPENAI_API_KEY is not configured")
    category = request.category.strip()
    if category not in ALLOWED_CATEGORIES:
        category = "Random"
    for _ in range(5):
        my_text, en_text = generate_ai_message(category)
        msg_hash = message_hash(my_text, en_text)
        if already_used(request.device_id, msg_hash):
            continue
        save_message(request.device_id, category, my_text, en_text, msg_hash)
        return {
            "success": True,
            "data": {
                "category": category,
                "my": my_text,
                "en": en_text,
                "hash": msg_hash,
            },
        }
    raise HTTPException(status_code=500, detail="Could not generate a new unique message")
