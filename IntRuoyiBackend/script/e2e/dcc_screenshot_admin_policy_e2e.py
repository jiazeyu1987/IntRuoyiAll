from __future__ import annotations

import csv
import json
import os
import re
import subprocess
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable

from playwright.sync_api import Page, TimeoutError as PlaywrightTimeoutError, expect, sync_playwright

from script.e2e.dcc_screenshot_e2e_helpers import (
    APPLICANT,
    COMMON_USER,
    FRONTEND_BASE_URL,
    TENANT_ID,
    assert_services_ready,
    clear_permission_cache,
    e2e_tmp_dir,
    ensure_e2e_baseline,
    login,
    mysql_rows,
    mysql_scalar,
    run_mysql,
    unique_code,
)


T4_PREFIX = "CODEX_E2E_T4_"
E2E_CATEGORY_ID = 906101
E2E_DIRECTORY_ID = 906200
E2E_DIRECTORY_PATH = "DCC E2E Documents"
E2E_DEPARTMENT_ID = 111
MINIO_CONTAINER = os.environ.get("DCC_E2E_MINIO_CONTAINER", "docker-minio-1")
MINIO_ENDPOINT = os.environ.get("DCC_E2E_MINIO_ENDPOINT", "http://host.docker.internal:9000")
MINIO_BUCKET = os.environ.get("DCC_E2E_MINIO_BUCKET", "yudao")
MINIO_CLIENT_IMAGE = os.environ.get("DCC_E2E_MINIO_CLIENT_IMAGE", "minio/mc")
WEAK_PASSWORD = "12345678"
RESET_USER_ID = 910241
RESET_USERNAME = "codexe2ereset"
EXPIRED_USER_ID = 910242
EXPIRED_USERNAME = "codexe2eexpired"
PASSWORD_POLICY_MESSAGE = "密码至少 8 位且必须包含英文和数字"
MINIMAL_DCC_FIXTURE_PDF = (
    b"%PDF-1.4\n"
    b"1 0 obj\n"
    b"<< /Type /Catalog /Pages 2 0 R >>\n"
    b"endobj\n"
    b"2 0 obj\n"
    b"<< /Type /Pages /Count 0 >>\n"
    b"endobj\n"
    b"trailer\n"
    b"<< /Root 1 0 R >>\n"
    b"%%EOF\n"
)


class DccT4Blocker(AssertionError):
    """Raised when a real product entry point is absent, so the AC cannot be truthfully passed."""


@dataclass(frozen=True)
class T4DistributionFixture:
    controlled_file_id: int
    master_id: int
    file_number: str
    file_name: str
    version_no: str
    electronic_distribution_id: int
    electronic_recipient_id: int
    paper_distribution_id: int


def _sql_literal(value: str | None) -> str:
    if value is None:
        return "NULL"
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def _bool_count(sql: str) -> bool:
    return int(mysql_scalar(sql) or "0") > 0


def _now_tag() -> str:
    return str(int(time.time() * 1000))


def _required_container_env(container_name: str, key: str) -> str:
    proc = subprocess.run(
        ["docker", "exec", container_name, "printenv", key],
        text=True,
        encoding="utf-8",
        capture_output=True,
        check=False,
    )
    value = proc.stdout.strip()
    if proc.returncode != 0 or not value:
        raise DccT4Blocker(f"local MinIO container {container_name} is missing required env {key}")
    return value


def _write_minio_fixture_object(file_name: str) -> None:
    if not re.fullmatch(r"[A-Za-z0-9_.-]+", file_name):
        raise DccT4Blocker(f"unsafe T4 fixture file name for MinIO object: {file_name}")

    fixture_dir = e2e_tmp_dir() / "minio-fixtures"
    fixture_dir.mkdir(parents=True, exist_ok=True)
    fixture_path = fixture_dir / file_name
    fixture_path.write_bytes(MINIMAL_DCC_FIXTURE_PDF)

    access_key = _required_container_env(MINIO_CONTAINER, "MINIO_ROOT_USER")
    secret_key = _required_container_env(MINIO_CONTAINER, "MINIO_ROOT_PASSWORD")
    target = f"dst/{MINIO_BUCKET}/codex-e2e/{file_name}"
    proc = subprocess.run(
        [
            "docker",
            "run",
            "--rm",
            "--add-host",
            "host.docker.internal:host-gateway",
            "-v",
            f"{fixture_path}:/fixture.pdf:ro",
            "-e",
            f"MINIO_ENDPOINT={MINIO_ENDPOINT}",
            "-e",
            f"MINIO_ACCESS_KEY={access_key}",
            "-e",
            f"MINIO_SECRET_KEY={secret_key}",
            "-e",
            f"MINIO_TARGET={target}",
            "--entrypoint",
            "/bin/sh",
            MINIO_CLIENT_IMAGE,
            "-c",
            'mc alias set dst "$MINIO_ENDPOINT" "$MINIO_ACCESS_KEY" "$MINIO_SECRET_KEY" >/dev/null '
            '&& mc cp /fixture.pdf "$MINIO_TARGET" >/dev/null',
        ],
        text=True,
        encoding="utf-8",
        capture_output=True,
        check=False,
    )
    if proc.returncode != 0:
        raise DccT4Blocker(
            "failed to upload T4 fixture object to local MinIO\n"
            f"target={MINIO_BUCKET}/codex-e2e/{file_name}\n"
            f"stdout={proc.stdout}\n"
            f"stderr={proc.stderr}"
        )


def _headless() -> bool:
    return os.environ.get("DCC_E2E_HEADLESS", "1") != "0" and os.environ.get("DCC_E2E_HEADED") != "1"


def _open_page(context: Any) -> Page:
    page = context.new_page()
    page.set_default_timeout(20_000)
    return page


def _wait_quiet(page: Page, timeout: int = 15_000) -> None:
    try:
        page.wait_for_load_state("networkidle", timeout=timeout)
    except PlaywrightTimeoutError:
        pass
    page.wait_for_timeout(500)


def _body_text(page: Page) -> str:
    try:
        text = page.locator("body").inner_text(timeout=10_000)
    except PlaywrightTimeoutError:
        return ""
    return re.sub(r"\s+", " ", text).strip()


