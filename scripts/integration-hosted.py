#!/usr/bin/env python3
"""Real Auth/Java/PostgREST acceptance using existing development users.

Secrets come only from process environment. Creates and cleans its own data;
never creates/deletes users, changes trusted roles, or resets the database.
"""
from http.cookiejar import CookieJar
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
import uuid

REF = "dajpnhkutkhgxwkjzvpg"
URL = os.environ.get("SUPABASE_URL", "").rstrip("/")
KEY = os.environ.get("SUPABASE_ANON_KEY", "")
if URL != f"https://{REF}.supabase.co" or not KEY:
    raise SystemExit("Provide the authorized development URL and public key through environment.")
accounts = {}
for role in ["staff", "member"]:
    prefix = "DEPOR_TEST_" + role.upper()
    fields = {field: os.environ.get(prefix + "_" + field.upper(), "")
              for field in ["email", "password", "uuid"]}
    if not all(fields.values()):
        raise SystemExit(f"Missing {role} test account environment.")
    uuid.UUID(fields["uuid"])
    accounts[role] = fields
origin = "http://localhost:3000"
remote = urllib.request.build_opener()
passed = 0
created = []
profile_original = None
members_task = None


def http(opener, base, path, method="GET", body=None, headers=None):
    raw = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(base + path, data=raw, method=method,
                                 headers={"Content-Type": "application/json", **(headers or {})})
    try:
        response = opener.open(req, timeout=140)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        try:
            payload = json.loads(response.read())
        except (ValueError, UnicodeDecodeError):
            payload = None
        return response.status, payload, response.headers


def check(label, condition):
    global passed
    if not condition:
        raise RuntimeError("FAIL: " + label)
    passed += 1
    print("PASS: " + label, flush=True)


class Client:
    def __init__(self, base, role):
        self.base = base
        self.role = role
        self.token = None
        self.cookies = CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.ProxyHandler({}),
                                                  urllib.request.HTTPCookieProcessor(self.cookies))

    def call(self, path, method="GET", body=None, expected=200, bearer=True):
        headers = {"Origin": origin}
        if bearer and self.token:
            headers["Authorization"] = "Bearer " + self.token
        status, data, response_headers = http(self.opener, self.base, "/api" + path, method, body, headers)
        if status != expected:
            code = data.get("error", {}).get("code", "") if isinstance(data, dict) else ""
            raise RuntimeError(f"{self.role} {method} {path}: HTTP {status}, expected {expected}; code={code}")
        return data, response_headers

    def login(self):
        account = accounts[self.role]
        data, headers = self.call("/auth/login", "POST", {"email": account["email"], "password": account["password"]}, bearer=False)
        session = data["data"]
        self.token = session["accessToken"]
        check(self.role + " real Auth login and trusted profile",
              str(session["user"]["id"]) == account["uuid"] and session["user"]["role"] == self.role)
        cookie = headers.get("Set-Cookie", "")
        check(self.role + " HttpOnly refresh cookie", "HttpOnly" in cookie and "Path=/api/auth" in cookie)

    def rest(self, table, suffix="", method="GET", body=None):
        return http(remote, URL, "/rest/v1/" + table + suffix, method, body,
                    {"apikey": KEY, "Authorization": "Bearer " + self.token,
                     "Prefer": "return=representation"})


repo = Path(__file__).resolve().parents[1]
jar = repo / "target/depor-hub-1.0.0.jar"
if not jar.is_file():
    raise SystemExit("Build the JAR with mvn verify first.")
java = str(Path(os.environ["JAVA_HOME"]) / "bin/java") if os.environ.get("JAVA_HOME") else "java"
with socket.socket() as sock:
    sock.bind(("127.0.0.1", 0))
    port = sock.getsockname()[1]
base = f"http://127.0.0.1:{port}"
args = [java]
proxy = urllib.parse.urlsplit(os.environ.get("HTTPS_PROXY", ""))
if proxy.hostname and proxy.port:
    if proxy.username or proxy.password:
        raise SystemExit("Authenticated proxy requires separately configured Java proxy credentials.")
    args += [f"-Dhttps.proxyHost={proxy.hostname}", f"-Dhttps.proxyPort={proxy.port}",
             "-Dhttp.nonProxyHosts=localhost|127.*"]
args += ["-jar", str(jar), "--server.address=127.0.0.1", "--logging.level.id.csui.depor.Supabase=DEBUG"]
env = {**{key: value for key, value in os.environ.items() if not key.startswith("DEPOR_TEST_")},
       "PORT": str(port), "APP_ALLOWED_ORIGINS": origin, "APP_COOKIE_SECURE": "false",
       "SUPABASE_CONNECT_TIMEOUT_MS": "15000", "SUPABASE_READ_TIMEOUT_MS": "30000"}
