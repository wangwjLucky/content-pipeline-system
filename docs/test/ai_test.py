import urllib.request, json, sys

# ========== Java Backend API Tests ==========
print("=" * 60)
print("  AI Model Tests - Java Backend")
print("=" * 60)

login_data = json.dumps({"username": "admin", "password": "admin123"}).encode()
req = urllib.request.Request("http://localhost:8080/api/v1/auth/login", data=login_data,
    headers={"Content-Type": "application/json"})
resp = urllib.request.urlopen(req)
token = json.loads(resp.read())["data"]["token"]
auth = "Bearer " + token

def api(method, path, body=None):
    data = json.dumps(body, ensure_ascii=False).encode() if body else None
    req = urllib.request.Request(f"http://localhost:8080{path}", data=data,
        headers={"Content-Type": "application/json; charset=utf-8", "Authorization": auth})
    req.method = method
    try:
        resp = urllib.request.urlopen(req)
        return resp.status, json.loads(resp.read())
    except urllib.request.HTTPError as e:
        return e.code, json.loads(e.read())

results = {"pass": 0, "fail": 0, "details": []}

def check(name, status, data, expect_code=200):
    ok = data.get("code") == expect_code
    if ok:
        results["pass"] += 1
    else:
        results["fail"] += 1
    results["details"].append((name, "PASS" if ok else "FAIL", status, data))
    print(f"  {"PASS" if ok else "FAIL"} {name}")

# === Java AiModelController ===
print("\n--- Java: AiModelController CRUD ---")

s, d = api("GET", "/api/v1/ai-models?page=1&size=20")
check("List models", s, d)

s, d = api("POST", "/api/v1/ai-models", {
    "provider": "openai", "modelName": "gpt-4o-test",
    "apiKey": "sk-test-key-12345", "baseUrl": "https://api.openai.com/v1", "enabled": True
})
check("Create model", s, d)
model_id = d.get("data", {}).get("id") if d.get("code") == 200 else None

if model_id:
    s, d = api("GET", f"/api/v1/ai-models/{model_id}")
    check("Get model by id", s, d)

if model_id:
    s, d = api("POST", f"/api/v1/ai-models/{model_id}/test")
    check("Test model connection", s, d)

if model_id:
    s, d = api("PUT", f"/api/v1/ai-models/{model_id}", {
        "modelName": "gpt-4o-test-updated", "enabled": False
    })
    check("Update model", s, d)

s, d = api("GET", "/api/v1/ai-models/99999")
check("Get non-existent model", s, d, expect_code=404)

if model_id:
    s, d = api("DELETE", f"/api/v1/ai-models/{model_id}")
    check("Delete model", s, d)

s, d = api("POST", "/api/v1/ai-models/test-mq")
check("Test MQ connection", s, d)

# === Edge Cases ===
print("\n--- Java: Edge Cases ---")

s, d = api("POST", "/api/v1/ai-models", {"provider": "", "modelName": "", "enabled": True})
check("Create model with empty fields", s, d)

s, d = api("POST", "/api/v1/ai-models", {"modelName": "test-model"})
check("Create model without provider", s, d)

# ========== AI Gateway Tests ==========
print("\n" + "=" * 60)
print("  AI Gateway Tests")
print("=" * 60)

gw = "http://localhost:8001"

s, d = 200, {}
try:
    r = urllib.request.urlopen(f"{gw}/health", timeout=5)
    d = json.loads(r.read())
except Exception as e:
    s, d = 0, {"error": str(e)}
print(f"  {"PASS" if s == 200 and d.get("status") == "ok" else "FAIL"} GET /health")

s, d = 200, {}
try:
    r = urllib.request.urlopen(f"{gw}/ai/v1/models", timeout=5)
    d = json.loads(r.read())
    s = r.status
except Exception as e:
    s, d = 0, {"error": str(e)}
has_models = len(d.get("models", [])) > 0
print(f"  {"PASS" if s == 200 and has_models else "FAIL"} GET /ai/v1/models ({len(d.get("models", []))} models)")

s, d = 200, {}
try:
    req = urllib.request.Request(f"{gw}/ai/v1/chat",
        data=json.dumps({"model": "gpt-4o", "messages": [{"role": "user", "content": "Hello"}]}).encode(),
        headers={"Content-Type": "application/json"})
    r = urllib.request.urlopen(req, timeout=10)
    d = json.loads(r.read())
    s = r.status