def _form_item(scope: Page | Any, label: str) -> Any:
    item = scope.locator(
        "xpath=.//label[normalize-space()="
        f"{json.dumps(label, ensure_ascii=False)}]"
        "/ancestor::*[contains(concat(' ', normalize-space(@class), ' '), ' el-form-item ')][1]"
    ).first
    item.wait_for(state="visible", timeout=15_000)
    return item


def _fill_form_input(scope: Page | Any, label: str, value: str) -> None:
    _form_item(scope, label).locator("input").first.fill(value)


def _fill_form_textarea(scope: Page | Any, label: str, value: str) -> None:
    _form_item(scope, label).locator("textarea").first.fill(value)


def _click_confirm_message_box(page: Page, label: str = "确定|确认") -> None:
    box = page.locator(".el-message-box:visible").last
    box.wait_for(state="visible", timeout=10_000)
    box.get_by_role("button", name=re.compile(label)).click()
    _wait_quiet(page, 10_000)


def _eventually(fn: Callable[[], Any], *, timeout: float = 20.0, interval: float = 0.5) -> Any:
    deadline = time.time() + timeout
    last_exc: BaseException | None = None
    while time.time() < deadline:
        try:
            return fn()
        except AssertionError as exc:
            last_exc = exc
            time.sleep(interval)
    if last_exc is not None:
        raise last_exc
    raise AssertionError("timed out waiting for condition")


def _reset_t4_runtime_users() -> None:
    password_hash = mysql_scalar(
        f"""
SELECT password
FROM system_users
WHERE tenant_id={TENANT_ID}
  AND deleted=b'0'
  AND id={APPLICANT.user_id};
"""
    )
    if not password_hash:
        raise AssertionError("T4 precondition failed: applicant user 113 is missing in tenant 122")

    run_mysql(
        f"""
INSERT INTO system_users
  (id, username, password, password_update_time, nickname, remark, dept_id, post_ids, email,
   mobile, sex, avatar, status, login_ip, login_date, creator, create_time, updater, update_time,
   deleted, tenant_id)
VALUES
  ({RESET_USER_ID}, {_sql_literal(RESET_USERNAME)}, {_sql_literal(password_hash)}, NOW(),
   'CODEX_E2E重置密码', 'CODEX_E2E T4 reset target', NULL, NULL, '', '', 0, '', 0, '',
   NULL, 'codex-e2e-t4', NOW(), 'codex-e2e-t4', NOW(), b'0', {TENANT_ID}),
  ({EXPIRED_USER_ID}, {_sql_literal(EXPIRED_USERNAME)}, {_sql_literal(password_hash)},
   DATE_SUB(NOW(), INTERVAL 91 DAY), 'CODEX_E2E过期密码', 'CODEX_E2E T4 expired login target',
   NULL, NULL, '', '', 0, '', 0, '', NULL, 'codex-e2e-t4', NOW(), 'codex-e2e-t4',
   NOW(), b'0', {TENANT_ID})
ON DUPLICATE KEY UPDATE
  password=VALUES(password),
  nickname=VALUES(nickname),
  remark=VALUES(remark),
  status=0,
  deleted=b'0',
  password_update_time=VALUES(password_update_time),
  updater='codex-e2e-t4',
  update_time=NOW();

DELETE FROM system_oauth2_access_token
WHERE tenant_id={TENANT_ID}
  AND user_id IN ({RESET_USER_ID}, {EXPIRED_USER_ID});
""",
        batch=False,
    )


def _ensure_t4_baseline() -> None:
    assert_services_ready()
    ensure_e2e_baseline()
    _reset_t4_runtime_users()
    clear_permission_cache()


def _insert_infra_file(file_id: int, file_name: str) -> None:
    _write_minio_fixture_object(file_name)
    file_size = len(MINIMAL_DCC_FIXTURE_PDF)
    run_mysql(
        f"""
INSERT INTO infra_file
  (id, config_id, name, path, url, type, size, creator, create_time, updater, update_time, deleted)
VALUES
  ({file_id}, 0, {_sql_literal(file_name)}, {_sql_literal('/codex-e2e/' + file_name)},
   {_sql_literal('http://127.0.0.1/codex-e2e/' + file_name)}, 'application/pdf', {file_size},
   'codex-e2e-t4', NOW(), 'codex-e2e-t4', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  name=VALUES(name),
  path=VALUES(path),
  url=VALUES(url),
  type=VALUES(type),
  size=VALUES(size),
  updater='codex-e2e-t4',
  update_time=NOW(),
  deleted=b'0';
""",
        batch=False,
    )


