from __future__ import annotations

import json
import re
import time
from typing import Any

from playwright.sync_api import Page, sync_playwright

from script.e2e.dcc_screenshot_e2e_helpers import (
    APPLICANT,
    DccE2EError,
    FRONTEND_BASE_URL,
    assert_services_ready,
    ensure_e2e_baseline,
    login,
    mysql_rows,
)
from script.e2e.dcc_screenshot_workflow_actions_e2e import (
    _body_text,
    _ensure_runtime_files,
    _safe_sql,
    _submit_file,
    _wait_quiet,
)


class DccWithdrawnActionE2EError(AssertionError):
    pass


def _confirm_visible_dialog(page: Page, text: str) -> None:
    dialog = page.locator(".el-message-box, .el-dialog").filter(has_text=text).last
    dialog.wait_for(state="visible", timeout=15000)
    dialog.locator("button, .el-button").filter(has_text=re.compile("确认|确定")).last.click()


def _controlled_file_row_any(file_id: int) -> dict[str, str]:
    rows = mysql_rows(
        f"""
SELECT
  id,
  master_id,
  version_no,
  status,
  process_instance_id,
  requester_id,
  superseded_by_file_id,
  deleted + 0 AS deleted
FROM dcc_controlled_file
WHERE tenant_id=122 AND id={file_id};
"""
    )
    if not rows:
        raise DccE2EError(f"controlled file {file_id} was hard-deleted")
    return rows[0]


def _active_versions_for_master(master_id: str) -> list[dict[str, str]]:
    return mysql_rows(
        f"""
SELECT id, status, version_no, deleted + 0 AS deleted
FROM dcc_controlled_file
WHERE tenant_id=122
  AND master_id={int(master_id)}
  AND status='ACTIVE'
  AND deleted=0
ORDER BY id;
"""
    )


def _history_count(process_instance_id: str) -> int:
    rows = mysql_rows(
        f"""
SELECT COUNT(*) AS c
FROM act_hi_procinst
WHERE PROC_INST_ID_='{_safe_sql(process_instance_id)}';
"""
    )
    return int(rows[0]["c"]) if rows else 0


def _wait_for_status(file_id: int, status: str, timeout_seconds: int = 60) -> dict[str, str]:
    deadline = time.time() + timeout_seconds
    current = _controlled_file_row_any(file_id)
    while time.time() < deadline:
        current = _controlled_file_row_any(file_id)
        if current["status"] == status and str(current["deleted"]) == "0":
            return current
        time.sleep(1)
    raise DccE2EError(f"controlled file {file_id} did not reach status {status}: {current}")


def _withdraw_from_detail(page: Page, file_id: int) -> dict[str, str]:
    page.goto(
        f"{FRONTEND_BASE_URL}/dcc/controlled-file/detail/{file_id}",
        wait_until="domcontentloaded",
        timeout=30000,
    )
    _wait_quiet(page, 20000)
    with page.expect_response(
        lambda response: response.request.method == "POST"
        and f"/admin-api/dcc/controlled-files/{file_id}/withdraw" in response.url,
        timeout=60000,
    ):
        page.locator("button, .el-button").filter(has_text="撤回申请").first.click()
        _confirm_visible_dialog(page, "撤回")
    _wait_quiet(page, 20000)
    return _wait_for_status(file_id, "WITHDRAWN")


def _assert_withdrawn_actions_visible(page: Page, file_id: int) -> str:
    page.goto(
        f"{FRONTEND_BASE_URL}/dcc/controlled-file/detail/{file_id}",
        wait_until="domcontentloaded",
        timeout=30000,
    )
    _wait_quiet(page, 20000)
    body = _body_text(page)
    if "删除流程" not in body or "重新提交" not in body:
        raise DccWithdrawnActionE2EError(
            f"withdrawn detail actions are not visible for file {file_id}; body={body[:2000]}"
        )
    return body


def _assert_withdrawn_actions_hidden(page: Page, file_id: int) -> str:
    page.goto(
        f"{FRONTEND_BASE_URL}/dcc/controlled-file/detail/{file_id}",
        wait_until="domcontentloaded",
        timeout=30000,
    )
    _wait_quiet(page, 20000)
    body = _body_text(page)
    action_count = page.locator("button, .el-button").filter(
        has_text=re.compile("删除流程|重新提交")
    ).count()
    if action_count > 0:
        raise DccWithdrawnActionE2EError(
            f"processed withdrawn detail still exposes actions for file {file_id}; body={body[:2000]}"
        )
    return body


def _delete_withdrawn_flow(page: Page, file_id: int) -> dict[str, str]:
    with page.expect_response(
        lambda response: response.request.method == "DELETE"
        and f"/admin-api/dcc/controlled-files/{file_id}/withdrawn-flow" in response.url,
        timeout=60000,
    ) as response_info:
        page.locator("button, .el-button").filter(has_text="删除流程").first.click()
        _confirm_visible_dialog(page, "删除流程")
    payload = response_info.value.json()
    if response_info.value.status >= 400 or payload.get("code") != 0:
        raise DccE2EError(f"delete withdrawn flow failed: {payload}")
    _wait_quiet(page, 20000)
    row = _controlled_file_row_any(file_id)
    if str(row["deleted"]) != "1":
        raise DccE2EError(f"withdrawn file {file_id} was not soft-deleted: {row}")
    return row


