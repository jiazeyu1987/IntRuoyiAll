from __future__ import annotations

import json
import re
import time
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from playwright.sync_api import Page, TimeoutError as PlaywrightTimeoutError, sync_playwright

from script.e2e.dcc_screenshot_e2e_helpers import (
    APPLICANT,
    COMMON_USER,
    DccE2EError,
    FRONTEND_BASE_URL,
    assert_services_ready,
    e2e_tmp_dir,
    ensure_e2e_baseline,
    login,
    mysql_rows,
    run_mysql,
)


PENDING_STATUS_TO_STAGE = {
    "PENDING_DOC_CONTROL_REVIEW": "DOC_CONTROL_REVIEW",
    "PENDING_MATRIX_REVIEW": "MATRIX_REVIEW",
    "PENDING_MATRIX_APPROVAL": "MATRIX_APPROVAL",
    "PENDING_APPLICANT_TRAINING_RECORD": "DOC_CONTROL_APPROVAL",
    "PENDING_DOC_CONTROL_APPROVAL": "DOC_CONTROL_APPROVAL",
}


@dataclass(frozen=True)
class SubmittedFile:
    id: int
    file_name: str
    file_number: str
    product_code: str
    need_training: bool


class DccWorkflowE2EBlocker(AssertionError):
    pass


def _safe_sql(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "\\'")


def _now_tag() -> str:
    return str(int(time.time() * 1000))


def _product_code(tag: str) -> str:
    return f"C{tag[-13:]:0>13}"[-14:]


def _wait_quiet(page: Page, timeout: int = 15000) -> None:
    try:
        page.wait_for_load_state("networkidle", timeout=timeout)
    except PlaywrightTimeoutError:
        pass
    page.wait_for_timeout(600)


def _body_text(page: Page) -> str:
    text = page.locator("body").inner_text(timeout=10000)
    return re.sub(r"\s+", " ", text).strip()


def _visible_messages(page: Page) -> list[str]:
    return page.locator(".el-message").evaluate_all(
        "(nodes) => nodes.map((node) => (node.textContent || '').replace(/\\s+/g, ' ').trim())"
    )