def _create_distribution_fixture(scenario: str) -> T4DistributionFixture:
    tag = _now_tag()
    file_name = f"{T4_PREFIX}{scenario}_{tag}.pdf"
    file_number = f"CODEX-E2E-T4-{scenario}-{tag[-8:]}"
    version_no = "V1.0"
    original_file_id = int(f"91{tag[-10:]}0")
    published_file_id = original_file_id + 1
    stamped_file_id = original_file_id + 2

    for file_id, suffix in (
        (original_file_id, "original"),
        (published_file_id, "published"),
        (stamped_file_id, "stamped"),
    ):
        _insert_infra_file(file_id, f"{file_name}.{suffix}.pdf")

    run_mysql(
        f"""
INSERT INTO dcc_controlled_file_master
  (category_id, file_name, file_number, current_active_controlled_file_id, status,
   tenant_id, create_time, update_time, creator, updater, deleted)
VALUES
  ({E2E_CATEGORY_ID}, {_sql_literal(file_name)}, {_sql_literal(file_number)}, NULL, 'ACTIVE_CHAIN',
   {TENANT_ID}, NOW(), NOW(), 'codex-e2e-t4', 'codex-e2e-t4', 0);
""",
        batch=False,
    )
    master_id = int(
        mysql_scalar(
            f"""
SELECT id
FROM dcc_controlled_file_master
WHERE tenant_id={TENANT_ID}
  AND deleted=0
  AND file_number={_sql_literal(file_number)}
ORDER BY id DESC
LIMIT 1;
"""
        )
        or "0"
    )
    if master_id <= 0:
        raise AssertionError(f"failed to create DCC master for {file_number}")

    run_mysql(
        f"""
INSERT INTO dcc_controlled_file
  (master_id, category_id, binding_directory_id, directory_id, binding_directory_path,
   submit_directory_path, source_file_id, original_file_id, drawing_pdf_file_id,
   training_record_file_id, published_file_id, stamped_file_id, file_name, title, file_number,
   product_code, need_training, process_type, version_no, effective_date, remark, status,
   submitter_id, requester_id, process_instance_id, process_definition_key, submitted_time,
   approved_time, published_time, rejected_time, stamped_time, obsoleted_by, obsoleted_time,
   obsolete_reason, superseded_by_file_id, reject_reason, finalization_error, tenant_id,
   create_time, update_time, creator, updater, deleted)
VALUES
  ({master_id}, {E2E_CATEGORY_ID}, {E2E_DIRECTORY_ID}, {E2E_DIRECTORY_ID},
   {_sql_literal(E2E_DIRECTORY_PATH)}, {_sql_literal(E2E_DIRECTORY_PATH)}, {original_file_id},
   {original_file_id}, NULL, NULL, {published_file_id}, {stamped_file_id}, {_sql_literal(file_name)},
   {_sql_literal(file_name)}, {_sql_literal(file_number)}, 'CODEX-E2E', b'0', 'CONTROLLED_FILE',
   {_sql_literal(version_no)}, CURDATE(), {_sql_literal('CODEX_E2E T4 ' + scenario)}, 'ACTIVE', {APPLICANT.user_id},
   {APPLICANT.user_id}, NULL, 'dcc-controlled-file-approval', NOW(), NOW(), NOW(), NULL, NOW(),
   NULL, NULL, NULL, NULL, NULL, NULL, {TENANT_ID}, NOW(), NOW(), 'codex-e2e-t4',
   'codex-e2e-t4', 0);
""",
        batch=False,
    )
    controlled_file_id = int(
        mysql_scalar(
            f"""
SELECT id
FROM dcc_controlled_file
WHERE tenant_id={TENANT_ID}
  AND deleted=0
  AND master_id={master_id}
ORDER BY id DESC
LIMIT 1;
"""
        )
        or "0"
    )
    if controlled_file_id <= 0:
        raise AssertionError(f"failed to create DCC controlled file for {file_number}")

    run_mysql(
        f"""
UPDATE dcc_controlled_file_master
SET current_active_controlled_file_id={controlled_file_id},
    update_time=NOW(),
    updater='codex-e2e-t4'
WHERE id={master_id}
  AND tenant_id={TENANT_ID};

INSERT INTO dcc_controlled_file_distribution
  (controlled_file_id, department_id, distribution_medium, status,
   acknowledged_by, acknowledged_at, recovered_by, recovered_at, tenant_id,
   create_time, update_time, creator, updater, deleted)
VALUES
  ({controlled_file_id}, {E2E_DEPARTMENT_ID}, 'PUBLIC_FOLDER',
   'PENDING', NULL, NULL, NULL, NULL, {TENANT_ID}, NOW(), NOW(), 'codex-e2e-t4',
   'codex-e2e-t4', 0),
  ({controlled_file_id}, {E2E_DEPARTMENT_ID}, 'PAPER',
   'PENDING', NULL, NULL, NULL, NULL, {TENANT_ID}, NOW(), NOW(), 'codex-e2e-t4',
   'codex-e2e-t4', 0);
""",
        batch=False,
    )

    electronic_distribution_id = int(
        mysql_scalar(
            f"""
SELECT id
FROM dcc_controlled_file_distribution
WHERE tenant_id={TENANT_ID}
  AND deleted=0
  AND controlled_file_id={controlled_file_id}
  AND distribution_medium='PUBLIC_FOLDER'
ORDER BY id DESC
LIMIT 1;
"""
        )
        or "0"
    )
    paper_distribution_id = int(
        mysql_scalar(
            f"""
SELECT id
FROM dcc_controlled_file_distribution
WHERE tenant_id={TENANT_ID}
  AND deleted=0
  AND controlled_file_id={controlled_file_id}
  AND distribution_medium='PAPER'
ORDER BY id DESC
LIMIT 1;
"""
        )
        or "0"
    )
    if electronic_distribution_id <= 0 or paper_distribution_id <= 0:
        raise AssertionError(f"failed to create distribution rows for {file_number}")

    run_mysql(
        f"""
INSERT INTO dcc_controlled_file_distribution_recipient
  (distribution_id, user_id, message_job_id, read_at, acknowledged_at, ack_comment,
   tenant_id, create_time, update_time, creator, updater, deleted)
VALUES
  ({electronic_distribution_id}, {COMMON_USER.user_id}, NULL, NULL, NULL, NULL,
   {TENANT_ID}, NOW(), NOW(), 'codex-e2e-t4', 'codex-e2e-t4', 0);
""",
        batch=False,
    )
    electronic_recipient_id = int(
        mysql_scalar(
            f"""
SELECT id
FROM dcc_controlled_file_distribution_recipient
WHERE tenant_id={TENANT_ID}
  AND deleted=0
  AND distribution_id={electronic_distribution_id}
  AND user_id={COMMON_USER.user_id}
ORDER BY id DESC
LIMIT 1;
"""
        )
        or "0"
    )
    if electronic_recipient_id <= 0:
        raise AssertionError(f"failed to create distribution recipient for {file_number}")

    return T4DistributionFixture(
        controlled_file_id=controlled_file_id,
        master_id=master_id,
        file_number=file_number,
        file_name=file_name,
        version_no=version_no,
        electronic_distribution_id=electronic_distribution_id,
        electronic_recipient_id=electronic_recipient_id,
        paper_distribution_id=paper_distribution_id,
    )


def _open_dcc_detail(page: Page, file_id: int) -> None:
    page.goto(f"{FRONTEND_BASE_URL}/dcc/controlled-file/detail/{file_id}", wait_until="domcontentloaded")
    _wait_quiet(page, 30_000)
    expect(page.get_by_text("分发状态")).to_be_visible(timeout=30_000)


