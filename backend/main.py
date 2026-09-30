import hashlib
import json
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

client = OpenAI(
    api_key=OPENAI_API_KEY,
    timeout=25.0,
    max_retries=1,
) if OPENAI_API_KEY else None

FIXED_STYLE = "Love Teasing"
ALLOWED_LANGUAGES = {
    "Burmese + English",
    "Burmese only",
    "English only",
}


class GenerateRequest(BaseModel):
    device_id: str = Field(min_length=1, max_length=128)
    category: str = FIXED_STYLE
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
            row[1] for row in db.execute(
                "PRAGMA table_info(messages)"
            ).fetchall()
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


def generate_ai_message(language: str) -> AIMessage:
    if client is None:
        raise RuntimeError("OPENAI_API_KEY is not configured")

    if language == "Burmese only":
        language_rules = "Return a natural Burmese message in my. Set en to an empty string."
    elif language == "English only":
        language_rules = "Set my to an empty string. Return a natural English message in en."
    else:
        language_rules = "Return both a natural Burmese message in my and a natural English message in en."

    prompt = f"""
Create one short daily social-media-style notification.

Fixed style: Love Teasing.
Topic: playful teasing about crushes, dating, or everyday love situations.

{language_rules}

Rules:
- Natural, funny, clever, slightly teasing.
- It can make the reader feel lightly called out, but never bullied, harassed, or cruel.
- Suitable for teenagers.
- No sexual content.
- No threats, self-harm, dangerous behavior, or insults targeting protected traits.
- Keep the used fields very short, ideally 1-2 sentences each.
- Use 0-2 emojis total.
- Return ONLY JSON with exactly two string fields: my and en.
"""

    response = client.responses.create(
        model=OPENAI_MODEL,
        input=prompt,
        store=False,
        reasoning={"effort": "none"},
        max_output_tokens=180,
        text={
            "format": {
                "type": "json_schema",
                "name": "daily_notification",
                "strict": True,
                "schema": {
                    "type": "object",
                    "properties": {
                        "my": {"type": "string"},
                        "en": {"type": "string"},
                    },
                    "required": ["my", "en"],
                    "additionalProperties": False,
                },
            }
        },
    )

    raw = response.output_text.strip()
    if not raw:
        raise RuntimeError("OpenAI returned no text output")

    try:
        data = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise RuntimeError("OpenAI returned invalid JSON") from exc

    message = AIMessage.model_validate(data)

    my_text = message.my.strip()
    en_text = message.en.strip()

    if language == "Burmese only":
        en_text = ""
    elif language == "English only":
        my_text = ""

    if not my_text and not en_text:
        raise RuntimeError("OpenAI returned empty message")

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

    language = request.language.strip()
    if language not in ALLOWED_LANGUAGES:
        language = "Burmese + English"

    for _ in range(3):
        try:
            message = generate_ai_message(language)
        except Exception as exc:
            raise HTTPException(
                status_code=502,
                detail=f"AI generation failed: {type(exc).__name__}: {exc}",
            ) from exc

        msg_hash = message_hash(message.my, message.en)
        if already_used(request.device_id, msg_hash):
            continue

        save_message(
            request.device_id,
            FIXED_STYLE,
            language,
            message.my,
            message.en,
            msg_hash,
        )

        return {
            "success": True,
            "data": {
                "category": FIXED_STYLE,
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