staff, member = Client(base, "staff"), Client(base, "member")
run = "Integration " + uuid.uuid4().hex[:10]
date = time.strftime("%Y-%m-%d", time.gmtime())
failure = None
cleanup_errors = []
logged_out = set()
with tempfile.TemporaryFile() as log:
    process = subprocess.Popen(args, env=env, stdout=log, stderr=subprocess.STDOUT)
    try:
        deadline = time.monotonic() + 35
        while True:
            if process.poll() is not None:
                raise RuntimeError("Backend exited during startup.")
            try:
                if http(staff.opener, base, "/api/health")[0] == 200:
                    break
            except urllib.error.URLError:
                pass
            if time.monotonic() > deadline:
                raise RuntimeError("Backend startup timed out.")
            time.sleep(0.25)
        staff.login()
        member.login()
        baseline = staff.call("/dashboard")[0]["data"]["summary"]
        payloads = {
            "programs": {"name": run, "description": "Temporary integration fixture", "pic": "Development staff",
                         "pic_id": accounts["staff"]["uuid"], "start_date": date, "end_date": date,
                         "status": "Planning", "progress": 0, "budget": 1000},
            "tasks": {"title": run, "description": "Temporary fixture", "program_id": None,
                      "assignee_id": accounts["staff"]["uuid"], "status": "Backlog", "priority": "Medium", "due_date": date},
            "finances": {"description": run, "program_id": None, "type": "expense", "amount": 100,
                         "category": "Integration test", "transaction_date": date, "status": "approved"},
            "inventory": {"name": run, "category": "Integration test", "quantity": 2, "status": "Available",
                          "condition": "Baik", "emoji": None, "location": "Development"},
            "events": {"name": run, "description": "Temporary fixture", "program_id": None, "venue": "Development",
                       "start_date": date, "end_date": date, "status": "Planning", "permit_status": "Pending"},
        }
        ids = {}
        for table, body in payloads.items():
            if table in ["tasks", "finances", "events"]:
                body["program_id"] = ids["programs"]
            row = staff.call("/" + table, "POST", body, 201)[0]["data"]
            ids[table] = str(uuid.UUID(row["id"]))
            created.append((table, ids[table]))
            check("staff creates " + table, row["created_by"] == accounts["staff"]["uuid"])
        payloads["programs"].update(name=run + " updated", progress=25)
        payloads["tasks"]["status"] = "Done"
        payloads["finances"]["amount"] = 125
        payloads["inventory"]["quantity"] = 3
        payloads["events"]["permit_status"] = "Approved"
        for table, body in payloads.items():
            row = staff.call("/" + table + "/" + ids[table], "PUT", body)[0]["data"]
            check("staff updates " + table, row["id"] == ids[table])
        expected_values = {"programs": ("progress", 25), "tasks": ("status", "Done"),
                           "finances": ("amount", 125), "inventory": ("quantity", 3),
                           "events": ("permit_status", "Approved")}
        for table, (field, value) in expected_values.items():
            status, rows, _ = member.rest(table, "?id=eq." + ids[table] + "&select=*")
            check("persisted " + table + " visible via member PostgREST",
                  status == 200 and len(rows) == 1 and rows[0][field] == value)
        stats = staff.call("/dashboard")[0]["data"]["summary"]
        check("dashboard aggregates real persisted data",
              stats["budget"] == baseline["budget"] + 1000 and stats["expense"] == baseline["expense"] + 125
              and stats["totalAssets"] == baseline["totalAssets"] + 3
              and stats["remaining"] == baseline["remaining"] + 875)
        for table in ["programs", "finances", "inventory", "events"]:
            member.call("/" + table, "POST", payloads[table], 403)
            check("member cannot create " + table, True)
        member.call("/tasks/" + ids["tasks"], "PUT", payloads["tasks"], 403)
        check("member cannot edit another owner's task", True)
        staff.call("/programs/" + ids["programs"], "DELETE", expected=409)
        check("linked program deletion returns conflict", True)
        own = {**payloads["tasks"], "title": run + " member", "assignee_id": None, "status": "Backlog"}
        row = member.call("/tasks", "POST", own, 201)[0]["data"]
        members_task = str(uuid.UUID(row["id"]))
        check("member creates own task", row["assignee_id"] == accounts["member"]["uuid"])
        own["assignee_id"] = accounts["member"]["uuid"]
        own["status"] = "Done"
        member.call("/tasks/" + members_task, "PUT", own)
        status, rows, _ = member.rest("tasks", "?id=eq." + members_task)
        check("member task update persists", status == 200 and rows[0]["status"] == "Done")
        member.call("/tasks/" + members_task, "PUT", {**own, "assignee_id": accounts["staff"]["uuid"]}, 403)
        check("member cannot reassign task through backend", True)
        status, _, _ = member.rest("tasks", "?id=eq." + members_task, "PATCH", {"assignee_id": accounts["staff"]["uuid"]})
        check("direct PostgREST RLS blocks task reassignment", status == 403)
        status, _, _ = member.rest("profiles", "?id=eq." + accounts["member"]["uuid"], "PATCH", {"role": "admin"})
        check("direct PostgREST column grants block role escalation", status == 403)
        status, rows, _ = member.rest("profiles", "?id=eq." + accounts["member"]["uuid"])
        check("trusted member role remains unchanged", status == 200 and rows[0]["role"] == "member")
        profile_original = rows[0]["name"]
        member.call("/profiles/me", "PUT", {"name": run + " member"})
        status, rows, _ = member.rest("profiles", "?id=eq." + accounts["member"]["uuid"])
        check("own profile name persists", status == 200 and rows[0]["name"] == run + " member")
        status, before, _ = member.rest("profiles", "?id=eq." + accounts["staff"]["uuid"])
        status, changed, _ = member.rest("profiles", "?id=eq." + accounts["staff"]["uuid"], "PATCH", {"name": run + " forbidden"})
        status2, after, _ = staff.rest("profiles", "?id=eq." + accounts["staff"]["uuid"])
        check("RLS blocks another profile's name change", status == 200 and changed == [] and status2 == 200 and before[0]["name"] == after[0]["name"])
        member.call("/profiles/me", "PUT", {"name": profile_original})
        profile_original = None
        member.call("/tasks/" + members_task, "DELETE")
        status, rows, _ = member.rest("tasks", "?id=eq." + members_task)
        check("member deletes own task persistently", status == 200 and rows == [])
        members_task = None
        for table in ["events", "tasks", "finances", "inventory", "programs"]:
            staff.call("/" + table + "/" + ids[table], "DELETE")
            status, rows, _ = staff.rest(table, "?id=eq." + ids[table])
            check("staff deletes " + table + " persistently", status == 200 and rows == [])
            created.remove((table, ids[table]))
        check("dashboard returns to baseline after cleanup", staff.call("/dashboard")[0]["data"]["summary"] == baseline)
        for client in [staff, member]:
            old = next(c.value for c in client.cookies if c.name == "depor_refresh")
            data, _ = client.call("/auth/refresh", "POST", {}, bearer=False)
            client.token = data["data"]["accessToken"]
            new = next(c.value for c in client.cookies if c.name == "depor_refresh")
            check(client.role + " real refresh rotates session cookie", new != old and data["data"]["user"]["role"] == client.role)
            check(client.role + " refreshed access token authenticates", client.call("/auth/session")[0]["data"]["role"] == client.role)
            cookie_value = new
            client.call("/auth/logout", "POST", {})
            logged_out.add(client.role)
            check(client.role + " logout clears browser refresh cookie", not any(c.name == "depor_refresh" for c in client.cookies))
            status, payload, _ = http(client.opener, base, "/api/auth/refresh", "POST", {},
                                       {"Origin": origin, "Cookie": "depor_refresh=" + cookie_value})
            check(client.role + " logged-out refresh token cannot be reused", status == 401 and payload["error"]["code"] == "UNAUTHENTICATED")
    except Exception as error:
        failure = error
    finally:
        if profile_original is not None:
            try:
                member.call("/profiles/me", "PUT", {"name": profile_original})
            except Exception:
                cleanup_errors.append("member profile restore")
        if members_task:
            try:
                staff.call("/tasks/" + members_task, "DELETE")
            except Exception:
                cleanup_errors.append("member task")
        for table, row_id in reversed(created):
            try:
                staff.call("/" + table + "/" + row_id, "DELETE")
            except Exception:
                cleanup_errors.append(table + " fixture")
        for client in [staff, member]:
            if client.token and client.role not in logged_out:
                try:
                    client.call("/auth/logout", "POST", {})
                except Exception:
                    cleanup_errors.append(client.role + " logout")
        process.terminate()
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()
        if failure or cleanup_errors:
            log.seek(0)
            for line in log.read().decode(errors="replace").splitlines():
                if "Supabase transport failure:" in line:
                    print("Diagnostic: " + line.rsplit("Supabase transport failure:", 1)[1].strip(), flush=True)
if cleanup_errors:
    print("Cleanup requires attention: " + ", ".join(cleanup_errors), flush=True)
    raise SystemExit(1)
if failure:
    print(str(failure), flush=True)
    raise SystemExit(1)
print(f"PASS: {passed} real Auth/Java/PostgREST checks; fixtures removed and profile restored.", flush=True)
print("React browser reload and browser cookie policies are outside this API acceptance test.")