def _dialog_by_title(page: Page, title: str) -> Any:
    dialog = page.locator(".el-dialog").filter(has_text=re.compile(re.escape(title))).last
    dialog.wait_for(state="visible", timeout=15_000)
    return dialog


def run_e2e_11_electronic_distribution_receipt() -> dict[str, Any]:
    _ensure_t4_baseline()
    fixture = _create_distribution_fixture("E11")
    ack_comment = f"CODEX_E2E_ACK_{_now_tag()}"

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=_headless())
        context = browser.new_context(viewport={"width": 1440, "height": 1000}, accept_downloads=True)
        page = _open_page(context)
        login(page, COMMON_USER, f"/dcc/controlled-file/detail/{fixture.controlled_file_id}")
        _open_dcc_detail(page, fixture.controlled_file_id)
        expect(page.get_by_text(fixture.file_number, exact=True).first).to_be_visible(timeout=30_000)
        page.locator("button, .el-button").filter(has_text="确认签收").first.click()
        dialog = _dialog_by_title(page, "电子发放签收")
        _fill_form_input(dialog, "登录密码", COMMON_USER.password)
        _fill_form_textarea(dialog, "签收意见", ack_comment)

        with page.expect_response(
            lambda response: response.request.method == "POST"
            and f"/admin-api/dcc/controlled-files/{fixture.controlled_file_id}/distributions/"
            in response.url
            and "/acknowledge" in response.url,
            timeout=60_000,
        ) as response_info:
            dialog.get_by_role("button", name="确认签收").click()
        payload = response_info.value.json()
        if payload.get("code") != 0:
            raise AssertionError(f"E2E-11 acknowledge failed: {payload}")
        _wait_quiet(page, 20_000)
        context.close()
        browser.close()

    recipient_acknowledged = _eventually(
        lambda: _bool_count(
            f"""
SELECT COUNT(*) AS c
FROM dcc_controlled_file_distribution_recipient
WHERE tenant_id={TENANT_ID}
  AND deleted=0
  AND id={fixture.electronic_recipient_id}
  AND acknowledged_at IS NOT NULL;
"""
        )
        or (_raise_assertion("recipient row was not acknowledged")),
        timeout=20,
    )
    ack_comment_recorded = _bool_count(
        f"""
SELECT COUNT(*) AS c
FROM dcc_controlled_file_distribution_recipient
WHERE tenant_id={TENANT_ID}
  AND deleted=0
  AND id={fixture.electronic_recipient_id}
  AND ack_comment={_sql_literal(ack_comment)};
"""
    )
    signature_recorded = _bool_count(
        f"""
SELECT COUNT(*) AS c
FROM dcc_controlled_file_signature
WHERE tenant_id={TENANT_ID}
  AND deleted=0
  AND controlled_file_id={fixture.controlled_file_id}
  AND actor_id={COMMON_USER.user_id}
  AND action_type='DISTRIBUTION_ACK'
  AND comment={_sql_literal(ack_comment)};
"""
    )
    return {
        "fileId": fixture.controlled_file_id,
        "distributionId": fixture.electronic_distribution_id,
        "recipientId": fixture.electronic_recipient_id,
        "recipient_acknowledged": bool(recipient_acknowledged),
        "ack_comment_recorded": ack_comment_recorded,
        "signature_recorded": signature_recorded,
    }


def _raise_assertion(message: str) -> None:
    raise AssertionError(message)


def run_e2e_11_recipient_add_sign_gap_probe() -> dict[str, Any]:
    _ensure_t4_baseline()
    fixture = _create_distribution_fixture("E11GAP")
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=_headless())
        context = browser.new_context(viewport={"width": 1440, "height": 1000}, accept_downloads=True)
        page = _open_page(context)
        login(page, COMMON_USER, f"/dcc/controlled-file/detail/{fixture.controlled_file_id}")
        _open_dcc_detail(page, fixture.controlled_file_id)
        body = _body_text(page)
        add_sign_action_count = page.locator("button, .el-button").filter(
            has_text=re.compile("接收人加签|接收加签")
        ).count()
        context.close()
        browser.close()

    if add_sign_action_count == 0 and "接收人加签" not in body:
        raise DccT4Blocker(
            "E2E-11/AC-11 blocker: 当前真实 DCC 详情页只暴露电子发放“确认签收/签收意见”，"
            "未暴露“接收人加签”入口；不能伪造接收人加签通过。"
        )
    return {"recipient_add_sign_visible": True, "fileId": fixture.controlled_file_id}


def _paper_distribution_row(distribution_id: int) -> dict[str, str]:
    rows = mysql_rows(
        f"""
SELECT status,
       acknowledged_by,
       acknowledged_at,
       recovered_by,
       recovered_at
FROM dcc_controlled_file_distribution
WHERE tenant_id={TENANT_ID}
  AND deleted=0
  AND id={distribution_id};
"""
    )
    if not rows:
        raise AssertionError(f"paper distribution {distribution_id} not found")
    return rows[0]


def _csv_text(path: Path) -> str:
    raw = path.read_bytes()
    return raw.decode("utf-8-sig")


def _assert_receipt_fields(text: str, fixture: T4DistributionFixture) -> bool:
    required_markers = [
        "文件编号",
        "版本",
        "名称",
        "发放人",
        "接收人",
        "发放日期",
        "回收人",
        "回收日期",
        fixture.file_number,
        fixture.version_no,
        fixture.file_name,
        "芋道1",
        COMMON_USER.username,
    ]
    missing = [marker for marker in required_markers if marker not in text]
    if missing:
        raise AssertionError(f"receipt text is missing fields {missing}; text={text[:2000]}")
    return True


def _select_paper_distribution_recipient(page: Page, dialog: Any) -> None:
    _form_item(dialog, "纸质接收人").locator(".el-input").first.click()
    user_dialog = _dialog_by_title(page, "人员选择")
    _fill_form_input(user_dialog, "用户名称", COMMON_USER.username)
    user_dialog.get_by_role("button", name="搜索").click()
    row = user_dialog.locator(".el-table__body tr").filter(has_text=COMMON_USER.username).first
    row.wait_for(state="visible", timeout=15_000)
    row.locator(".el-checkbox__input").first.click()
    user_dialog.get_by_role("button", name=re.compile("确 定|确定")).click()
    user_dialog.wait_for(state="hidden", timeout=15_000)


