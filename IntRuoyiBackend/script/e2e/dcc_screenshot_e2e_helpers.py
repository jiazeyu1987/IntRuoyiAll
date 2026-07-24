from __future__ import annotations

import json
import os
import subprocess
import time
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable


TENANT_ID = 122
TENANT_NAME = "测试租户"
FRONTEND_BASE_URL = os.environ.get("DCC_E2E_FRONTEND_URL", "http://127.0.0.1:8089")
BACKEND_BASE_URL = os.environ.get("DCC_E2E_BACKEND_URL", "http://127.0.0.1:48089")
MYSQL_CONTAINER = os.environ.get("DCC_E2E_MYSQL_CONTAINER", "int-ruoyi-mysql")
MYSQL_DATABASE = os.environ.get("DCC_E2E_MYSQL_DATABASE", "ruoyi-vue-pro")
MYSQL_USER = os.environ.get("DCC_E2E_MYSQL_USER", "root")
MYSQL_PASSWORD = os.environ.get("DCC_E2E_MYSQL_PASSWORD", "123456")


@dataclass(frozen=True)
class E2EUser:
    username: str
    password: str
    user_id: int
    label: str


APPLICANT = E2EUser("aoteman", "admin123", 113, "申请人")
DOC_CONTROL = E2EUser("aoteman", "admin123", 113, "文控")
NODE_OWNER = E2EUser("aoteman", "admin123", 113, "节点负责人")
COMMON_USER = E2EUser("showroomviewer", "admin123", 910204, "普通用户")


class DccE2EError(RuntimeError):
    pass


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def e2e_tmp_dir() -> Path:
    tmp = repo_root() / "output" / "e2e" / "dcc-screenshot"
    tmp.mkdir(parents=True, exist_ok=True)
    return tmp


def assert_url_ready(url: str, label: str, timeout: float = 5.0) -> None:
    try:
        with urllib.request.urlopen(url, timeout=timeout) as response:
            if response.status >= 400:
                raise DccE2EError(f"{label} returned HTTP {response.status}: {url}")
    except Exception as exc:  # noqa: BLE001 - fail-fast wrapper for prerequisites
        raise DccE2EError(f"{label} is not reachable at {url}: {exc}") from exc


def assert_services_ready() -> None:
    assert_url_ready(f"{FRONTEND_BASE_URL}/", "frontend")
    assert_url_ready(f"{BACKEND_BASE_URL}/actuator/health", "backend")


def run_mysql(sql: str, *, batch: bool = True) -> str:
    args = [
        "docker",
        "exec",
        "-i",
        MYSQL_CONTAINER,
        "mysql",
        f"-u{MYSQL_USER}",
        f"-p{MYSQL_PASSWORD}",
        "--default-character-set=utf8mb4",
        "-D",
        MYSQL_DATABASE,
    ]
    if batch:
        args.extend(["--batch", "--raw"])
    proc = subprocess.run(
        args,
        input=sql,
        text=True,
        encoding="utf-8",
        capture_output=True,
        check=False,
    )
    if proc.returncode != 0:
        raise DccE2EError(
            "mysql command failed\n"
            f"sql:\n{sql}\n"
            f"stdout:\n{proc.stdout}\n"
            f"stderr:\n{proc.stderr}"
        )
    return proc.stdout


def mysql_rows(sql: str) -> list[dict[str, str]]:
    output = run_mysql(sql, batch=True).strip()
    if not output:
        return []
    lines = output.splitlines()
    headers = lines[0].split("\t")
    rows: list[dict[str, str]] = []
    for line in lines[1:]:
        values = line.split("\t")
        rows.append(dict(zip(headers, values)))
    return rows


def mysql_scalar(sql: str) -> str | None:
    rows = mysql_rows(sql)
    if not rows:
        return None
    return next(iter(rows[0].values()))


