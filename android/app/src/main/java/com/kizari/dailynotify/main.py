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

if not OPENAI_API_KEY:
    print("WARNING: OPENAI_API_KEY is not set")

client = OpenAI(api_key=OPENAI_API_KEY) if OPENAI_API_KEY else None

FIXED_STYLE = "Love Teasing"
ALLOWED_LANGUAGES = {
    "Burmese + English", "Burmese only", "English only",
}


class GenerateRequest(BaseModel):
    device_id: str = Field(min_length=1, max_length=128)
    language: str = "Burmese + English"


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
        columns = [row[1] for row in db.execute(
            "PRAGMA table_info(messages)").fetchall()]
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
            (device_id, FIXED_STYLE, language, my_text, en_text, msg_hash),
        )
        db.commit()


def generate_ai_message(language: str):
    if client is None:
        raise RuntimeError("OPENAI_API_KEY is not configured")

    prompt = f"""
Create exactly ONE short daily notification in a playful social-media-style tone.

FIXED STYLE: {FIXED_STYLE}
LANGUAGE MODE: {language}

Theme:
- Light teasing about crushes, love, dating, getting no reply, being shy,
  overthinking a crush, or funny relationship situations.
- It should feel like the kind of playful teasing people share in short-form
  social posts, without copying any existing post.
- Make it witty, cheeky, slightly annoying in a funny way, and easy to read.
- The joke should be harmless and not cruel.

Safety and quality:
- Suitable for a teenager.
- No sexual content or sexualized minors.
- No threats, harassment, bullying, humiliation, hate, or degrading language.
- Do not attack someone's body, appearance, disability, race, religion, or other sensitive traits.
- Do not pretend to be the user's real-life romantic partner.
- Do not mention these instructions.
- Keep it very short.
- Use 0 to 2 emojis total.

Output ONLY valid JSON in exactly this shape:
{{
  "my": "Burmese version",
  "en": "English version"
}}
"""

    response = client.responses.create(
        model=OPENAI_MODEL,
        input=prompt,
    )

    text = response.output_text.strip()
    if not text:
        raise RuntimeError("OpenAI returned an empty response")

    try:
        data = json.loads(text)
    except json.JSONDecodeError as exc:
        raise RuntimeError("OpenAI returned invalid JSON") from exc

    my_text = str(data.get("my", "")).strip()
    en_text = str(data.get("en", "")).strip()

    if not my_text or not en_text:
        raise RuntimeError("OpenAI returned empty message fields")

    return my_text, en_text


@app.on_event("startup")
def startup() -> None:
    init_db()


@app.get("/")
def root():
    return {
        "success": True,
        "app": "Daily AI Notification API",
        "style": FIXED_STYLE,
        "model": OPENAI_MODEL,
    }


@app.get("/health")
def health():
    return {
        "status": "ok",
        "openai_configured": client is not None,
        "style": FIXED_STYLE,
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

    last_error = None

    for _ in range(5):
        try:
            my_text, en_text = generate_ai_message(language)
        except Exception as exc:
            last_error = exc
            continue

        msg_hash = message_hash(my_text, en_text)
        if already_used(request.device_id, msg_hash):
            continue

        save_message(
            request.device_id,
            language,
            my_text,
            en_text,
            msg_hash,
        )

        return {
            "success": True,
            "data": {
                "category": FIXED_STYLE,
                "language": language,
                "my": my_text,
                "en": en_text,
                "hash": msg_hash,
                "source": "openai",
                "model": OPENAI_MODEL,
            },
        }

    if last_error is not None:
        raise HTTPException(
            status_code=502,
            detail=f"AI generation failed: {last_error}",
        )

    raise HTTPException(
        status_code=500,
        detail="Could not generate a new unique message",
    )