def _is_paper_records_response(response: Any, file_id: int) -> bool:
    return (
        response.request.method == "GET"
        and f"/admin-api/dcc/controlled-files/{file_id}/paper-distributions/records" in response.url
    )


def _assert_paper_records_payload(payload: dict[str, Any], fixture: T4DistributionFixture) -> bool:
    if payload.get("code") != 0:
        raise AssertionError(f"paper records endpoint failed: {payload}")
    records = payload.get("data")
    if not isinstance(records, list):
        raise AssertionError(f"paper records endpoint did not return list data: {payload}")
    matching = [
        record for record in records
        if str(record.get("distributionId")) == str(fixture.paper_distribution_id)
    ]
    if not matching:
        raise AssertionError(f"paper records endpoint missing distribution {fixture.paper_distribution_id}: {payload}")
    record = matching[0]
    issuer_name = mysql_scalar(
        f"SELECT nickname FROM system_users WHERE tenant_id={TENANT_ID} AND deleted=b'0' "
        f"AND id={APPLICANT.user_id};"
    )
    recipient_name = mysql_scalar(
        f"SELECT nickname FROM system_users WHERE tenant_id={TENANT_ID} AND deleted=b'0' "
        f"AND id={COMMON_USER.user_id};"
    )
    if not issuer_name or not recipient_name:
        raise AssertionError("paper records E2E users are missing nicknames")
    expected_values = {
        "fileNumber": fixture.file_number,
        "versionNo": fixture.version_no,
        "fileName": fixture.file_name,
        "issuerName": issuer_name,
        "recovererName": issuer_name,
    }
    for key, expected in expected_values.items():
        if record.get(key) != expected:
            raise AssertionError(f"paper records {key} mismatch: expected={expected}; record={record}")
    if recipient_name not in (record.get("recipientNames") or []):
        raise AssertionError(f"paper records missing recipient name: {record}")
    if not record.get("issuedAt") or not record.get("recoveredAt"):
        raise AssertionError(f"paper records missing issue/recovery dates: {record}")
    return True


def run_e2e_12_paper_distribution_recovery_export_print() -> dict[str, Any]:
    _ensure_t4_baseline()
    fixture = _create_distribution_fixture("E12")

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=_headless())
        context = browser.new_context(viewport={"width": 1440, "height": 1000}, accept_downloads=True)
        page = _open_page(context)
        login(page, APPLICANT, f"/dcc/controlled-file/detail/{fixture.controlled_file_id}")
        with page.expect_response(
            lambda response: _is_paper_records_response(response, fixture.controlled_file_id),
            timeout=60_000,
        ) as initial_records_response:
            _open_dcc_detail(page, fixture.controlled_file_id)
        initial_records_payload = _response_payload(initial_records_response.value)
        if initial_records_payload.get("code") != 0:
            raise AssertionError(f"E2E-12 initial paper records load failed: {initial_records_payload}")

        with page.expect_response(
            lambda response: response.request.method == "POST"
            and f"/admin-api/dcc/controlled-files/{fixture.controlled_file_id}/paper-distributions/"
            in response.url
            and "/acknowledge" in response.url,
            timeout=60_000,
        ) as ack_response:
            page.locator("button, .el-button").filter(has_text="确认纸质发放").first.click()
            dialog = _dialog_by_title(page, "纸质发放登记")
            _select_paper_distribution_recipient(page, dialog)
            dialog.get_by_role("button", name="确认发放").click()
        ack_payload = ack_response.value.json()
        if ack_payload.get("code") != 0:
            raise AssertionError(f"E2E-12 paper acknowledge failed: {ack_payload}")

        def wait_ack() -> bool:
            row = _paper_distribution_row(fixture.paper_distribution_id)
            assert row["status"] == "ACKNOWLEDGED", row
            assert row["acknowledged_by"] == str(APPLICANT.user_id), row
            assert row["acknowledged_at"] != "NULL", row
            recipient_count = int(mysql_scalar(
                f"""
SELECT COUNT(*)
FROM dcc_controlled_file_distribution_recipient
WHERE tenant_id={TENANT_ID}
  AND deleted=0
  AND distribution_id={fixture.paper_distribution_id}
  AND user_id={COMMON_USER.user_id}
  AND message_job_id IS NULL
  AND read_at IS NULL
  AND acknowledged_at IS NULL;
"""
            ) or "0")
            assert recipient_count == 1, recipient_count
            return True

        _eventually(wait_ack, timeout=20)
        page.reload(wait_until="domcontentloaded")
        _wait_quiet(page, 20_000)

        with page.expect_response(
            lambda response: response.request.method == "POST"
            and f"/admin-api/dcc/controlled-files/{fixture.controlled_file_id}/paper-distributions/"
            in response.url
            and "/recover" in response.url,
            timeout=60_000,
        ) as recover_response:
            page.locator("button, .el-button").filter(has_text="确认回收").first.click()
            _click_confirm_message_box(page)
        recover_payload = recover_response.value.json()
        if recover_payload.get("code") != 0:
            raise AssertionError(f"E2E-12 paper recover failed: {recover_payload}")

        def wait_recover() -> bool:
            row = _paper_distribution_row(fixture.paper_distribution_id)
            assert row["status"] == "RECOVERED", row
            assert row["recovered_by"] == str(APPLICANT.user_id), row
            assert row["recovered_at"] != "NULL", row
            return True

        _eventually(wait_recover, timeout=20)
        with page.expect_response(
            lambda response: _is_paper_records_response(response, fixture.controlled_file_id),
            timeout=60_000,
        ) as recovered_records_response:
            page.reload(wait_until="domcontentloaded")
        _wait_quiet(page, 20_000)
        records_payload = _response_payload(recovered_records_response.value)
        records_endpoint_contains_required_fields = _assert_paper_records_payload(records_payload, fixture)

        download_dir = e2e_tmp_dir() / "downloads"
        download_dir.mkdir(parents=True, exist_ok=True)
        csv_path = download_dir / f"{fixture.file_number}.csv"
        with page.expect_download(timeout=30_000) as download_info:
            page.locator("button, .el-button").filter(has_text="导出回执").first.click()
        download_info.value.save_as(str(csv_path))
        csv_text = _csv_text(csv_path)
        csv_contains_required_fields = _assert_receipt_fields(csv_text, fixture)
        list(csv.reader(csv_text.splitlines()))

        with page.expect_popup(timeout=30_000) as popup_info:
            page.locator("button, .el-button").filter(has_text="打印回执").first.click()
        print_page = popup_info.value
        print_page.wait_for_load_state("domcontentloaded", timeout=15_000)
        print_text = _body_text(print_page)
        print_contains_required_fields = _assert_receipt_fields(print_text, fixture)

        context.close()
        browser.close()

    paper_row = _paper_distribution_row(fixture.paper_distribution_id)
    return {
        "fileId": fixture.controlled_file_id,
        "paperDistributionId": fixture.paper_distribution_id,
        "paper_acknowledged": paper_row["acknowledged_at"] != "NULL",
        "paper_recovered": paper_row["recovered_at"] != "NULL",
        "records_endpoint_contains_required_fields": records_endpoint_contains_required_fields,
        "csvPath": str(csv_path),
        "csv_contains_required_fields": csv_contains_required_fields,
        "print_contains_required_fields": print_contains_required_fields,
    }