def clear_permission_cache() -> None:
    keys = [
        "permission_menu_ids:dcc:controlled-file:query",
        "permission_menu_ids:dcc:controlled-file:submit",
        "permission_menu_ids:dcc:controlled-file:download",
        "permission_menu_ids:dcc:controlled-file:review",
        "permission_menu_ids:dcc:controlled-file:approve",
        "user_role_ids:113",
        "user_role_ids:910204",
    ]
    subprocess.run(
        ["docker", "exec", "int-ruoyi-redis", "redis-cli", "DEL", *keys],
        text=True,
        encoding="utf-8",
        capture_output=True,
        check=False,
    )


def ensure_e2e_baseline() -> None:
    sql = """
SET @tenant_id := 122;
SET @user_id := 113;
SET @common_user_id := 910204;
SET @dir_id := 906200;
SET @dept_id := 906400;

INSERT INTO system_dept
  (id, name, parent_id, sort, leader_user_id, phone, email, status,
   tenant_id, create_time, update_time, creator, updater, deleted)
VALUES
  (@dept_id, 'DCC E2E Department', 111, 1, @user_id, NULL, NULL, 0,
   @tenant_id, NOW(), NOW(), 'codex-e2e', 'codex-e2e', 0)
ON DUPLICATE KEY UPDATE status=0, deleted=0, updater='codex-e2e', update_time=NOW();

UPDATE system_users
SET dept_id=@dept_id, updater='codex-e2e', update_time=NOW()
WHERE tenant_id=@tenant_id
  AND id IN (@user_id, @common_user_id)
  AND deleted=0;

INSERT INTO dcc_file_directory
  (id, parent_id, code, name, active, sort, remark, tenant_id, create_time, update_time, creator, updater, deleted)
VALUES
  (@dir_id, NULL, 'CODEX_E2E_DIR', 'DCC E2E Documents', 1, 1, 'Codex E2E directory', @tenant_id, NOW(), NOW(), 'codex-e2e', 'codex-e2e', 0)
ON DUPLICATE KEY UPDATE active=1, deleted=0, updater='codex-e2e', update_time=NOW();

INSERT INTO dcc_file_category
  (id, code, name, parent_id, active, sort, source, remark, description, distribution_required, training_required, tenant_id, create_time, update_time, creator, updater, deleted)
VALUES
  (906101, 'CODEX_E2E_SYSTEM', '体系文件', NULL, 1, 10, 'CODEX_E2E', 'Codex E2E', 'Codex E2E system category', 0, 0, @tenant_id, NOW(), NOW(), 'codex-e2e', 'codex-e2e', 0),
  (906102, 'CODEX_E2E_DHF', '技术文件-DHF', NULL, 1, 20, 'CODEX_E2E', 'Codex E2E', 'Codex E2E DHF category', 0, 0, @tenant_id, NOW(), NOW(), 'codex-e2e', 'codex-e2e', 0),
  (906103, 'CODEX_E2E_DMR', '技术文件-DMR', NULL, 1, 30, 'CODEX_E2E', 'Codex E2E', 'Codex E2E DMR category', 0, 0, @tenant_id, NOW(), NOW(), 'codex-e2e', 'codex-e2e', 0)
ON DUPLICATE KEY UPDATE active=1, deleted=0, updater='codex-e2e', update_time=NOW();

INSERT INTO dcc_category_directory_binding
  (id, category_id, directory_id, active, tenant_id, create_time, update_time, creator, updater, deleted)
VALUES
  (906201, 906101, @dir_id, 1, @tenant_id, NOW(), NOW(), 'codex-e2e', 'codex-e2e', 0),
  (906202, 906102, @dir_id, 1, @tenant_id, NOW(), NOW(), 'codex-e2e', 'codex-e2e', 0),
  (906203, 906103, @dir_id, 1, @tenant_id, NOW(), NOW(), 'codex-e2e', 'codex-e2e', 0)
ON DUPLICATE KEY UPDATE active=1, deleted=0, updater='codex-e2e', update_time=NOW();

INSERT INTO dcc_directory_access_rule
  (directory_id, subject_type, subject_id, can_query, can_preview, can_download, active, change_reason, tenant_id, create_time, update_time, creator, updater, deleted)
SELECT @dir_id, 'USER', subject_id, 1, 1, 1, 1, 'Codex E2E baseline', @tenant_id, NOW(), NOW(), 'codex-e2e', 'codex-e2e', 0
FROM (SELECT @user_id AS subject_id UNION ALL SELECT @common_user_id AS subject_id) seed
WHERE NOT EXISTS (
  SELECT 1 FROM dcc_directory_access_rule r
  WHERE r.tenant_id=@tenant_id AND r.directory_id=@dir_id AND r.subject_type='USER' AND r.subject_id=seed.subject_id AND r.deleted=0
);

INSERT INTO dcc_file_category_permission_rule
  (category_id, action_type, subject_type, subject_id, active, remark, tenant_id, create_time, update_time, creator, updater, deleted)
SELECT category_id, action_type, 'USER', @user_id, 1, 'Codex E2E baseline', @tenant_id, NOW(), NOW(), 'codex-e2e', 'codex-e2e', 0
FROM (
  SELECT 906101 AS category_id UNION ALL SELECT 906102 UNION ALL SELECT 906103
) categories
JOIN (
  SELECT 'VIEW' AS action_type UNION ALL SELECT 'UPLOAD' UNION ALL SELECT 'DOWNLOAD' UNION ALL
  SELECT 'REVIEW' UNION ALL SELECT 'APPROVE' UNION ALL SELECT 'DISTRIBUTE'
) actions
WHERE NOT EXISTS (
  SELECT 1 FROM dcc_file_category_permission_rule r
  WHERE r.tenant_id=@tenant_id AND r.category_id=categories.category_id AND r.action_type=actions.action_type
    AND r.subject_type='USER' AND r.subject_id=@user_id AND r.deleted=0
);

INSERT INTO dcc_file_category_permission_rule
  (category_id, action_type, subject_type, subject_id, active, remark, tenant_id, create_time, update_time, creator, updater, deleted)
SELECT 906101, action_type, 'USER', @common_user_id, 1, 'Codex E2E common download baseline', @tenant_id, NOW(), NOW(), 'codex-e2e', 'codex-e2e', 0
FROM (SELECT 'VIEW' AS action_type UNION ALL SELECT 'DOWNLOAD') actions
WHERE NOT EXISTS (
  SELECT 1 FROM dcc_file_category_permission_rule r
  WHERE r.tenant_id=@tenant_id AND r.category_id=906101 AND r.action_type=actions.action_type
    AND r.subject_type='USER' AND r.subject_id=@common_user_id AND r.deleted=0
);

INSERT INTO dcc_category_approval_route
  (id, category_id, version_no, active, effective_time, remark, tenant_id, create_time, update_time, creator, updater, deleted)
VALUES
  (906301, 906101, 1, 1, NOW(), 'Codex E2E four-stage route', @tenant_id, NOW(), NOW(), 'codex-e2e', 'codex-e2e', 0),
  (906302, 906102, 1, 1, NOW(), 'Codex E2E four-stage route', @tenant_id, NOW(), NOW(), 'codex-e2e', 'codex-e2e', 0),
  (906303, 906103, 1, 1, NOW(), 'Codex E2E four-stage route', @tenant_id, NOW(), NOW(), 'codex-e2e', 'codex-e2e', 0)
ON DUPLICATE KEY UPDATE active=1, deleted=0, updater='codex-e2e', update_time=NOW();

DELETE FROM dcc_category_approval_route_node WHERE route_id IN (906301, 906302, 906303) AND tenant_id=@tenant_id;
INSERT INTO dcc_category_approval_route_node
  (route_id, stage_no, stage_code, stage_name, stage_order, candidate_source_type, candidate_source_id, candidate_source_ids, approve_method, approve_ratio, require_all_approvals, required, sort, tenant_id, create_time, update_time, creator, updater, deleted)
SELECT route_id, stage_no, stage_code, stage_name, stage_no, 'USER', @user_id, CAST(@user_id AS CHAR), approve_method, approve_ratio, require_all, 1, stage_no, @tenant_id, NOW(), NOW(), 'codex-e2e', 'codex-e2e', 0
FROM (
  SELECT 906301 AS route_id UNION ALL SELECT 906302 UNION ALL SELECT 906303
) routes
JOIN (
  SELECT 1 AS stage_no, 'DOC_CONTROL_REVIEW' AS stage_code, '文控审核' AS stage_name, 'ANY' AS approve_method, NULL AS approve_ratio, 0 AS require_all UNION ALL
  SELECT 2, 'MATRIX_REVIEW', '审核会签', 'ALL', 100, 1 UNION ALL
  SELECT 3, 'MATRIX_APPROVAL', '批准', 'ANY', NULL, 0 UNION ALL
  SELECT 4, 'DOC_CONTROL_APPROVAL', '文控批准', 'ANY', NULL, 0
) nodes;

INSERT INTO dcc_electronic_signature_authorization
  (user_id, electronic_signature_enabled, tenant_id, create_time, update_time, creator, updater, deleted)
VALUES
  (@user_id, 1, @tenant_id, NOW(), NOW(), 'codex-e2e', 'codex-e2e', 0),
  (@common_user_id, 1, @tenant_id, NOW(), NOW(), 'codex-e2e', 'codex-e2e', 0)
ON DUPLICATE KEY UPDATE electronic_signature_enabled=1, deleted=0, updater='codex-e2e', update_time=NOW();
"""
    run_mysql(sql, batch=False)
    clear_permission_cache()