except Exception as e:
    s, d = 0, {"error": str(e)}
print(f"  {"PASS" if s == 200 and d.get("content") else "FAIL"} POST /ai/v1/chat")

s, d = 200, {}
try:
    req = urllib.request.Request(f"{gw}/ai/v1/chat",
        data=json.dumps({"model": "nonexistent-model", "messages": [{"role": "user", "content": "Hi"}]}).encode(),
        headers={"Content-Type": "application/json"})
    r = urllib.request.urlopen(req, timeout=5)
    d = json.loads(r.read())
    s = r.status
except urllib.request.HTTPError as e:
    s, d = e.code, json.loads(e.read())
except Exception as e:
    s, d = 0, {"error": str(e)}
print(f"  {"PASS" if s == 400 else "FAIL"} POST /ai/v1/chat (unsupported model) HTTP {s}")

s, d = 200, {}
try:
    req = urllib.request.Request(f"{gw}/ai/v1/chat/openai",
        data=json.dumps({"model": "gpt-4o", "messages": [{"role": "user", "content": "Hi"}]}).encode(),
        headers={"Content-Type": "application/json"})
    r = urllib.request.urlopen(req, timeout=10)
    d = json.loads(r.read())
    s = r.status
except Exception as e:
    s, d = 0, {"error": str(e)}
print(f"  {"PASS" if s == 200 and d.get("content") else "FAIL"} POST /ai/v1/chat/openai")

s, d = 200, {}
try:
    req = urllib.request.Request(f"{gw}/ai/v1/chat/nonexistent",
        data=json.dumps({"model": "gpt-4o", "messages": [{"role": "user", "content": "Hi"}]}).encode(),
        headers={"Content-Type": "application/json"})
    r = urllib.request.urlopen(req, timeout=5)
    s = r.status
except urllib.request.HTTPError as e:
    s = e.code
except Exception as e:
    s = 0
print(f"  {"PASS" if s == 404 else "FAIL"} POST /ai/v1/chat/nonexistent (404 expected) HTTP {s}")

s, d = 200, {}
try:
    req = urllib.request.Request(f"{gw}/ai/v1/generate",
        data=json.dumps({"model": "gpt-4o", "prompt": "Tell me a joke"}).encode(),
        headers={"Content-Type": "application/json"})
    r = urllib.request.urlopen(req, timeout=10)
    d = json.loads(r.read())
    s = r.status
except Exception as e:
    s, d = 0, {"error": str(e)}
print(f"  {"PASS" if s == 200 and d.get("result") else "FAIL"} POST /ai/v1/generate")

s, d = 200, {}
try:
    req = urllib.request.Request(f"{gw}/ai/v1/script/generate",
        data=json.dumps({"task_id": 1, "topic_title": "AI Technology", "model": "gpt-4o"}).encode(),
        headers={"Content-Type": "application/json"})
    r = urllib.request.urlopen(req, timeout=10)
    d = json.loads(r.read())
    s = r.status
except Exception as e:
    s, d = 0, {"error": str(e)}
print(f"  {"PASS" if s == 200 and d.get("task_id") else "FAIL"} POST /ai/v1/script/generate")

s, d = 200, {}
try:
    req = urllib.request.Request(f"{gw}/ai/v1/script/rewrite",
        data=json.dumps({"content": "Original script content", "instructions": "Make it shorter"}).encode(),
        headers={"Content-Type": "application/json"})
    r = urllib.request.urlopen(req, timeout=10)
    d = json.loads(r.read())
    s = r.status
except Exception as e:
    s, d = 0, {"error": str(e)}
print(f"  {"PASS" if s == 200 and d.get("content") else "FAIL"} POST /ai/v1/script/rewrite")

s, d = 200, {}
try:
    req = urllib.request.Request(f"{gw}/ai/v1/prompt/generate",
        data=json.dumps({"task_id": 1, "storyboard_content": "A man walking in the park", "model": "gpt-4o"}).encode(),
        headers={"Content-Type": "application/json"})
    r = urllib.request.urlopen(req, timeout=10)
    d = json.loads(r.read())
    s = r.status
except Exception as e:
    s, d = 0, {"error": str(e)}
print(f"  {"PASS" if s == 200 else "FAIL"} POST /ai/v1/prompt/generate")