def run_e2e_13_process_export_print_template_probe() -> dict[str, Any]:
    _ensure_t4_baseline()
    fixture = _create_distribution_fixture("E13")
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=_headless())
        context = browser.new_context(viewport={"width": 1440, "height": 1000}, accept_downloads=True)
        page = _open_page(context)
        login(page, APPLICANT, f"/dcc/controlled-file/detail/{fixture.controlled_file_id}")
        _open_dcc_detail(page, fixture.controlled_file_id)
        dcc_body = _body_text(page)
        dcc_has_fields = all(marker in dcc_body for marker in ("文件编号", "产品编号", fixture.file_number))
        dcc_export_print_count = page.locator("button, .el-button").filter(
            has_text=re.compile("流程.*导出|导出.*流程|流程.*打印|打印.*流程|Word|模板")
        ).count()

        page.goto(f"{FRONTEND_BASE_URL}/bpm/manager/model", wait_until="domcontentloaded")
        _wait_quiet(page, 20_000)
        bpm_body = _body_text(page)
        bpm_has_print_template = "自定义打印模板" in bpm_body or "打印模板" in bpm_body
        context.close()
        browser.close()

    if not dcc_has_fields:
        raise AssertionError("E2E-13 precondition failed: DCC detail page did not show DCC fields")
    if dcc_export_print_count == 0 or "Word" not in bpm_body:
        raise DccT4Blocker(
            "E2E-13/AC-13 blocker: 当前真实 DCC 详情页包含 DCC 字段，但未暴露可操作的"
            "“流程导出/流程打印/Word 模板缺占位符校验”入口；BPM 侧仅发现打印模板相关页面线索，"
            "无法通过真实前端验证 Word 模板缺占位符失败。"
        )
    return {
        "dcc_fields_visible": dcc_has_fields,
        "dcc_process_export_print_actions": dcc_export_print_count,
        "bpm_print_template_visible": bpm_has_print_template,
    }


def _response_payload(response: Any) -> dict[str, Any]:
    try:
        return response.json()
    except Exception as exc:  # noqa: BLE001 - fail with exact response body for diagnostics
        raise AssertionError(f"response is not JSON: status={response.status}; text={response.text()}") from exc


def _assert_password_policy_error(payload: dict[str, Any], action: str) -> bool:
    if payload.get("code") == 0:
        raise AssertionError(f"{action} unexpectedly accepted weak password: {payload}")
    msg = str(payload.get("msg") or payload.get("message") or "")
    if "密码强度不足" not in msg:
        raise AssertionError(f"{action} failed for unexpected reason: {payload}")
    return True


def _track_api_requests(page: Page, method: str, url_part: str) -> list[str]:
    requests: list[str] = []

    def on_request(request: Any) -> None:
        if request.method == method and url_part in request.url:
            requests.append(request.url)

    page.on("request", on_request)
    return requests


def _assert_frontend_password_policy_blocks(page: Page, requests: list[str], action: str) -> bool:
    expect(page.get_by_text(PASSWORD_POLICY_MESSAGE).first).to_be_visible(timeout=10_000)
    page.wait_for_timeout(1_000)
    if requests:
        raise AssertionError(
            f"{action} called backend even though frontend password policy should block weak input: "
            f"{json.dumps(requests, ensure_ascii=False)}"
        )
    return True


def _open_system_user_page(page: Page) -> None:
    page.goto(f"{FRONTEND_BASE_URL}/system/user", wait_until="domcontentloaded")
    _wait_quiet(page, 30_000)
    expect(page.get_by_text("用户名称").first).to_be_visible(timeout=30_000)


def _create_user_with_weak_password(page: Page) -> bool:
    username = ("codexe2e" + _now_tag()[-8:])[:30]
    before_count = int(
        mysql_scalar(
            f"""
SELECT COUNT(*) AS c
FROM system_users
WHERE tenant_id={TENANT_ID}
  AND deleted=b'0'
  AND username={_sql_literal(username)};
"""
        )
        or "0"
    )
    page.get_by_role("button", name=re.compile("新增")).first.click()
    dialog = page.get_by_role("dialog").last
    dialog.wait_for(state="visible", timeout=15_000)
    _fill_form_input(dialog, "用户昵称", "CODEX_E2E弱密码新增")
    _fill_form_input(dialog, "用户名称", username)
    _fill_form_input(dialog, "用户密码", WEAK_PASSWORD)
    requests = _track_api_requests(page, "POST", "/admin-api/system/user/create")
    dialog.get_by_role("button", name=re.compile(r"确\s*定")).click()
    result = _assert_frontend_password_policy_blocks(page, requests, "create user")
    after_count = int(
        mysql_scalar(
            f"""
SELECT COUNT(*) AS c
FROM system_users
WHERE tenant_id={TENANT_ID}
  AND deleted=b'0'
  AND username={_sql_literal(username)};
"""
        )
        or "0"
    )
    if after_count != before_count:
        raise AssertionError(f"weak-password user was persisted unexpectedly: {username}")
    return result


