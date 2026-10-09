#!/usr/bin/env python3
"""Read-only hosted checks. Does not create users or validate successful login/CRUD."""
import json
import os
from pathlib import Path
import socket
import subprocess
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request

REF = "dajpnhkutkhgxwkjzvpg"
URL = os.environ.get("SUPABASE_URL", "").rstrip("/")
KEY = os.environ.get("SUPABASE_ANON_KEY", "")
if URL != f"https://{REF}.supabase.co" or not KEY:
    raise SystemExit("Set development SUPABASE_URL and SUPABASE_ANON_KEY in the process environment.")

remote = urllib.request.build_opener()
local = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def request(opener, base, path, method="GET", body=None, headers=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(base + path, data=data, method=method,
                                 headers={"Content-Type": "application/json", **(headers or {})})
    try:
        response = opener.open(req, timeout=20)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        raw = response.read()
        try:
            payload = json.loads(raw)
        except (ValueError, UnicodeDecodeError):
            payload = None
        return response.status, payload, response.headers


def check(label, condition):
    if not condition:
        raise RuntimeError(f"FAIL: {label}")
    print(f"PASS: {label}", flush=True)


status, settings, _ = request(remote, URL, "/auth/v1/settings", headers={"apikey": KEY})
check("hosted Auth settings reachable", status == 200 and isinstance(settings, dict))
print("Email confirmation required:", not settings.get("mailer_autoconfirm", False), flush=True)
for table in ["profiles", "programs", "tasks", "finances", "inventory", "events"]:
    status, _, _ = request(remote, URL, f"/rest/v1/{table}?select=id&limit=1", headers={"apikey": KEY})
    check(f"anonymous {table} access denied", status in (401, 403))
status, _, _ = request(remote, URL, "/rest/v1/rpc/dashboard_stats", method="POST", body={}, headers={"apikey": KEY})
check("anonymous dashboard RPC denied", status in (401, 403))

repo = Path(__file__).resolve().parents[1]
jar = repo / "target/depor-hub-1.0.0.jar"
if not jar.is_file():
    raise SystemExit("Build the backend JAR with mvn verify first.")
java = str(Path(os.environ["JAVA_HOME"]) / "bin/java") if os.environ.get("JAVA_HOME") else "java"
with socket.socket() as sock:
    sock.bind(("127.0.0.1", 0))
    port = sock.getsockname()[1]
base = f"http://127.0.0.1:{port}"
args = [java]
proxy = urllib.parse.urlsplit(os.environ.get("HTTPS_PROXY", ""))
if proxy.hostname and proxy.port:
    if proxy.username or proxy.password:
        raise SystemExit("Authenticated proxy requires a separately configured Java environment.")
    args += [f"-Dhttps.proxyHost={proxy.hostname}", f"-Dhttps.proxyPort={proxy.port}",
             "-Dhttp.nonProxyHosts=localhost|127.*"]
args += ["-jar", str(jar), "--server.address=127.0.0.1"]
env = {**os.environ, "PORT": str(port), "APP_ALLOWED_ORIGINS": "http://localhost:3000",
       "APP_COOKIE_SECURE": "false"}
with tempfile.TemporaryFile() as log:
    process = subprocess.Popen(args, env=env, stdout=log, stderr=subprocess.STDOUT)
    try:
        deadline = time.monotonic() + 35
        while True:
            if process.poll() is not None:
                raise RuntimeError("Backend exited during startup; inspect your local log securely.")
            try:
                status, _, _ = request(local, base, "/api/health")
                if status == 200:
                    break
            except urllib.error.URLError:
                pass
            if time.monotonic() > deadline:
                raise RuntimeError("Backend startup timed out.")
            time.sleep(0.25)
        check("backend health", True)
        status, _, _ = request(local, base, "/api/programs")
        check("backend requires authentication", status == 401)
        origin = {"Origin": "http://localhost:3000"}
        invalid = {"email": "depor-readiness-nonexistent@example.invalid", "password": "invalid-test-only"}
        status, payload, _ = request(local, base, "/api/auth/login", "POST", invalid, origin)
        check("real hosted Auth rejects invalid login through Java adapter",
              status == 401 and payload.get("error", {}).get("code") == "UNAUTHENTICATED")
        status, _, _ = request(local, base, "/api/auth/login", "POST", invalid, {"Origin": "https://untrusted.example.invalid"})
        check("untrusted origin rejected", status == 403)
        status, payload, _ = request(local, base, "/api/programs", headers={"Authorization": "Bearer invalid-test-token"})
        check("real hosted Auth rejects forged token through Java adapter",
              status == 401 and payload.get("error", {}).get("code") == "UNAUTHENTICATED")
        status, _, headers = request(local, base, "/api/auth/refresh", "POST", {}, origin)
        check("missing refresh session rejected and cookie cleared", status == 401 and "Max-Age=0" in headers.get("Set-Cookie", ""))
        status, payload, _ = request(local, base, "/api/auth/refresh", "POST", {}, {**origin, "Cookie": "depor_refresh=invalid-test-refresh"})
        check("real hosted Auth rejects invalid refresh through Java adapter",
              status == 401 and payload.get("error", {}).get("code") == "UNAUTHENTICATED")
    finally:
        process.terminate()
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()
print("Read-only hosted checks complete. Successful login/CRUD/refresh/logout still require verified development accounts.")
