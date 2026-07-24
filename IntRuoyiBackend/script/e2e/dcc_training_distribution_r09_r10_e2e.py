from __future__ import annotations

import json
import re
import time
from typing import Any

from playwright.sync_api import Page, sync_playwright

from script.e2e.dcc_screenshot_e2e_helpers import (
    APPLICANT,
    COMMON_USER,
    DccE2EError,
    FRONTEND_BASE_URL,
    assert_services_ready,
    ensure_e2e_baseline,
    login,
    mysql_rows,
)
from script.e2e.dcc_screenshot_workflow_actions_e2e import (
    _approve_current_task,
    _body_text,
    _controlled_file_row,
    _current_action_dialog,
    _ensure_runtime_files,
    _form_item,
    _open_approval_task,
    _select_user_in_dialog,
    _submit_action_dialog,
    _submit_file,
    _upload_preview,
    _visible_dialog_with_text,
    _wait_for_file_status_change,
    _wait_quiet,
)


def _wait_for_distribution_recipient(file_id: int, user_id: int, timeout_seconds: int = 90) -> dict[str, str]:
    deadline = time.time() + timeout_seconds
    sql = f"""
SELECT distribution.id AS distribution_id,
       distribution.status AS distribution_status,
       recipient.id AS recipient_id,
       recipient.user_id,
       recipient.message_job_id,
       recipient.acknowledged_at
FROM dcc_controlled_file_distribution distribution
JOIN dcc_controlled_file_distribution_recipient recipient
  ON recipient.distribution_id = distribution.id
 AND recipient.deleted = 0
WHERE distribution.deleted = 0
  AND distribution.controlled_file_id = {file_id}
  AND distribution.distribution_medium = 'PUBLIC_FOLDER'
  AND recipient.user_id = {user_id}
ORDER BY recipient.id DESC
LIMIT 1
"""
    last_rows: list[dict[str, str]] = []
    while time.time() < deadline:
        last_rows = mysql_rows(sql)
        if last_rows and last_rows[0].get("message_job_id") not in (None, "", "NULL"):
            return last_rows[0]
        time.sleep(1)
    raise DccE2EError(f"distribution recipient was not dispatched for file {file_id}, user {user_id}: {last_rows}")


def _upload_applicant_training_record(page: Page, submitted_id: int, assets: dict[str, Any]) -> dict[str, str]:
    page.goto(
        f"{FRONTEND_BASE_URL}/dcc/controlled-file/detail/{submitted_id}",
        wait_until="domcontentloaded",
        timeout=30000,
    )
    _wait_quiet(page, 20000)
    body = _body_text(page)
    if "上传培训记录" not in body:
        raise DccE2EError(f"applicant training-record upload entry is not visible: {body[:2000]}")
    page.locator("button, .el-button").filter(has_text="上传培训记录").first.click()
    dialog = _visible_dialog_with_text(page, "上传培训记录")
    _upload_preview(page, dialog, "培训记录", assets["training"])
    with page.expect_response(
        lambda response: response.request.method == "POST"
        and f"/admin-api/dcc/controlled-files/{submitted_id}/training-record" in response.url,
        timeout=60000,
    ) as response_info:
        dialog.get_by_role("button", name="确认上传").click()
    response = response_info.value
    payload = response.json()
    if not response.ok or payload.get("code") != 0:
        raise DccE2EError(f"training-record upload failed: status={response.status}; payload={payload}")
    _wait_quiet(page, 20000)
    row = _controlled_file_row(submitted_id)
    if row["status"] != "PENDING_DOC_CONTROL_APPROVAL" or not row.get("training_record_file_id"):
        raise DccE2EError(f"training-record upload did not move to fourth node: {row}")
    return row


def _approve_fourth_node_with_recipient(page: Page, submitted_id: int, assets: dict[str, Any], submitted: Any) -> dict[str, Any]:
    _open_approval_task(page, submitted)
    page.locator("button, .el-button").filter(has_text=re.compile("批准通过|审核通过")).first.click()
    dialog = _current_action_dialog(page)
    dialog_text = re.sub(r"\s+", " ", dialog.inner_text()).strip()
    if "培训记录" in dialog_text:
        raise DccE2EError(f"fourth-node approval dialog still collects applicant training record: {dialog_text}")
    _upload_preview(page, dialog, "盖章 PDF", assets["stamp_pdf"])
    _select_user_in_dialog(page, dialog, "电子发放接收人", COMMON_USER.username, multiple=True)
    dialog.locator("input[type='password']").first.fill(APPLICANT.password)
    payload = _submit_action_dialog(page, "approve-task")
    final_row = _wait_for_file_status_change(submitted_id, "PENDING_DOC_CONTROL_APPROVAL")
    if final_row["status"] != "ACTIVE":
        raise DccE2EError(f"file did not activate after fourth-node approval: {final_row}")
    recipient_row = _wait_for_distribution_recipient(submitted_id, COMMON_USER.user_id)
    return {"payload": payload, "finalFile": final_row, "recipient": recipient_row}