s, d = 200, {}
try:
    req = urllib.request.Request(f"{gw}/ai/v1/prompt/generate",
        data=json.dumps({"task_id": 1, "content": "wrong field name"}).encode(),
        headers={"Content-Type": "application/json"})
    r = urllib.request.urlopen(req, timeout=5)
    s = r.status
except urllib.request.HTTPError as e:
    s = e.code
except Exception as e:
    s = 0
print(f"  {"PASS" if s == 422 else "FAIL"} POST /ai/v1/prompt/generate (wrong field, 422 expected) HTTP {s}")

s, d = 200, {}
try:
    req = urllib.request.Request(f"{gw}/ai/v1/video/generate",
        data=json.dumps({"task_id": 1, "prompt": "A cat playing piano", "model": "kling-v1"}).encode(),
        headers={"Content-Type": "application/json"})
    r = urllib.request.urlopen(req, timeout=10)
    d = json.loads(r.read())
    s = r.status
except Exception as e:
    s, d = 0, {"error": str(e)}
print(f"  {"PASS" if s == 200 and d.get("video_task_id") else "FAIL"} POST /ai/v1/video/generate")

s, d = 200, {}
try:
    r = urllib.request.urlopen(f"{gw}/ai/v1/video/test-task-123", timeout=5)
    d = json.loads(r.read())
    s = r.status
except Exception as e:
    s, d = 0, {"error": str(e)}
print(f"  {"PASS" if s == 200 and d.get("status") == "completed" else "FAIL"} GET /ai/v1/video/test-task-123")

s, d = 200, {}
try:
    req = urllib.request.Request(f"{gw}/ai/v1/image/generate",
        data=json.dumps({"prompt": "A beautiful sunset", "model": "kling-v1"}).encode(),
        headers={"Content-Type": "application/json"})
    r = urllib.request.urlopen(req, timeout=10)
    d = json.loads(r.read())
    s = r.status
except Exception as e:
    s, d = 0, {"error": str(e)}
print(f"  {"PASS" if s == 200 and d.get("status") == "pending" else "FAIL"} POST /ai/v1/image/generate")

s, d = 200, {}
try:
    req = urllib.request.Request(f"{gw}/ai/v1/embedding?text=hello&model=text-embedding-3-small",
        data=b"{}", headers={"Content-Type": "application/json"})
    req.method = "POST"
    r = urllib.request.urlopen(req, timeout=5)
    d = json.loads(r.read())
    s = r.status
except Exception as e:
    s, d = 0, {"error": str(e)}
print(f"  {"PASS" if s == 200 else "FAIL"} GET /ai/v1/embedding")

s, d = 200, {}
try:
    req = urllib.request.Request(f"{gw}/ai/v1/voice/generate",
        data=json.dumps({"task_id": 1, "text": "Hello world", "voice_type": "doubao-tts-1"}).encode(),
        headers={"Content-Type": "application/json"})
    r = urllib.request.urlopen(req, timeout=10)
    d = json.loads(r.read())
    s = r.status
except Exception as e:
    s, d = 0, {"error": str(e)}
print(f"  {"PASS" if s == 200 else "FAIL"} POST /ai/v1/voice/generate")

# ========== Python AI Service Health ==========
print("\n" + "=" * 60)
print("  Python AI Services Health")
print("=" * 60)

services = [
    ("script-service", 8002, "script-service"),
    ("prompt-service", 8003, "prompt-service"),
    ("video-service", 8004, "video-service"),
    ("voice-service", 8005, "voice-service"),
    ("ffmpeg-service", 8006, "ffmpeg-service"),
    ("image-service", 8007, "image-service"),
]
for name, port, expected_service in services:
    s, d = 0, {}
    try:
        r = urllib.request.urlopen(f"http://localhost:{port}/health", timeout=5)
        d = json.loads(r.read())
        s = r.status
    except Exception as e:
        s, d = 0, {"error": str(e)}
    ok = s == 200 and d.get("service") == expected_service
    print(f"  {"PASS" if ok else "FAIL"} {name} (:{port})")

# ========== Summary ==========
print("\n" + "=" * 60)
print(f"  Results: {results["pass"]} passed, {results["fail"]} failed")
print("=" * 60)

if results["fail"] > 0:
    print("\n  Failed tests:")
    for name, status, s, d in results["details"]:
        if status == "FAIL":
            print(f"    {name}: HTTP {s} {d.get("message","")}")

sys.exit(0 if results["fail"] == 0 else 1)