def _query_user_row(page: Page, username: str) -> Any:
    _open_system_user_page(page)
    _fill_form_input(page, "用户名称", username)
    page.get_by_role("button", name=re.compile("搜索")).first.click()
    _wait_quiet(page, 20_000)
    row = page.locator(".el-table__row").filter(has_text=username).first
    row.wait_for(state="visible", timeout=30_000)
    return row


def _reset_user_with_weak_password(page: Page) -> bool:
    before_hash = mysql_scalar(
        f"""
SELECT password
FROM system_users
WHERE tenant_id={TENANT_ID}
  AND deleted=b'0'
  AND id={RESET_USER_ID};
"""
    )
    row = _query_user_row(page, RESET_USERNAME)
    row.get_by_role("button", name=re.compile("更多")).click()
    page.get_by_role("menuitem").filter(has_text="重置密码").last.click()
    box = page.locator(".el-message-box:visible").last
    box.wait_for(state="visible", timeout=10_000)
    box.locator("input").first.fill(WEAK_PASSWORD)
    requests = _track_api_requests(page, "PUT", "/admin-api/system/user/update-password")
    box.get_by_role("button", name=re.compile("确定|确认")).click()
    result = _assert_frontend_password_policy_blocks(page, requests, "reset user password")
    after_hash = mysql_scalar(
        f"""
SELECT password
FROM system_users
WHERE tenant_id={TENANT_ID}
  AND deleted=b'0'
  AND id={RESET_USER_ID};
"""
    )
    if before_hash != after_hash:
        raise AssertionError("weak reset password changed target user's password hash")
    return result


def _change_profile_with_weak_password(page: Page) -> bool:
    before_hash = mysql_scalar(
        f"""
SELECT password
FROM system_users
WHERE tenant_id={TENANT_ID}
  AND deleted=b'0'
  AND id={APPLICANT.user_id};
"""
    )
    try:
        page.goto(f"{FRONTEND_BASE_URL}/user/profile", wait_until="domcontentloaded")
        _wait_quiet(page, 20_000)
        page.get_by_role("tab", name=re.compile("修改密码|密码设置|重置密码")).click()
    except Exception as exc:  # noqa: BLE001 - converted to an explicit E2E blocker
        body = _body_text(page)
        raise DccT4Blocker(
            "E2E-14/AC-14 blocker: 个人中心改密真实前端入口未能加载或未暴露改密页签；"
            "当前 8089 页面只显示系统壳，无法通过真实前端验证个人修改弱密码失败。"
            f" url={page.url}; body={body[:500]!r}; error={exc}"
        ) from exc
    form = page.locator("form").filter(has_text=re.compile("旧密码|新密码|确认密码")).first
    form.wait_for(state="visible", timeout=15_000)
    _fill_form_input(form, "旧密码", APPLICANT.password)
    _fill_form_input(form, "新密码", WEAK_PASSWORD)
    _fill_form_input(form, "确认密码", WEAK_PASSWORD)
    requests = _track_api_requests(page, "PUT", "/admin-api/system/user/profile/update-password")
    form.get_by_role("button", name=re.compile("保存")).click()
    result = _assert_frontend_password_policy_blocks(page, requests, "profile password change")
    after_hash = mysql_scalar(
        f"""
SELECT password
FROM system_users
WHERE tenant_id={TENANT_ID}
  AND deleted=b'0'
  AND id={APPLICANT.user_id};
"""
    )
    if before_hash != after_hash:
        raise AssertionError("weak profile password changed applicant password hash")
    return result


def _register_with_weak_password(page: Page) -> bool:
    page.goto(f"{FRONTEND_BASE_URL}/login?redirect=/index", wait_until="domcontentloaded")
    page.evaluate("() => { localStorage.clear(); sessionStorage.clear(); }")
    _wait_quiet(page, 10_000)
    page.get_by_role("button", name=re.compile("注册")).last.click()
    _wait_quiet(page, 10_000)
    tenant_inputs = page.locator('input[placeholder*="租户"]:visible')
    if tenant_inputs.count() > 0:
        tenant_inputs.first.fill("测试租户")
    page.locator('input[placeholder*="账号"]:visible, input[placeholder*="用户名"]:visible').first.fill(
        "codexe2eregister"
    )
    page.locator('input[placeholder*="昵称"]:visible').first.fill("CODEX_E2E注册弱密码")
    password_inputs = page.locator('input[type="password"]:visible')
    password_inputs.nth(0).fill(WEAK_PASSWORD)
    password_inputs.nth(1).fill(WEAK_PASSWORD)
    requests = _track_api_requests(page, "POST", "/admin-api/system/auth/register")
    page.get_by_role("button", name=re.compile("注册")).first.click()
    return _assert_frontend_password_policy_blocks(page, requests, "register")


def _forgot_password_with_weak_password(page: Page) -> bool:
    page.goto(f"{FRONTEND_BASE_URL}/login?redirect=/index", wait_until="domcontentloaded")
    page.evaluate("() => { localStorage.clear(); sessionStorage.clear(); }")
    _wait_quiet(page, 10_000)
    page.get_by_text("忘记密码").first.click()
    _wait_quiet(page, 10_000)
    tenant_inputs = page.locator('input[placeholder*="租户"]:visible')
    if tenant_inputs.count() > 0:
        tenant_inputs.first.fill("测试租户")
    page.locator('input[placeholder*="手机"]:visible').first.fill("13800138000")
    page.locator('input[placeholder*="验证码"]:visible').first.fill("123456")
    password_inputs = page.locator('input[type="password"]:visible')
    password_inputs.nth(0).fill(WEAK_PASSWORD)
    password_inputs.nth(1).fill(WEAK_PASSWORD)
    requests = _track_api_requests(page, "POST", "/admin-api/system/auth/reset-password")
    page.get_by_role("button", name=re.compile("重置密码")).first.click()
    return _assert_frontend_password_policy_blocks(page, requests, "forgot password")


