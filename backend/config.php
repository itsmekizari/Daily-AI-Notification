<?php

// OpenAI Responses API
define('OPENAI_API_URL', 'https://api.openai.com/v1/responses');

// IMPORTANT:
// Do NOT put your real API key directly in this file.
// Set OPENAI_API_KEY as an environment variable on your server.
define('OPENAI_API_KEY', getenv('OPENAI_API_KEY') ?: '');

// Model used for daily message generation.
define('OPENAI_MODEL', 'gpt-5.6-luna');

// SQLite database location.
define(
    'DB_PATH',
    __DIR__ . DIRECTORY_SEPARATOR . 'daily_messages.sqlite'
);

// Basic security setting.
define('APP_NAME', 'Daily AI Notification');