def _ensure_runtime_files() -> dict[str, Path]:
    tmp = e2e_tmp_dir()
    source_docx_path = tmp / "codex-e2e-source.docx"
    pdf_path = tmp / "codex-e2e-source.pdf"
    stamp_pdf_path = tmp / "codex-e2e-stamped.pdf"
    text_path = tmp / "codex-e2e-not-pdf.txt"
    training_path = tmp / "codex-e2e-training-record.txt"
    pdf_bytes = (
        b"%PDF-1.4\n"
        b"1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
        b"2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj\n"
        b"3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 200 120]/Contents 4 0 R>>endobj\n"
        b"4 0 obj<</Length 44>>stream\nBT /F1 12 Tf 20 70 Td (CODEX E2E PDF) Tj ET\nendstream endobj\n"
        b"xref\n0 5\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n"
        b"0000000115 00000 n \n0000000201 00000 n \ntrailer<</Size 5/Root 1 0 R>>\nstartxref\n295\n%%EOF\n"
    )
    with zipfile.ZipFile(source_docx_path, "w", zipfile.ZIP_DEFLATED) as docx:
        docx.writestr(
            "[Content_Types].xml",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""",
        )
        docx.writestr(
            "_rels/.rels",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""",
        )
        docx.writestr(
            "word/document.xml",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body><w:p><w:r><w:t>CODEX E2E SOURCE DOCX</w:t></w:r></w:p></w:body>
</w:document>""",
        )
    for path in (pdf_path, stamp_pdf_path):
        path.write_bytes(pdf_bytes)
    text_path.write_text("CODEX_E2E_NOT_PDF", encoding="utf-8")
    training_path.write_text("CODEX_E2E_TRAINING_RECORD", encoding="utf-8")
    return {
        "source_docx": source_docx_path,
        "pdf": pdf_path,
        "stamp_pdf": stamp_pdf_path,
        "text": text_path,
        "training": training_path,
    }


def _form_item(scope: Page | Any, label: str) -> Any:
    form_items = scope.locator(".el-form-item")
    deadline = time.time() + 15
    while time.time() < deadline:
        count = form_items.count()
        for index in range(count):
            item = form_items.nth(index)
            if item.is_visible() and label in re.sub(r"\s+", " ", item.inner_text()).strip():
                return item
        time.sleep(0.2)
    raise DccE2EError(f"form item not visible for label: {label}")


def _select_form_option(page: Page, label: str, option_text: str) -> None:
    item = _form_item(page, label)
    item.locator(".el-select").first.click()
    option = page.locator(".el-select-dropdown:visible .el-select-dropdown__item").filter(
        has_text=option_text
    ).last
    option.wait_for(state="visible", timeout=15000)
    option.click()
    _wait_quiet(page, 8000)


def _fill_form_input(page: Page, label: str, value: str) -> None:
    _form_item(page, label).locator("input").first.fill(value)


def _fill_form_textarea(page: Page, label: str, value: str) -> None:
    _form_item(page, label).locator("textarea").first.fill(value)


def _select_user_from_picker(page: Page, username: str, *, multiple: bool) -> None:
    picker = _visible_dialog_with_text(page, "用户名称")
    picker.get_by_placeholder("请输入用户名称").fill(username)
    picker.get_by_role("button", name=re.compile("搜索")).click()
    row = picker.locator(".el-table__row").filter(has_text=username).first
    row.wait_for(state="visible", timeout=15000)
    if multiple:
        row.locator(".el-checkbox").first.click()
    else:
        row.click()
    picker.get_by_role("button", name=re.compile(r"确\s*定")).click()
    picker.wait_for(state="hidden", timeout=15000)
    _wait_quiet(page, 5000)


def _visible_dialog_with_text(page: Page, text: str) -> Any:
    dialogs = page.locator(".el-dialog")
    deadline = time.time() + 15
    while time.time() < deadline:
        count = dialogs.count()
        for index in range(count):
            dialog = dialogs.nth(index)
            if dialog.is_visible() and text in re.sub(r"\s+", " ", dialog.inner_text()).strip():
                return dialog
        time.sleep(0.2)
    raise DccE2EError(f"dialog not visible containing text: {text}")


def _select_user_in_form(page: Page, label: str, username: str, *, multiple: bool) -> None:
    _form_item(page, label).locator(".el-input").first.click()
    _select_user_from_picker(page, username, multiple=multiple)


def _select_user_in_dialog(page: Page, dialog: Any, label: str, username: str, *, multiple: bool) -> None:
    _form_item(dialog, label).locator(".el-input").first.click()
    _select_user_from_picker(page, username, multiple=multiple)


def _upload_preview(page: Page, scope: Page | Any, label: str, path: Path) -> dict[str, Any]:
    file_input = _form_item(scope, label).locator("input[type='file']").first
    with page.expect_response(
        lambda response: response.request.method == "POST"
        and "/admin-api/dcc/controlled-files/upload-preview" in response.url,
        timeout=60000,
    ) as response_info:
        file_input.set_input_files(str(path))
    response = response_info.value
    payload = response.json()
    if not response.ok or payload.get("code") != 0:
        raise DccE2EError(f"upload-preview failed: status={response.status}; payload={payload}")
    _wait_quiet(page, 15000)
    return payload


def _expect_route_preview(page: Page) -> None:
    page.locator("button").filter(has_text="预览路线").first.click()
    for stage in ("文控审核", "审核会签", "批准", "文控批准"):
        page.get_by_text(stage, exact=False).first.wait_for(state="visible", timeout=15000)


def _submit_file(
    page: Page,
    assets: dict[str, Path],
    *,
    scenario: str,
    need_training: bool = False,
    selected_signoff_user: str | None = None,
) -> SubmittedFile:
    tag = _now_tag()
    file_name = f"CODEX_E2E_{scenario}_{tag}"
    file_number = f"CODEX-E2E-{scenario}-{tag[-7:]}"
    product_code = _product_code(tag)

    page.goto(f"{FRONTEND_BASE_URL}/dcc/controlled-file/upload", wait_until="domcontentloaded", timeout=30000)
    _wait_quiet(page, 30000)
    _select_form_option(page, "文件类别", "体系文件")
    _fill_form_input(page, "文件名称", file_name)
    _fill_form_input(page, "文件编号", file_number)
    _fill_form_input(page, "产品编号", product_code)
    _fill_form_input(page, "版本号", "V1.0")
    date_input = _form_item(page, "生效日期").locator("input").first
    date_input.fill("2026-05-25")
    date_input.press("Enter")
    if need_training:
        _form_item(page, "培训要求").locator(".el-switch").first.click()
    if selected_signoff_user:
        _select_user_in_form(page, "会签人员", selected_signoff_user, multiple=True)
    _fill_form_textarea(page, "提交备注", f"CODEX_E2E {scenario}")
    _upload_preview(page, page, "受控文件", assets["source_docx"])
    _expect_route_preview(page)

    with page.expect_response(
        lambda response: response.request.method == "POST"
        and "/admin-api/dcc/controlled-files/submit" in response.url,
        timeout=60000,
    ) as response_info:
        page.locator("button").filter(has_text="提交审批").first.click()
    response = response_info.value
    payload = response.json()
    if not response.ok or payload.get("code") != 0 or not payload.get("data"):
        raise DccE2EError(f"submit failed: status={response.status}; payload={payload}")
    _wait_quiet(page, 20000)
    return SubmittedFile(
        id=int(payload["data"]),
        file_name=file_name,
        file_number=file_number,
        product_code=product_code,
        need_training=need_training,
    )


def _approval_load_error(page: Page) -> str:
    alert = page.locator("[data-testid='dcc-controlled-file-approval-load-error']").first
    if not alert.is_visible():
        return ""
    return re.sub(r"\s+", " ", alert.inner_text()).strip()


def _open_approval_task(page: Page, submitted: SubmittedFile) -> str:
    page.goto(
        f"{FRONTEND_BASE_URL}/dcc/controlled-file/approval-tasks",
        wait_until="domcontentloaded",
        timeout=30000,
    )
    _wait_quiet(page, 30000)
    deadline = time.time() + 70
    last_body = ""
    while time.time() < deadline:
        load_error = _approval_load_error(page)
        if load_error:
            raise DccE2EError(f"approval task page load error: {load_error}")
        last_body = _body_text(page)
        row = page.locator(".el-table__row").filter(has_text=submitted.file_name).first
        if row.is_visible():
            row_text = re.sub(r"\s+", " ", row.inner_text()).strip()
            row.locator("button, .el-button").filter(has_text="处理审批").first.click()
            _wait_quiet(page, 15000)
            return row_text
        page.wait_for_timeout(1000)
    raise DccE2EError(
        f"approval task row not visible for {submitted.file_name}; body={last_body[:3000]}"
    )


def _current_action_dialog(page: Page) -> Any:
    dialog = page.get_by_role("dialog").filter(has_text="签名").last
    dialog.wait_for(state="visible", timeout=15000)
    return dialog


def _submit_action_dialog(
    page: Page,
    endpoint: str,
    *,
    forbidden_request_fields: tuple[str, ...] = (),
) -> dict[str, Any]:
    dialog = _current_action_dialog(page)
    with page.expect_response(
        lambda response: response.request.method == "POST"
        and f"/admin-api/dcc/controlled-files/" in response.url
        and endpoint in response.url,
        timeout=60000,
    ) as response_info:
        dialog.get_by_role("button", name="确认签名").click()
    response = response_info.value
    payload = response.json()
    if not response.ok or payload.get("code") != 0:
        dialog_text = dialog.inner_text() if dialog.is_visible() else ""
        raise DccE2EError(
            f"{endpoint} failed: status={response.status}; payload={payload}; dialog={dialog_text[:1000]}"
        )
    request_post_data = response.request.post_data or ""
    forbidden_fields = [field for field in forbidden_request_fields if field in request_post_data]
    if forbidden_fields:
        raise DccE2EError(f"{endpoint} request submitted forbidden fields: {forbidden_fields}")
    _wait_quiet(page, 20000)
    return payload


def _approve_current_task(page: Page, submitted: SubmittedFile) -> dict[str, Any]:
    row_text = _open_approval_task(page, submitted)
    page.locator("button, .el-button").filter(has_text=re.compile("审核通过|批准通过")).first.click()
    dialog = _current_action_dialog(page)
    dialog.locator("input[type='password']").first.fill(APPLICANT.password)
    payload = _submit_action_dialog(page, "approve-task")
    return {"rowText": row_text, "payload": payload}


def _perform_task_action(
    page: Page,
    submitted: SubmittedFile,
    *,
    mode: str,
    reason: str,
    assignee_username: str | None = None,
) -> dict[str, Any]:
    row_text = _open_approval_task(page, submitted)
    label_by_mode = {"return": "回退", "transfer": "转办", "sign": "加签"}
    endpoint_by_mode = {
        "return": "return-task",
        "transfer": "transfer-task",
        "sign": "sign-task",
    }
    page.locator("button, .el-button").filter(has_text=label_by_mode[mode]).first.click()
    dialog = _current_action_dialog(page)
    if mode == "transfer":
        if not assignee_username:
            raise DccE2EError("transfer action requires an assignee username")
        _select_user_in_dialog(page, dialog, "转办人", assignee_username, multiple=False)
    if mode == "sign":
        dialog.locator("label").filter(has_text="前加签").first.click()
        if not assignee_username:
            raise DccE2EError("sign action requires a sign user username")
        _select_user_in_dialog(page, dialog, "加签人", assignee_username, multiple=True)
    dialog.locator("input[type='password']").first.fill(APPLICANT.password)
    dialog.locator("textarea").first.fill(reason)
    payload = _submit_action_dialog(page, endpoint_by_mode[mode])
    return {"rowText": row_text, "payload": payload}


def _controlled_file_row(file_id: int) -> dict[str, str]:
    rows = mysql_rows(
        f"""
SELECT
  id,
  status,
  process_instance_id,
  reject_reason,
  stamped_file_id,
  training_record_file_id,
  need_training + 0 AS need_training
FROM dcc_controlled_file
WHERE tenant_id=122 AND id={file_id} AND deleted=0;
"""
    )
    if not rows:
        raise DccE2EError(f"controlled file {file_id} not found in tenant 122")
    return rows[0]


def _running_tasks(process_instance_id: str) -> list[dict[str, str]]:
    return mysql_rows(
        f"""
SELECT ID_, TASK_DEF_KEY_, NAME_, ASSIGNEE_, PARENT_TASK_ID_
FROM act_ru_task
WHERE PROC_INST_ID_='{_safe_sql(process_instance_id)}'
ORDER BY CREATE_TIME_, ID_;
"""
    )


def _route_snapshot(file_id: int, stage_code: str) -> dict[str, str]:
    rows = mysql_rows(
        f"""
SELECT stage_code, resolved_user_ids, candidate_source_ids
FROM dcc_controlled_file_route_snapshot
WHERE tenant_id=122 AND controlled_file_id={file_id} AND stage_code='{_safe_sql(stage_code)}' AND deleted=0;
"""
    )
    if not rows:
        raise DccE2EError(f"route snapshot {stage_code} not found for file {file_id}")
    return rows[0]


def _signature_count(file_id: int, action_type: str) -> int:
    rows = mysql_rows(
        f"""
SELECT COUNT(*) AS c
FROM dcc_controlled_file_signature
WHERE tenant_id=122 AND controlled_file_id={file_id}
  AND action_type='{_safe_sql(action_type)}' AND deleted=0;
"""
    )
    return int(rows[0]["c"]) if rows else 0


def _assert_dcc_bpm_consistent(file_id: int) -> dict[str, Any]:
    file_row = _controlled_file_row(file_id)
    process_instance_id = file_row["process_instance_id"]
    tasks = _running_tasks(process_instance_id)
    expected_stage = PENDING_STATUS_TO_STAGE.get(file_row["status"])
    if expected_stage and not any(task["TASK_DEF_KEY_"] == expected_stage for task in tasks):
        raise DccE2EError(
            "DCC/BPM status mismatch: "
            f"fileId={file_id}, status={file_row['status']}, expectedStage={expected_stage}, tasks={tasks}"
        )
    return {"file": file_row, "runningTasks": tasks}


def _wait_for_file_status_change(file_id: int, previous_status: str, timeout_seconds: int = 90) -> dict[str, str]:
    deadline = time.time() + timeout_seconds
    current = _controlled_file_row(file_id)
    while time.time() < deadline:
        current = _controlled_file_row(file_id)
        if current["status"] != previous_status:
            return current
        time.sleep(1)
    raise DccE2EError(f"file {file_id} status did not change from {previous_status}: {current}")


def _verify_applicant_return_blocker(page: Page, submitted: SubmittedFile) -> dict[str, Any]:
    file_row = _controlled_file_row(submitted.id)
    page.goto(f"{FRONTEND_BASE_URL}/dcc/controlled-file/browser", wait_until="domcontentloaded", timeout=30000)
    _wait_quiet(page, 20000)
    page.goto(
        f"{FRONTEND_BASE_URL}/dcc/controlled-file/detail/{submitted.id}",
        wait_until="domcontentloaded",
        timeout=30000,
    )
    _wait_quiet(page, 20000)
    body = _body_text(page)
    has_prompt = "有流程回退，需处理" in body or "流程回退" in body
    resubmit_button_count = page.locator("button, .el-button").filter(
        has_text=re.compile("重提|重新提交|继续提交|处理回退")
    ).count()
    if not has_prompt:
        raise DccE2EError(
            f"returned file detail does not show applicant prompt; rejectReason={file_row.get('reject_reason')}; body={body[:2000]}"
        )
    if resubmit_button_count == 0:
        screenshot = e2e_tmp_dir() / f"dcc-e2e-07-applicant-return-blocker-{submitted.id}.png"
        page.screenshot(path=str(screenshot), full_page=True)
        return {
            "status": "blocked",
            "reason": "Returned file shows the prompt but the applicant page exposes no resubmit/reopen entry.",
            "rejectReason": file_row.get("reject_reason"),
            "screenshot": str(screenshot),
        }
    return {
        "status": "passed",
        "reason": "Applicant prompt and resubmit entry are visible.",
        "rejectReason": file_row.get("reject_reason"),
    }


def _upload_applicant_training_record(page: Page, submitted: SubmittedFile, assets: dict[str, Path]) -> dict[str, Any]:
    page.goto(
        f"{FRONTEND_BASE_URL}/dcc/controlled-file/detail/{submitted.id}",
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
        and f"/admin-api/dcc/controlled-files/{submitted.id}/training-record" in response.url,
        timeout=60000,
    ) as response_info:
        dialog.get_by_role("button", name="确认上传").click()
    response = response_info.value
    payload = response.json()
    if not response.ok or payload.get("code") != 0:
        raise DccE2EError(f"training-record upload failed: status={response.status}; payload={payload}")
    _wait_quiet(page, 20000)
    consistency = _assert_dcc_bpm_consistent(submitted.id)
    file_row = consistency["file"]
    if file_row["status"] != "PENDING_DOC_CONTROL_APPROVAL" or not file_row.get("training_record_file_id"):
        raise DccE2EError(f"training-record upload did not move to fourth node: {consistency}")
    return {"payload": payload, "file": file_row, "runningTasks": consistency["runningTasks"]}


def _verify_fourth_node_guards(page: Page, assets: dict[str, Path], submitted: SubmittedFile) -> dict[str, Any]:
    _open_approval_task(page, submitted)
    page.locator("button, .el-button").filter(has_text=re.compile("批准通过|审核通过")).first.click()
    dialog = _current_action_dialog(page)
    dialog_text = re.sub(r"\s+", " ", dialog.inner_text()).strip()
    if "培训记录" in dialog_text:
        raise DccE2EError(f"fourth-node approval dialog still collects applicant training record: {dialog_text}")
    dialog.locator("input[type='password']").first.fill(APPLICANT.password)

    dialog.get_by_role("button", name="确认签名").click()
    page.wait_for_timeout(700)
    missing_stamp_text = re.sub(r"\s+", " ", dialog.inner_text()).strip()
    if "请上传盖章 PDF" not in missing_stamp_text:
        raise DccE2EError(f"missing stamped PDF was not blocked: {missing_stamp_text[:1500]}")

    _form_item(dialog, "盖章 PDF").locator("input[type='file']").first.set_input_files(str(assets["text"]))
    page.wait_for_timeout(700)
    non_pdf_text = re.sub(r"\s+", " ", dialog.inner_text()).strip()
    if "盖章 PDF 必须为 PDF 格式" not in non_pdf_text:
        raise DccE2EError(f"non-PDF stamped upload was not blocked: {non_pdf_text[:1500]}")

    _upload_preview(page, dialog, "盖章 PDF", assets["stamp_pdf"])
    dialog.locator("input[type='password']").first.fill(APPLICANT.password)
    payload = _submit_action_dialog(
        page,
        "approve-task",
        forbidden_request_fields=("trainingRecordFileId", "training_record_file_id"),
    )
    final_row = _wait_for_file_status_change(submitted.id, "PENDING_DOC_CONTROL_APPROVAL")
    if not final_row.get("stamped_file_id") or not final_row.get("training_record_file_id"):
        raise DccE2EError(f"fourth node artifacts were not persisted: {final_row}")
    return {
        "dialogText": dialog_text,
        "missingStampedPdf": missing_stamp_text,
        "nonPdfStampedPdf": non_pdf_text,
        "payload": payload,
        "finalFile": final_row,
    }


def _verify_selected_signoff_scope(
    page: Page,
    assets: dict[str, Path],
    comparison_file: SubmittedFile,
) -> dict[str, Any]:
    selected = _submit_file(
        page,
        assets,
        scenario="SIGNOFF",
        need_training=False,
        selected_signoff_user=COMMON_USER.username,
    )
    selected_snapshot = _route_snapshot(selected.id, "MATRIX_REVIEW")
    comparison_snapshot = _route_snapshot(comparison_file.id, "MATRIX_REVIEW")
    if selected_snapshot["resolved_user_ids"] != str(COMMON_USER.user_id):
        raise DccE2EError(f"selected signoff snapshot is wrong: {selected_snapshot}")
    if comparison_snapshot["resolved_user_ids"] == str(COMMON_USER.user_id):
        raise DccE2EError(
            "selected signoff leaked into another instance: "
            f"comparisonFile={comparison_file.id}, snapshot={comparison_snapshot}"
        )

    _approve_current_task(page, selected)
    consistency = _assert_dcc_bpm_consistent(selected.id)
    running_assignees = {task["ASSIGNEE_"] for task in consistency["runningTasks"]}
    if running_assignees != {str(COMMON_USER.user_id)}:
        raise DccE2EError(
            f"selected signoff BPM task assignees are not scoped to selected user: {consistency}"
        )
    return {
        "selectedFile": selected.__dict__,
        "selectedSnapshot": selected_snapshot,
        "comparisonFile": comparison_file.__dict__,
        "comparisonSnapshot": comparison_snapshot,
        "runningTasks": consistency["runningTasks"],
    }


def run_t3_workflow_actions_e2e() -> dict[str, Any]:
    assert_services_ready()
    ensure_e2e_baseline()
    assets = _ensure_runtime_files()
    result: dict[str, Any] = {
        "passed": [],
        "blocked": [],
        "files": {},
        "checks": {},
    }

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1440, "height": 1000}, accept_downloads=True)
        page = context.new_page()
        login(page, APPLICANT, "/dcc/controlled-file/upload")

        return_file = _submit_file(page, assets, scenario="RETURN")
        _approve_current_task(page, return_file)
        result["checks"]["returnAction"] = _perform_task_action(
            page,
            return_file,
            mode="return",
            reason="CODEX_E2E return to previous node",
        )
        return_consistency = _assert_dcc_bpm_consistent(return_file.id)
        if return_consistency["file"]["status"] != "PENDING_DOC_CONTROL_REVIEW":
            raise DccE2EError(f"return did not move file to DOC_CONTROL_REVIEW: {return_consistency}")
        if _signature_count(return_file.id, "RETURN") < 1:
            raise DccE2EError("return action did not create a RETURN signature")
        result["files"]["return"] = return_file.__dict__
        result["checks"]["returnConsistency"] = return_consistency
        applicant_return = _verify_applicant_return_blocker(page, return_file)
        if applicant_return["status"] == "blocked":
            result["blocked"].append({"id": "E2E-07", **applicant_return})
        else:
            result["passed"].append("E2E-07")
            result["checks"]["applicantReturn"] = applicant_return

        transfer_file = _submit_file(page, assets, scenario="TRANSFER")
        result["checks"]["transferAction"] = _perform_task_action(
            page,
            transfer_file,
            mode="transfer",
            reason="CODEX_E2E transfer to common user",
            assignee_username=COMMON_USER.username,
        )
        transfer_consistency = _assert_dcc_bpm_consistent(transfer_file.id)
        transfer_assignees = {task["ASSIGNEE_"] for task in transfer_consistency["runningTasks"]}
        if transfer_assignees != {str(COMMON_USER.user_id)}:
            raise DccE2EError(f"transfer did not move BPM task to common user: {transfer_consistency}")
        if _signature_count(transfer_file.id, "TRANSFER") < 1:
            raise DccE2EError("transfer action did not create a TRANSFER signature")
        result["files"]["transfer"] = transfer_file.__dict__
        result["checks"]["transferConsistency"] = transfer_consistency

        sign_file = _submit_file(page, assets, scenario="SIGN")
        result["checks"]["signAction"] = _perform_task_action(
            page,
            sign_file,
            mode="sign",
            reason="CODEX_E2E add sign user",
            assignee_username=COMMON_USER.username,
        )
        sign_consistency = _assert_dcc_bpm_consistent(sign_file.id)
        sign_snapshot = _route_snapshot(sign_file.id, "DOC_CONTROL_REVIEW")
        if str(COMMON_USER.user_id) not in sign_snapshot["resolved_user_ids"].split(","):
            raise DccE2EError(f"sign user was not added to route snapshot: {sign_snapshot}")
        if _signature_count(sign_file.id, "ADD_SIGN") < 1:
            raise DccE2EError("sign action did not create an ADD_SIGN signature")
        result["files"]["sign"] = sign_file.__dict__
        result["checks"]["signConsistency"] = sign_consistency
        result["checks"]["signSnapshot"] = sign_snapshot
        result["passed"].append("E2E-06")

        result["checks"]["selectedSignoff"] = _verify_selected_signoff_scope(page, assets, return_file)
        result["passed"].append("E2E-10")

        fourth_file = _submit_file(page, assets, scenario="FOURTH", need_training=True)
        for _ in range(3):
            _approve_current_task(page, fourth_file)
        fourth_gate = _assert_dcc_bpm_consistent(fourth_file.id)
        if fourth_gate["file"]["status"] != "PENDING_APPLICANT_TRAINING_RECORD":
            raise DccE2EError(f"fourth file did not enter applicant training-record gate: {fourth_gate}")
        if fourth_gate["file"].get("training_record_file_id") not in (None, "", "NULL"):
            raise DccE2EError(f"training record was persisted before applicant upload: {fourth_gate}")
        if not any(task["TASK_DEF_KEY_"] == "DOC_CONTROL_APPROVAL" for task in fourth_gate["runningTasks"]):
            raise DccE2EError(f"fourth Flowable task was not running during applicant gate: {fourth_gate}")
        result["files"]["fourth"] = fourth_file.__dict__
        result["checks"]["trainingGate"] = fourth_gate
        result["checks"]["trainingUpload"] = _upload_applicant_training_record(page, fourth_file, assets)
        result["checks"]["fourthNode"] = _verify_fourth_node_guards(page, assets, fourth_file)
        result["passed"].extend(["E2E-08", "E2E-09"])

        context.close()
        browser.close()

    return result


def main() -> None:
    result = run_t3_workflow_actions_e2e()
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
