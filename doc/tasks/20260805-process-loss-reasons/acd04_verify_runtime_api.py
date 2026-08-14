#!/usr/bin/env python3
"""Verify AC-D04 task-owned runtime API fixtures without printing secrets."""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any


TASK_ID = "20260805-process-loss-reasons"


class VerificationError(RuntimeError):
    pass


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--fixture", default=f"doc/tasks/{TASK_ID}/fixture-summary.json")
    parser.add_argument("--backend-url", default=None)
    parser.add_argument("--tenant-id", type=int, default=None)
    parser.add_argument("--password-env", default="ACD04_TEST_PASSWORD")
    parser.add_argument("--output", default=f"doc/tasks/{TASK_ID}/runtime-api-verification.json")
    return parser.parse_args()


def load_fixture(path: Path) -> dict[str, Any]:
    if not path.exists():
        raise VerificationError(f"Missing fixture summary: {path}")
    return json.loads(path.read_text(encoding="utf-8"))


def require_password(env_name: str) -> str:
    password = os.environ.get(env_name)
    if not password:
        raise VerificationError(f"Missing required environment variable: {env_name}")
    return password


def request_json(
    base_url: str,
    method: str,
    path: str,
    *,
    tenant_id: int,
    token: str | None = None,
    data: dict[str, Any] | None = None,
    params: dict[str, Any] | None = None,
) -> dict[str, Any]:
    url = base_url.rstrip("/") + "/admin-api" + path
    if params:
        url += "?" + urllib.parse.urlencode(params)
    body = None if data is None else json.dumps(data, ensure_ascii=False).encode("utf-8")
    headers = {
        "Accept": "application/json",
        "tenant-id": str(tenant_id),
    }
    if body is not None:
        headers["Content-Type"] = "application/json;charset=UTF-8"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            payload = response.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        raise VerificationError(f"HTTP {exc.code} for {method} {path}: {safe_backend_message(detail)}") from exc
    except urllib.error.URLError as exc:
        raise VerificationError(f"Connection failed for {method} {path}: {exc.reason}") from exc
    parsed = json.loads(payload)
    code = parsed.get("code", 0)
    if code not in (0, 200):
        raise VerificationError(f"Business code {code} for {method} {path}: {parsed.get('msg') or parsed.get('message')}")
    return parsed


def safe_backend_message(text: str) -> str:
    try:
        parsed = json.loads(text)
        return str(parsed.get("msg") or parsed.get("message") or parsed.get("code") or "backend error")
    except Exception:
        return text[:300]


def login(base_url: str, tenant_id: int, username: str, password: str) -> dict[str, Any]:
    response = request_json(
        base_url,
        "POST",
        "/system/auth/login",
        tenant_id=tenant_id,
        data={"username": username, "password": password},
    )
    data = response.get("data") or {}
    token = data.get("accessToken")
    user_id = data.get("userId")
    if not token or not user_id:
        raise VerificationError(f"Login response missing token or userId for {username}")
    return {"username": username, "userId": user_id, "token": token}


def data_of(response: dict[str, Any]) -> Any:
    return response.get("data")


def list_loss_rows(base_url: str, tenant_id: int, token: str) -> list[dict[str, Any]]:
    response = request_json(
        base_url,
        "GET",
        "/mes/pro/process-pool/team-leader/loss-reasons/page",
        tenant_id=tenant_id,
        token=token,
    )
    data = data_of(response)
    if not isinstance(data, list):
        raise VerificationError("loss-reasons/page did not return a list")
    return data


def find_reason(rows: list[dict[str, Any]], reason_id: int) -> dict[str, Any] | None:
    for row in rows:
        for reason in row.get("reasons") or []:
            if reason.get("id") == reason_id:
                return reason
    return None


def reason_ids(rows: list[dict[str, Any]]) -> set[int]:
    ids: set[int] = set()
    for row in rows:
        for reason in row.get("reasons") or []:
            if reason.get("id") is not None:
                ids.add(int(reason["id"]))
    return ids


def route_process_ids(rows: list[dict[str, Any]]) -> set[int]:
    return {int(row["routeProcessId"]) for row in rows if row.get("routeProcessId") is not None}


def assert_condition(condition: bool, message: str) -> None:
    if not condition:
        raise VerificationError(message)


