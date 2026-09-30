import hashlib
import os
import sqlite3
from contextlib import closing

from fastapi import FastAPI, HTTPException
from openai import OpenAI
from pydantic import BaseModel, Field

app = FastAPI(title="Daily AI Notification API")

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "").strip()
OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-5.6-luna").strip()
DB_PATH = os.getenv("DB_PATH", "messages.sqlite3")

if not OPENAI_API_KEY:
    print("WARNING: OPENAI_API_KEY is not set")

client = OpenAI(api_key=OPENAI_API_KEY) if OPENAI_API_KEY else None

FIXED_STYLE = "Love Teasing"
ALLOWED_CATEGORIES = {FIXED_STYLE}

ALLOWED_LANGUAGES = {
    "Burmese + English", "Burmese only", "English only",
}


class GenerateRequest(BaseModel):
    device_id: str = Field(min_length=1, max_length=128)
    category: str = "Random"
    language: str = "Burmese + English"


class AIMessage(BaseModel):
    my: str
    en: str


def init_db() -> None:
    with closing(sqlite3.connect(DB_PATH)) as db:
        db.execute(
            """
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                device_id TEXT NOT NULL,
                category TEXT NOT NULL,
                language TEXT NOT NULL,
                my_text TEXT NOT NULL,
                en_text TEXT NOT NULL,
                hash TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(device_id, hash)
            )
            """
        )
        columns = [
            row[1]
            for row in db.execute("PRAGMA table_info(messages)").fetchall()
        ]
        if "language" not in columns:
            db.execute(
                "ALTER TABLE messages ADD COLUMN language TEXT NOT NULL DEFAULT 'Burmese + English'"
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


def save_message(
    device_id: str,
    category: str,
    language: str,
    my_text: str,
    en_text: str,
    msg_hash: str,
) -> None:
    with closing(sqlite3.connect(DB_PATH)) as db:
        db.execute(
            """
            INSERT OR IGNORE INTO messages
            (device_id, category, language, my_text, en_text, hash)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            (device_id, category, language, my_text, en_text, msg_hash),
        )
        db.commit()


def generate_ai_message(category: str, language: str) -> AIMessage:
    if client is None:
        raise RuntimeError("OPENAI_API_KEY is not configured")

    prompt = f"""
Create one short social-media-style daily notification with a fixed style: Love Teasing.

The message should playfully tease the reader about love, crushes, or relationship situations without sexual content, insults, or harassment.

Language mode: {language}

Requirements:
- Generate a natural Burmese message in `my`.
- Generate a natural English message in `en`.
- Keep both messages short, friendly, playful and positive.
- Suitable for a teenager.
- Non-sexual and non-harmful.
- Use 1 to 3 emojis total across both fields.
- Do not explain the task.
"""

    response = client.responses.parse(
        model=OPENAI_MODEL,
        input=prompt,
        text_format=AIMessage,
    )

    parsed = response.output_parsed
    if parsed is None:
        raise RuntimeError("OpenAI returned no structured message")

    my_text = parsed.my.strip()
    en_text = parsed.en.strip()
    if not my_text or not en_text:
        raise RuntimeError("OpenAI returned empty message fields")

    return AIMessage(my=my_text, en=en_text)


@app.on_event("startup")
def startup() -> None:
    init_db()


@app.get("/")
def root():
    return {
        "success": True,
        "app": "Daily AI Notification API",
        "model": OPENAI_MODEL,
    }


@app.get("/health")
def health():
    return {
        "status": "ok",
        "openai_configured": client is not None,
        "model": OPENAI_MODEL,
    }


@app.post("/daily-message")
def daily_message(request: GenerateRequest):
    if client is None:
        raise HTTPException(
            status_code=500,
            detail="OPENAI_API_KEY is not configured",
        )

    category = FIXED_STYLE

    language = request.language.strip()
    if language not in ALLOWED_LANGUAGES:
        language = "Burmese + English"

    for _ in range(5):
        try:
            message = generate_ai_message(category, language)
        except Exception as exc:
            raise HTTPException(
                status_code=502,
                detail=f"AI generation failed: {exc}",
            ) from exc

        msg_hash = message_hash(message.my, message.en)
        if already_used(request.device_id, msg_hash):
            continue

        save_message(
            request.device_id,
            category,
            language,
            message.my,
            message.en,
            msg_hash,
        )

        return {
            "success": True,
            "data": {
                "category": category,
                "language": language,
                "my": message.my,
                "en": message.en,
                "hash": msg_hash,
                "source": "openai",
                "model": OPENAI_MODEL,
            },
        }

    raise HTTPException(
        status_code=500,
        detail="Could not generate a new unique message",
    )