def login(page: Any, user: E2EUser, redirect: str = "/index") -> None:
    page.context.clear_cookies()
    page.goto(f"{FRONTEND_BASE_URL}/", wait_until="domcontentloaded")
    page.evaluate("() => { localStorage.clear(); sessionStorage.clear(); }")
    page.goto(f"{FRONTEND_BASE_URL}/login?redirect={redirect}", wait_until="networkidle")
    tenant_input = page.locator("input.el-select__input:visible").first
    tenant_input.click()
    page.keyboard.press("Control+A")
    page.keyboard.press("Backspace")
    tenant_input.fill(TENANT_NAME)
    page.wait_for_timeout(500)
    tenant_option = page.locator(".el-select-dropdown:visible .el-select-dropdown__item", has_text=TENANT_NAME)
    if tenant_option.count() > 0:
        tenant_option.last.click()
    else:
        page.keyboard.press("Enter")
    page.wait_for_timeout(1000)
    page.get_by_placeholder("请输入用户名").fill(user.username)
    page.locator('input[placeholder="请输入密码"]:visible').fill(user.password)
    page.get_by_role("button", name="登录").first.click()
    page.wait_for_function(
        """(expectedPath) => !location.pathname.includes('/login') && location.pathname === expectedPath""",
        arg=redirect,
        timeout=30000,
    )
    page.wait_for_load_state("networkidle")