def runtime_config(base_url: str, tenant_id: int, token: str, params: dict[str, Any]) -> dict[str, Any]:
    response = request_json(
        base_url,
        "GET",
        "/mes/pro/feedback/frontline/device-account/runtime-config",
        tenant_id=tenant_id,
        token=token,
        params=params,
    )
    data = data_of(response)
    if not isinstance(data, dict):
        raise VerificationError("runtime-config did not return an object")
    return data


def create_loss_reason(base_url: str, tenant_id: int, token: str, route_process_id: int, code: str) -> int:
    response = request_json(
        base_url,
        "POST",
        "/mes/pro/process-pool/team-leader/loss-reasons",
        tenant_id=tenant_id,
        token=token,
        data={
            "routeProcessId": route_process_id,
            "reasonCode": code,
            "reasonName": f"{code}-created",
            "enabled": True,
            "remark": "AC-D04 runtime shared CRUD verification",
        },
    )
    reason_id = data_of(response)
    if not isinstance(reason_id, int):
        raise VerificationError("create loss reason did not return numeric id")
    return reason_id


def update_loss_reason(base_url: str, tenant_id: int, token: str, reason_id: int, name: str) -> None:
    request_json(
        base_url,
        "PUT",
        f"/mes/pro/process-pool/team-leader/loss-reasons/{reason_id}",
        tenant_id=tenant_id,
        token=token,
        data={"reasonName": name, "enabled": True, "remark": "AC-D04 runtime shared update verification"},
    )


def delete_loss_reason(base_url: str, tenant_id: int, token: str, reason_id: int) -> None:
    request_json(
        base_url,
        "DELETE",
        f"/mes/pro/process-pool/team-leader/loss-reasons/{reason_id}",
        tenant_id=tenant_id,
        token=token,
    )