def _recipient_adds_sign_and_acknowledges(page: Page, submitted_id: int) -> dict[str, Any]:
    login(page, COMMON_USER, f"/dcc/controlled-file/detail/{submitted_id}")
    _wait_quiet(page, 30000)
    body = _body_text(page)
    if "确认签收" not in body:
        raise DccE2EError(f"recipient receipt action is not visible: {body[:2000]}")
    if "接收人加签" not in body:
        raise DccE2EError(f"recipient sign action is not visible: {body[:2000]}")

    page.locator("button, .el-button").filter(has_text="接收人加签").first.click()
    sign_dialog = _visible_dialog_with_text(page, "接收人加签")
    _select_user_in_dialog(page, sign_dialog, "加签接收人", APPLICANT.username, multiple=True)
    _form_item(sign_dialog, "登录密码").locator("input[type='password']").first.fill(COMMON_USER.password)
    with page.expect_response(
        lambda response: response.request.method == "POST"
        and "/distributions/" in response.url
        and "/sign" in response.url,
        timeout=60000,
    ) as sign_response_info:
        sign_dialog.get_by_role("button", name="确认加签").click()
    sign_payload = sign_response_info.value.json()
    if sign_response_info.value.status >= 400 or sign_payload.get("code") != 0:
        raise DccE2EError(f"recipient sign failed: {sign_payload}")
    _wait_quiet(page, 20000)
    signed_row = _wait_for_distribution_recipient(submitted_id, APPLICANT.user_id)

    page.locator("button, .el-button").filter(has_text="确认签收").first.click()
    ack_dialog = _visible_dialog_with_text(page, "电子发放签收")
    _form_item(ack_dialog, "登录密码").locator("input[type='password']").first.fill(COMMON_USER.password)
    _form_item(ack_dialog, "签收意见").locator("textarea").first.fill("CODEX_E2E R10 receipt")
    with page.expect_response(
        lambda response: response.request.method == "POST"
        and "/distributions/" in response.url
        and "/acknowledge" in response.url,
        timeout=60000,
    ) as ack_response_info:
        ack_dialog.get_by_role("button", name="确认签收").click()
    ack_payload = ack_response_info.value.json()
    if ack_response_info.value.status >= 400 or ack_payload.get("code") != 0:
        raise DccE2EError(f"recipient acknowledgement failed: {ack_payload}")
    _wait_quiet(page, 20000)
    acknowledged_row = _wait_for_distribution_recipient(submitted_id, COMMON_USER.user_id)
    if acknowledged_row.get("acknowledged_at") in (None, "", "NULL"):
        raise DccE2EError(f"recipient acknowledgement was not persisted: {acknowledged_row}")

    return {
        "signPayload": sign_payload,
        "ackPayload": ack_payload,
        "signedRecipient": signed_row,
        "acknowledgedRecipient": acknowledged_row,
    }


def run_r09_r10_training_distribution_e2e() -> dict[str, Any]:
    assert_services_ready()
    ensure_e2e_baseline()
    assets = _ensure_runtime_files()
    result: dict[str, Any] = {"passed": [], "checks": {}, "files": {}}

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1440, "height": 1000}, accept_downloads=True)
        page = context.new_page()
        login(page, APPLICANT, "/dcc/controlled-file/upload")

        submitted = _submit_file(page, assets, scenario="R09R10", need_training=True)
        for _ in range(3):
            _approve_current_task(page, submitted)
        gated_row = _controlled_file_row(submitted.id)
        if gated_row["status"] != "PENDING_APPLICANT_TRAINING_RECORD" or gated_row.get("training_record_file_id") not in (None, "", "NULL"):
            raise DccE2EError(f"file did not enter applicant training-record gate: {gated_row}")
        result["files"]["submitted"] = submitted.__dict__
        result["checks"]["trainingGate"] = gated_row

        result["checks"]["trainingUpload"] = _upload_applicant_training_record(page, submitted.id, assets)
        result["checks"]["fourthApproval"] = _approve_fourth_node_with_recipient(page, submitted.id, assets, submitted)
        result["checks"]["recipientReceipt"] = _recipient_adds_sign_and_acknowledges(page, submitted.id)
        result["passed"].extend(["R09", "R10"])

        context.close()
        browser.close()

    return result


def main() -> None:
    result = run_r09_r10_training_distribution_e2e()
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