def select_first_option_by_form_label(page: Any, label: str, option_text: str) -> None:
    item = page.locator(".el-form-item", has_text=label).first
    item.locator(".el-select").click()
    page.wait_for_timeout(500)
    page.get_by_text(option_text, exact=True).last.click()
    page.wait_for_timeout(1000)


def fill_form_input(page: Any, label: str, value: str) -> None:
    page.locator(".el-form-item", has_text=label).first.locator("input").first.fill(value)


def fill_form_textarea(page: Any, label: str, value: str) -> None:
    page.locator(".el-form-item", has_text=label).first.locator("textarea").first.fill(value)


def unique_code(prefix: str) -> str:
    return f"{prefix}{int(time.time() * 1000)}"


def collect_api_errors(page: Any) -> list[str]:
    errors: list[str] = []

    def on_response(response: Any) -> None:
        if "/admin-api/" not in response.url:
            return
        if response.status >= 400:
            errors.append(f"{response.status} {response.request.method} {response.url}")

    page.on("response", on_response)
    return errors


def collect_console_errors(page: Any) -> list[str]:
    errors: list[str] = []
    page.on("console", lambda msg: errors.append(msg.text) if msg.type == "error" else None)
    return errors


def assert_no_unexpected_browser_errors(api_errors: Iterable[str], console_errors: Iterable[str]) -> None:
    api = list(api_errors)
    console = list(console_errors)
    if api or console:
        raise AssertionError(
            "unexpected browser errors\n"
            f"api_errors={json.dumps(api, ensure_ascii=False)}\n"
            f"console_errors={json.dumps(console, ensure_ascii=False)}"
        )