def main() -> int:
    args = parse_args()
    fixture_path = Path(args.fixture)
    fixture = load_fixture(fixture_path)
    fixture_data = fixture["fixture"]
    tenant_id = args.tenant_id or int(fixture_data["tenantId"])
    base_url = args.backend_url or fixture_data["backendUrl"]
    password = require_password(args.password_env)
    users = fixture_data["users"]
    route_processes = fixture_data["routeProcesses"]
    reasons = fixture_data["lossReasons"]
    authorized_a = int(route_processes["routeProcessAId"])
    authorized_b = int(route_processes["routeProcessBId"])
    unauthorized = int(route_processes["unauthorizedRouteProcessId"])
    expected_route_processes = {authorized_a, authorized_b}
    created_reason_id: int | None = None

    evidence: dict[str, Any] = {
        "taskId": TASK_ID,
        "generatedAt": time.strftime("%Y-%m-%dT%H:%M:%S"),
        "tenantId": tenant_id,
        "backendUrl": base_url,
        "checks": [],
    }

    try:
        leader_a = login(base_url, tenant_id, users["leaderA"]["username"], password)
        leader_b = login(base_url, tenant_id, users["leaderB"]["username"], password)
        worker = login(base_url, tenant_id, users["worker"]["username"], password)
        evidence["checks"].append({
            "name": "task users can login",
            "status": "PASS",
            "users": {
                "leaderA": {"username": leader_a["username"], "userId": leader_a["userId"]},
                "leaderB": {"username": leader_b["username"], "userId": leader_b["userId"]},
                "worker": {"username": worker["username"], "userId": worker["userId"]},
            },
        })

        rows_a = list_loss_rows(base_url, tenant_id, leader_a["token"])
        rows_b = list_loss_rows(base_url, tenant_id, leader_b["token"])
        ids_a = route_process_ids(rows_a)
        ids_b = route_process_ids(rows_b)
        assert_condition(ids_a == expected_route_processes, f"leader A routeProcess scope mismatch: {sorted(ids_a)}")
        assert_condition(ids_b == expected_route_processes, f"leader B routeProcess scope mismatch: {sorted(ids_b)}")
        assert_condition(unauthorized not in ids_a and unauthorized not in ids_b, "unauthorized routeProcess is visible")
        evidence["checks"].append({
            "name": "leaders see only route-start authorized route processes",
            "status": "PASS",
            "leaderARouteProcessIds": sorted(ids_a),
            "leaderBRouteProcessIds": sorted(ids_b),
            "unauthorizedRouteProcessId": unauthorized,
        })

        code = f"ACD04-{int(time.time())}-API"
        created_reason_id = create_loss_reason(base_url, tenant_id, leader_a["token"], authorized_a, code)
        rows_after_create = list_loss_rows(base_url, tenant_id, leader_b["token"])
        assert_condition(find_reason(rows_after_create, created_reason_id) is not None,
                         "leader B cannot see leader A created shared reason")

        updated_name = f"{code}-updated-by-leader-b"
        update_loss_reason(base_url, tenant_id, leader_b["token"], created_reason_id, updated_name)
        rows_after_update = list_loss_rows(base_url, tenant_id, leader_a["token"])
        updated_reason = find_reason(rows_after_update, created_reason_id)
        assert_condition(updated_reason is not None, "leader A cannot see leader B updated shared reason")
        assert_condition(updated_reason.get("reasonName") == updated_name,
                         "leader A sees stale shared reason name after leader B update")
        evidence["checks"].append({
            "name": "shared CRUD is visible across two authorized production leaders",
            "status": "PASS",
            "createdReasonId": created_reason_id,
            "routeProcessId": authorized_a,
        })

        runtime_before_delete = runtime_config(base_url, tenant_id, worker["token"], {
            "routeId": int(fixture_data["route"]["authorizedRouteId"]),
            "routeProcessId": authorized_a,
            "processId": int(fixture_data["processes"]["processAId"]),
        })
        runtime_reason_ids = {
            int(reason["reasonId"])
            for reason in runtime_before_delete.get("defectReasons") or []
            if reason.get("reasonId") is not None
        }
        assert_condition(int(reasons["enabledReasonId"]) in runtime_reason_ids, "enabled route-process reason missing")
        assert_condition(created_reason_id in runtime_reason_ids, "new enabled shared reason missing from runtime config")
        assert_condition(int(reasons["disabledReasonId"]) not in runtime_reason_ids, "disabled reason appears in runtime config")
        assert_condition(int(reasons["crossProcessReasonId"]) not in runtime_reason_ids,
                         "cross-process reason appears in routeProcess A runtime config")

        delete_loss_reason(base_url, tenant_id, leader_a["token"], created_reason_id)
        runtime_after_delete = runtime_config(base_url, tenant_id, worker["token"], {
            "routeId": int(fixture_data["route"]["authorizedRouteId"]),
            "routeProcessId": authorized_a,
            "processId": int(fixture_data["processes"]["processAId"]),
        })
        runtime_reason_ids_after_delete = {
            int(reason["reasonId"])
            for reason in runtime_after_delete.get("defectReasons") or []
            if reason.get("reasonId") is not None
        }
        assert_condition(created_reason_id not in runtime_reason_ids_after_delete,
                         "deleted shared reason still appears in runtime config")
        rows_after_delete = list_loss_rows(base_url, tenant_id, leader_b["token"])
        deleted_reason = find_reason(rows_after_delete, created_reason_id)
        assert_condition(deleted_reason is not None and deleted_reason.get("enabled") is False,
                         "leader B cannot see deleted reason disabled state")
        evidence["checks"].append({
            "name": "runtime dropdown comes from backend config and filters disabled/deleted/cross-process reasons",
            "status": "PASS",
            "beforeDeleteReasonIds": sorted(runtime_reason_ids),
            "afterDeleteReasonIds": sorted(runtime_reason_ids_after_delete),
            "excludedReasonIds": [
                int(reasons["disabledReasonId"]),
                int(reasons["crossProcessReasonId"]),
                created_reason_id,
            ],
        })
        created_reason_id = None

        evidence["status"] = "PASS"
        output_path = Path(args.output)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(json.dumps({
            "status": "PASS",
            "output": str(output_path),
            "checks": [check["name"] for check in evidence["checks"]],
        }, ensure_ascii=False))
        return 0
    except Exception as exc:
        evidence["status"] = "FAIL"
        evidence["error"] = str(exc)
        output_path = Path(args.output)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(json.dumps({"status": "FAIL", "output": str(output_path), "error": str(exc)}, ensure_ascii=False), file=sys.stderr)
        try:
            if created_reason_id is not None:
                delete_loss_reason(base_url, tenant_id, leader_a["token"], created_reason_id)
        except Exception:
            pass
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