def _resubmit_withdrawn_flow(page: Page, file_id: int) -> int:
    with page.expect_response(
        lambda response: response.request.method == "POST"
        and f"/admin-api/dcc/controlled-files/{file_id}/resubmit" in response.url,
        timeout=60000,
    ) as response_info:
        page.locator("button, .el-button").filter(has_text="重新提交").first.click()
        _confirm_visible_dialog(page, "重新提交")
    payload = response_info.value.json()
    if response_info.value.status >= 400 or payload.get("code") != 0 or not payload.get("data"):
        raise DccE2EError(f"resubmit withdrawn flow failed: {payload}")
    _wait_quiet(page, 20000)
    return int(payload["data"])


def _try_resubmit_processed_withdrawn_flow_by_api(page: Page, file_id: int) -> dict[str, Any]:
    return page.evaluate(
        """async (fileId) => {
          const readCacheValue = (key) => {
            const raw = localStorage.getItem(key)
            if (!raw) return ''
            try {
              const parsed = JSON.parse(raw)
              if (parsed && typeof parsed === 'object') {
                return parsed.v ?? parsed.value ?? parsed.data ?? ''
              }
              return parsed || ''
            } catch (error) {
              return raw
            }
          }
          const accessToken = readCacheValue('ACCESS_TOKEN')
          const tenantId = readCacheValue('tenantId')
          const headers = { 'Content-Type': 'application/json' }
          if (accessToken) headers.Authorization = `Bearer ${accessToken}`
          if (tenantId) headers['tenant-id'] = String(tenantId)
          const response = await fetch(`/admin-api/dcc/controlled-files/${fileId}/resubmit`, {
            method: 'POST',
            headers
          })
          const payload = await response.json().catch(() => ({}))
          return { status: response.status, payload }
        }""",
        file_id,
    )


def run_r05_withdrawn_actions_e2e() -> dict[str, Any]:
    assert_services_ready()
    ensure_e2e_baseline()
    assets = _ensure_runtime_files()
    result: dict[str, Any] = {
        "passed": [],
        "files": {},
        "checks": {},
    }

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1440, "height": 1000}, accept_downloads=True)
        try:
            page = context.new_page()
            login(page, APPLICANT, "/dcc/controlled-file/upload")

            delete_file = _submit_file(page, assets, scenario="R05DEL")
            withdrawn_delete = _withdraw_from_detail(page, delete_file.id)
            if _history_count(withdrawn_delete["process_instance_id"]) < 1:
                raise DccE2EError(f"withdrawn BPM history is missing: {withdrawn_delete}")
            before_active_versions = _active_versions_for_master(withdrawn_delete["master_id"])
            result["checks"]["deleteActionsBody"] = _assert_withdrawn_actions_visible(page, delete_file.id)
            deleted_row = _delete_withdrawn_flow(page, delete_file.id)
            after_active_versions = _active_versions_for_master(withdrawn_delete["master_id"])
            if before_active_versions != after_active_versions:
                raise DccE2EError(
                    f"delete withdrawn flow changed active versions: before={before_active_versions}, after={after_active_versions}"
                )
            if _history_count(withdrawn_delete["process_instance_id"]) < 1:
                raise DccE2EError(f"delete withdrawn flow removed BPM history: {withdrawn_delete}")
            result["files"]["delete"] = delete_file.__dict__
            result["checks"]["deleteRow"] = deleted_row
            result["passed"].append("R05-delete-withdrawn-flow")

            resubmit_file = _submit_file(page, assets, scenario="R05RESUB")
            withdrawn_resubmit = _withdraw_from_detail(page, resubmit_file.id)
            result["checks"]["resubmitActionsBody"] = _assert_withdrawn_actions_visible(page, resubmit_file.id)
            new_file_id = _resubmit_withdrawn_flow(page, resubmit_file.id)
            old_row = _controlled_file_row_any(resubmit_file.id)
            new_row = _wait_for_status(new_file_id, "PENDING_DOC_CONTROL_REVIEW")
            if str(old_row["deleted"]) != "0" or old_row["status"] != "WITHDRAWN":
                raise DccE2EError(f"resubmit changed old withdrawn record unexpectedly: {old_row}")
            if str(old_row.get("superseded_by_file_id")) != str(new_file_id):
                raise DccE2EError(f"resubmit did not mark old withdrawn record as processed: {old_row}")
            if new_file_id == resubmit_file.id:
                raise DccE2EError("resubmit reused the withdrawn DCC business record")
            if new_row["process_instance_id"] == withdrawn_resubmit["process_instance_id"]:
                raise DccE2EError("resubmit reused the withdrawn BPM process instance")
            if _history_count(withdrawn_resubmit["process_instance_id"]) < 1:
                raise DccE2EError(f"resubmit removed old BPM history: {withdrawn_resubmit}")
            result["checks"]["processedWithdrawnActionsBody"] = _assert_withdrawn_actions_hidden(
                page, resubmit_file.id
            )
            repeat_resubmit = _try_resubmit_processed_withdrawn_flow_by_api(page, resubmit_file.id)
            repeat_payload = repeat_resubmit.get("payload") or {}
            if repeat_resubmit.get("status", 0) < 400 and repeat_payload.get("code") == 0:
                raise DccE2EError(f"processed withdrawn record allowed repeat resubmit: {repeat_resubmit}")
            result["files"]["resubmit"] = resubmit_file.__dict__
            result["checks"]["resubmitOldRow"] = old_row
            result["checks"]["resubmitNewRow"] = new_row
            result["checks"]["repeatResubmit"] = repeat_resubmit
            result["passed"].append("R05-resubmit-withdrawn-flow")
        finally:
            context.close()
            browser.close()

    return result


def main() -> None:
    result = run_r05_withdrawn_actions_e2e()
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