def run_e2e_14_weak_password_policy() -> dict[str, Any]:
    _ensure_t4_baseline()
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=_headless())
        context = browser.new_context(viewport={"width": 1440, "height": 1000}, accept_downloads=True)
        page = _open_page(context)
        register_rejected = _register_with_weak_password(page)
        forgot_rejected = _forgot_password_with_weak_password(page)
        login(page, APPLICANT, "/system/user")
        create_rejected = _create_user_with_weak_password(page)
        reset_rejected = _reset_user_with_weak_password(page)
        profile_rejected = _change_profile_with_weak_password(page)
        context.close()
        browser.close()
    return {
        "register_weak_password_rejected": register_rejected,
        "forgot_weak_password_rejected": forgot_rejected,
        "create_weak_password_rejected": create_rejected,
        "reset_weak_password_rejected": reset_rejected,
        "profile_weak_password_rejected": profile_rejected,
        "weakPassword": WEAK_PASSWORD,
    }


def _attempt_login_expect_failure(page: Page, username: str, password: str) -> dict[str, Any]:
    page.goto(f"{FRONTEND_BASE_URL}/login?redirect=/dcc/controlled-file/browser", wait_until="domcontentloaded")
    page.evaluate("() => { localStorage.clear(); sessionStorage.clear(); }")
    _wait_quiet(page, 10_000)
    tenant_input = page.locator("input.el-select__input:visible").first
    tenant_input.click()
    page.keyboard.press("Control+A")
    page.keyboard.press("Backspace")
    tenant_input.fill("测试租户")
    page.wait_for_timeout(500)
    tenant_option = page.locator(".el-select-dropdown:visible .el-select-dropdown__item", has_text="测试租户")
    if tenant_option.count() > 0:
        tenant_option.last.click()
    else:
        page.keyboard.press("Enter")
    page.wait_for_timeout(500)
    page.get_by_placeholder("请输入用户名").fill(username)
    page.locator('input[placeholder="请输入密码"]:visible').fill(password)
    with page.expect_response(
        lambda response: response.request.method == "POST"
        and "/admin-api/system/auth/login" in response.url,
        timeout=60_000,
    ) as response_info:
        page.get_by_role("button", name="登录").first.click()
    payload = _response_payload(response_info.value)
    _wait_quiet(page, 10_000)
    return {"payload": payload, "url": page.url, "body": _body_text(page)}


def run_e2e_15_expired_password_login_policy() -> dict[str, Any]:
    _ensure_t4_baseline()
    run_mysql(
        f"""
UPDATE system_users
SET password_update_time=DATE_SUB(NOW(), INTERVAL 91 DAY),
    status=0,
    deleted=b'0',
    updater='codex-e2e-t4',
    update_time=NOW()
WHERE tenant_id={TENANT_ID}
  AND id={EXPIRED_USER_ID};

DELETE FROM system_oauth2_access_token
WHERE tenant_id={TENANT_ID}
  AND user_id={EXPIRED_USER_ID};
""",
        batch=False,
    )
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=_headless())
        context = browser.new_context(viewport={"width": 1440, "height": 1000}, accept_downloads=True)
        page = _open_page(context)
        attempt = _attempt_login_expect_failure(page, EXPIRED_USERNAME, APPLICANT.password)
        context.close()
        browser.close()

    payload = attempt["payload"]
    message = str(payload.get("msg") or payload.get("message") or "")
    login_rejected = payload.get("code") != 0 and "密码已过期" in message
    if not login_rejected:
        raise AssertionError(f"E2E-15 expired password login was not explicitly rejected: {attempt}")
    token_count = int(
        mysql_scalar(
            f"""
SELECT COUNT(*) AS c
FROM system_oauth2_access_token
WHERE tenant_id={TENANT_ID}
  AND deleted=b'0'
  AND user_id={EXPIRED_USER_ID};
"""
        )
        or "0"
    )
    business_page_not_reached = "/login" in attempt["url"] and token_count == 0
    if not business_page_not_reached:
        raise AssertionError(f"E2E-15 expired user reached business state: attempt={attempt}, tokenCount={token_count}")
    return {
        "login_rejected": login_rejected,
        "business_page_not_reached": business_page_not_reached,
        "message": message,
    }


def run_e2e_16_external_file_review_probe() -> dict[str, Any]:
    _ensure_t4_baseline()
    candidate_paths = [
        "/dcc/controlled-file/upload",
        "/dcc/controlled-file/external-review",
        "/dcc/controlled-file/external",
        "/dcc/external-file-review",
    ]
    seen: dict[str, str] = {}
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=_headless())
        context = browser.new_context(viewport={"width": 1440, "height": 1000}, accept_downloads=True)
        page = _open_page(context)
        login(page, APPLICANT, "/dcc/controlled-file/upload")
        for path in candidate_paths:
            page.goto(f"{FRONTEND_BASE_URL}{path}", wait_until="domcontentloaded")
            _wait_quiet(page, 15_000)
            seen[path] = _body_text(page)[:1000]
        body = "\n".join(seen.values())
        submit_review_count = page.locator("button, .el-button").filter(
            has_text=re.compile("外来文件评审|提交评审|审批|查看")
        ).count()
        context.close()
        browser.close()

    if "外来文件评审" not in body or submit_review_count == 0:
        raise DccT4Blocker(
            "E2E-16/AC-16 blocker: 当前真实 8089 前端未发现 DCC 外来文件评审提交/审批/查看入口；"
            "上传页仍是受控文件流程，不能伪造外来文件评审 E2E 通过。"
        )
    return {"external_review_entry_visible": True, "checkedPaths": candidate_paths}


def main() -> int:
    _ensure_t4_baseline()
    results = {
        "E2E-11": run_e2e_11_electronic_distribution_receipt(),
        "E2E-12": run_e2e_12_paper_distribution_recovery_export_print(),
        "E2E-14": run_e2e_14_weak_password_policy(),
        "E2E-15": run_e2e_15_expired_password_login_policy(),
    }
    print(json.dumps(results, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
