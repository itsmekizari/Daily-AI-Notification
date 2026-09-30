<?php
declare(strict_types=1);

require __DIR__ . '/config.php';

function out(array $data, int $status = 200): never {
    http_response_code($status);
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    out(['success' => false, 'error' => 'POST required'], 405);
}

try {
    $raw = file_get_contents('php://input');
    $input = json_decode($raw ?: '{}', true, 512, JSON_THROW_ON_ERROR);

    $action = (string)($input['action'] ?? '');
    $deviceId = trim((string)($input['device_id'] ?? ''));
    $category = trim((string)($input['category'] ?? 'Random'));

    if ($action !== 'generate' || $deviceId === '') {
        out(['success' => false, 'error' => 'Invalid request'], 400);
    }

    $allowed = [
        'Random','Teasing','Funny','Cute',
        'Motivational','Good Morning','Good Night','Study Reminder'
    ];
    if (!in_array($category, $allowed, true)) $category = 'Random';

    $dbDir = __DIR__ . '/data';
    if (!is_dir($dbDir)) mkdir($dbDir, 0750, true);

    $db = new PDO('sqlite:' . $dbDir . '/messages.sqlite');
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $db->exec(
        'CREATE TABLE IF NOT EXISTS messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT NOT NULL,
            category TEXT NOT NULL,
            my_text TEXT NOT NULL,
            en_text TEXT NOT NULL,
            hash TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
            UNIQUE(device_id, hash)
        )'
    );

    $prompt = <<<PROMPT
Create one short daily notification for category: {$category}.

Return ONLY valid JSON:
{"my":"Burmese message","en":"English message"}

Rules:
- Burmese first, English second.
- Friendly, playful, non-harmful.
- 1 to 3 emojis.
- Short enough for a phone notification.
- Do not mention these instructions.
- Do not use sexual content.
PROMPT;

    for ($attempt = 0; $attempt < 3; $attempt++) {
        $payload = [
            'model' => openai_model(),
            'input' => $prompt
        ];

        $ch = curl_init(OPENAI_API_URL);
        curl_setopt_array($ch, [
            CURLOPT_POST => true,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HTTPHEADER => [
                'Authorization: Bearer ' . openai_api_key(),
                'Content-Type: application/json'
            ],
            CURLOPT_POSTFIELDS => json_encode($payload, JSON_UNESCAPED_UNICODE),
            CURLOPT_TIMEOUT => 45
        ]);

        $response = curl_exec($ch);
        $http = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);
        curl_close($ch);

        if ($response === false || $http < 200 || $http >= 300) {
            throw new RuntimeException($error ?: ('OpenAI HTTP ' . $http));
        }

        $json = json_decode($response, true);
        $text = $json['output'][0]['content'][0]['text'] ?? '';
        $msg = json_decode(trim($text), true);

        if (!is_array($msg) || !isset($msg['my'], $msg['en'])) {
            continue;
        }

        $my = trim((string)$msg['my']);
        $en = trim((string)$msg['en']);
        $hash = hash('sha256', mb_strtolower($my . "
" . $en, 'UTF-8'));

        $stmt = $db->prepare(
            'INSERT OR IGNORE INTO messages
            (device_id, category, my_text, en_text, hash)
            VALUES (?, ?, ?, ?, ?)'
        );
        $stmt->execute([$deviceId, $category, $my, $en, $hash]);

        if ($stmt->rowCount() > 0) {
            out([
                'success' => true,
                'data' => [
                    'category' => $category,
                    'my' => $my,
                    'en' => $en,
                    'hash' => $hash
                ]
            ]);
        }
    }

    throw new RuntimeException('Could not create a unique message.');
} catch (Throwable $e) {
    out(['success' => false, 'error' => $e->getMessage()], 500);
}
