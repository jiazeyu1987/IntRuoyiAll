from __future__ import annotations

import argparse
import json
import os
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from playwright.sync_api import Page, expect, sync_playwright

from script.e2e.dcc_screenshot_e2e_helpers import (
    APPLICANT,
    COMMON_USER,
    FRONTEND_BASE_URL,
    TENANT_ID,
    assert_no_unexpected_browser_errors,
    assert_services_ready,
    clear_permission_cache,
    collect_api_errors,
    collect_console_errors,
    e2e_tmp_dir,
    ensure_e2e_baseline,
    fill_form_input,
    fill_form_textarea,
    login,
    mysql_rows,
    mysql_scalar,
    run_mysql,
    select_first_option_by_form_label,
    unique_code,
)


DMR_CATEGORY_ID = 906103
DHF_CATEGORY_ID = 906102
E2E_DIRECTORY_ID = 906200
E2E_DIRECTORY_PATH = "DCC E2E Documents"
PROCESS_KEY = "dcc-controlled-file-approval"
T2_PREFIX = "CODEX_E2E_T2_"


@dataclass(frozen=True)
class SubmittedUpload:
    controlled_file_id: int
    master_id: int
    source_file_id: int
    drawing_pdf_file_id: int
    file_name: str
    file_number: str
    source_file_name: str


@dataclass(frozen=True)
class FixtureFile:
    id: int
    master_id: int
    file_name: str
    file_number: str
    status: str


def _sql_literal(value: str | None) -> str:
    if value is None:
        return "NULL"
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def _product_code() -> str:
    return f"P{int(time.time() * 1000) % 10_000_000_000_000:013d}"


def _today() -> str:
    return time.strftime("%Y-%m-%d")


def _evidence_path() -> Path:
    path = e2e_tmp_dir() / "t2-upload-download-evidence.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    return path


def _sample_files() -> dict[str, Path]:
    sample_dir = e2e_tmp_dir() / "samples"
    sample_dir.mkdir(parents=True, exist_ok=True)
    files = {
        "dwg": sample_dir / "CODEX_E2E_T2_drawing.dwg",
        "pdf": sample_dir / "CODEX_E2E_T2_drawing.pdf",
        "zip": sample_dir / "CODEX_E2E_T2_archive.zip",
    }
    files["dwg"].write_bytes(b"CODEX_E2E_T2_DWG_SAMPLE\n")
    files["zip"].write_bytes(b"CODEX_E2E_T2_ZIP_SAMPLE\n")
    files["pdf"].write_bytes(
        b"%PDF-1.4\n"
        b"1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
        b"2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj\n"
        b"3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 200 200]>>endobj\n"
        b"trailer<</Root 1 0 R>>\n%%EOF\n"
    )
    return files


def _controlled_file_count(file_name: str) -> int:
    count = mysql_scalar(
        f"""
SELECT COUNT(*) AS c
FROM dcc_controlled_file
WHERE tenant_id={TENANT_ID}
  AND deleted=0
  AND file_name={_sql_literal(file_name)};
"""
    )
    return int(count or "0")


def _wait_for_controlled_file(file_name: str, timeout_seconds: float = 20.0) -> SubmittedUpload:
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        rows = mysql_rows(
            f"""
SELECT cf.id,
       cf.master_id,
       cf.source_file_id,
       cf.drawing_pdf_file_id,
       cf.file_name,
       cf.file_number,
       source_file.name AS source_file_name
FROM dcc_controlled_file cf
LEFT JOIN infra_file source_file ON source_file.id = cf.source_file_id
WHERE cf.tenant_id={TENANT_ID}
  AND cf.deleted=0
  AND cf.file_name={_sql_literal(file_name)}
ORDER BY cf.id DESC
LIMIT 1;
"""
        )
        if rows:
            row = rows[0]
            return SubmittedUpload(
                controlled_file_id=int(row["id"]),
                master_id=int(row["master_id"]),
                source_file_id=int(row["source_file_id"]),
                drawing_pdf_file_id=int(row["drawing_pdf_file_id"]),
                file_name=row["file_name"],
                file_number=row["file_number"],
                source_file_name=row["source_file_name"],
            )
        time.sleep(0.5)
    raise AssertionError(f"Timed out waiting for controlled file {file_name}")


def _download_access_log_count(file_id: int, user_id: int, result: str = "ALLOWED") -> int:
    value = mysql_scalar(
        f"""
SELECT COUNT(*) AS c
FROM dcc_controlled_file_access_log
WHERE tenant_id={TENANT_ID}
  AND deleted=0
  AND controlled_file_id={file_id}
  AND user_id={user_id}
  AND action_type='DOWNLOAD'
  AND result={_sql_literal(result)};
"""
    )
    return int(value or "0")


def _prepare_category_permission_preconditions() -> None:
    active_download_count = mysql_scalar(
        f"""
SELECT COUNT(*) AS c
FROM dcc_file_category_permission_rule
WHERE tenant_id={TENANT_ID}
  AND deleted=0
  AND active=1
  AND category_id={DHF_CATEGORY_ID}
  AND action_type='DOWNLOAD'
  AND subject_type='USER'
  AND subject_id={COMMON_USER.user_id};
"""
    )
    if int(active_download_count or "0") != 0:
        raise AssertionError(
            "Precondition failed: common user already has category DOWNLOAD permission on "
            f"{DHF_CATEGORY_ID}, so AC-05 cannot prove INT/RE system-record bypass."
        )

    run_mysql(
        f"""
INSERT INTO dcc_file_category_permission_rule
  (category_id, action_type, subject_type, subject_id, active, remark, tenant_id,
   create_time, update_time, creator, updater, deleted)
SELECT {DHF_CATEGORY_ID}, 'VIEW', 'USER', {COMMON_USER.user_id}, 1,
       'Codex E2E T2 common view-only fixture', {TENANT_ID},
       NOW(), NOW(), 'codex-e2e-t2', 'codex-e2e-t2', 0
WHERE NOT EXISTS (
  SELECT 1
  FROM dcc_file_category_permission_rule r
  WHERE r.tenant_id={TENANT_ID}
    AND r.deleted=0
    AND r.category_id={DHF_CATEGORY_ID}
    AND r.action_type='VIEW'
    AND r.subject_type='USER'
    AND r.subject_id={COMMON_USER.user_id}
);
""",
        batch=False,
    )
    clear_permission_cache()


def _insert_master(category_id: int, file_name: str, file_number: str) -> int:
    run_mysql(
        f"""
INSERT INTO dcc_controlled_file_master
  (category_id, file_name, file_number, current_active_controlled_file_id, status,
   tenant_id, create_time, update_time, creator, updater, deleted)
VALUES
  ({category_id}, {_sql_literal(file_name)}, {_sql_literal(file_number)}, NULL, 'ACTIVE_CHAIN',
   {TENANT_ID}, NOW(), NOW(), 'codex-e2e-t2', 'codex-e2e-t2', 0);
""",
        batch=False,
    )
    value = mysql_scalar(
        f"""
SELECT id
FROM dcc_controlled_file_master
WHERE tenant_id={TENANT_ID}
  AND deleted=0
  AND file_name={_sql_literal(file_name)}
ORDER BY id DESC
LIMIT 1;
"""
    )
    if value is None:
        raise AssertionError(f"Failed to create DCC master for {file_name}")
    return int(value)


def _insert_controlled_file(
    *,
    master_id: int,
    category_id: int,
    file_name: str,
    file_number: str,
    version_no: str,
    status: str,
    source_file_id: int,
    drawing_pdf_file_id: int | None,
    requester_id: int,
    published: bool,
    remark: str,
) -> int:
    published_file_id = str(source_file_id) if published else "NULL"
    published_time = "NOW()" if published else "NULL"
    approved_time = "NOW()" if published else "NULL"
    run_mysql(
        f"""
INSERT INTO dcc_controlled_file
  (master_id, category_id, binding_directory_id, directory_id, binding_directory_path,
   submit_directory_path, source_file_id, original_file_id, drawing_pdf_file_id, published_file_id,
   file_name, title, file_number, product_code, need_training, process_type, version_no,
   effective_date, remark, status, submitter_id, requester_id, process_instance_id,
   process_definition_key, submitted_time, approved_time, published_time,
   tenant_id, create_time, update_time, creator, updater, deleted)
VALUES
  ({master_id}, {category_id}, {E2E_DIRECTORY_ID}, {E2E_DIRECTORY_ID}, {_sql_literal(E2E_DIRECTORY_PATH)},
   {_sql_literal(E2E_DIRECTORY_PATH)}, {source_file_id}, {source_file_id},
   {drawing_pdf_file_id if drawing_pdf_file_id is not None else 'NULL'}, {published_file_id},
   {_sql_literal(file_name)}, {_sql_literal(file_name)}, {_sql_literal(file_number)}, {_sql_literal(_product_code())},
   b'0', 'CONTROLLED_FILE', {_sql_literal(version_no)}, {_sql_literal(_today())}, {_sql_literal(remark)},
   {_sql_literal(status)}, {requester_id}, {requester_id}, NULL, {_sql_literal(PROCESS_KEY)},
   NOW(), {approved_time}, {published_time}, {TENANT_ID}, NOW(), NOW(),
   'codex-e2e-t2', 'codex-e2e-t2', 0);
""",
        batch=False,
    )
    value = mysql_scalar(
        f"""
SELECT id
FROM dcc_controlled_file
WHERE tenant_id={TENANT_ID}
  AND deleted=0
  AND master_id={master_id}
  AND version_no={_sql_literal(version_no)}
ORDER BY id DESC
LIMIT 1;
"""
    )
    if value is None:
        raise AssertionError(f"Failed to create DCC controlled file for {file_name}/{version_no}")
    file_id = int(value)
    if published:
        run_mysql(
            f"""
UPDATE dcc_controlled_file_master
SET current_active_controlled_file_id={file_id}, update_time=NOW(), updater='codex-e2e-t2'
WHERE id={master_id}
  AND tenant_id={TENANT_ID};
""",
            batch=False,
        )
    return file_id


def _build_fixture_file(
    *,
    category_id: int,
    file_name: str,
    file_number: str,
    source_file_id: int,
    drawing_pdf_file_id: int | None,
    requester_id: int,
    status: str,
    published: bool,
    version_no: str = "V1.0",
    remark: str = "CODEX_E2E_T2 fixture",
) -> FixtureFile:
    master_id = _insert_master(category_id, file_name, file_number)
    file_id = _insert_controlled_file(
        master_id=master_id,
        category_id=category_id,
        file_name=file_name,
        file_number=file_number,
        version_no=version_no,
        status=status,
        source_file_id=source_file_id,
        drawing_pdf_file_id=drawing_pdf_file_id,
        requester_id=requester_id,
        published=published,
        remark=remark,
    )
    return FixtureFile(
        id=file_id,
        master_id=master_id,
        file_name=file_name,
        file_number=file_number,
        status=status,
    )


def _build_modifying_fixture(upload: SubmittedUpload, suffix: str) -> FixtureFile:
    file_name = f"{T2_PREFIX}MOD_{suffix}.dwg"
    file_number = f"CODEX_E2E-T2-MOD-{suffix}"
    master_id = _insert_master(DMR_CATEGORY_ID, file_name, file_number)
    active_id = _insert_controlled_file(
        master_id=master_id,
        category_id=DMR_CATEGORY_ID,
        file_name=file_name,
        file_number=file_number,
        version_no="V1.0",
        status="ACTIVE",
        source_file_id=upload.source_file_id,
        drawing_pdf_file_id=upload.drawing_pdf_file_id,
        requester_id=APPLICANT.user_id,
        published=True,
        remark="CODEX_E2E_T2 active version for modifying badge",
    )
    _insert_controlled_file(
        master_id=master_id,
        category_id=DMR_CATEGORY_ID,
        file_name=file_name,
        file_number=file_number,
        version_no="V2.0",
        status="PENDING_DOC_CONTROL_REVIEW",
        source_file_id=upload.source_file_id,
        drawing_pdf_file_id=upload.drawing_pdf_file_id,
        requester_id=APPLICANT.user_id,
        published=False,
        remark="CODEX_E2E_T2 pending revision for modifying badge",
    )
    return FixtureFile(
        id=active_id,
        master_id=master_id,
        file_name=file_name,
        file_number=file_number,
        status="ACTIVE",
    )


def _activate_uploaded_file(upload: SubmittedUpload) -> None:
    run_mysql(
        f"""
UPDATE dcc_controlled_file
SET status='ACTIVE',
    published_file_id=source_file_id,
    approved_time=COALESCE(approved_time, NOW()),
    published_time=COALESCE(published_time, NOW()),
    update_time=NOW(),
    updater='codex-e2e-t2'
WHERE id={upload.controlled_file_id}
  AND tenant_id={TENANT_ID}
  AND deleted=0;

UPDATE dcc_controlled_file_master
SET current_active_controlled_file_id={upload.controlled_file_id},
    update_time=NOW(),
    updater='codex-e2e-t2'
WHERE id={upload.master_id}
  AND tenant_id={TENANT_ID}
  AND deleted=0;
""",
        batch=False,
    )


def _goto_upload(page: Page) -> None:
    page.goto(f"{FRONTEND_BASE_URL}/dcc/controlled-file/upload", wait_until="networkidle")
    expect(page.get_by_text("提交审批")).to_be_visible(timeout=30_000)


def _upload_file_by_label(page: Page, label: str, path: Path) -> None:
    form_item = page.locator(".el-form-item", has_text=label).first
    form_item.locator("input[type=file]").set_input_files(str(path))
    expect(page.get_by_text(path.name).first).to_be_visible(timeout=30_000)
    if label == "受控文件":
        expect(page.get_by_text(f"预览文件：{path.name}")).to_be_visible(timeout=30_000)
    if label == "图纸 PDF":
        expect(page.get_by_text(f"图纸 PDF：{path.name}")).to_be_visible(timeout=30_000)


def _fill_upload_metadata(
    page: Page,
    *,
    category_name: str,
    file_name: str,
    file_number: str,
    product_code: str,
    remark: str,
) -> None:
    select_first_option_by_form_label(page, "文件类别", category_name)
    fill_form_input(page, "文件名称", file_name)
    fill_form_input(page, "文件编号", file_number)
    fill_form_input(page, "产品编号", product_code)
    fill_form_input(page, "版本号", "V1.0")
    fill_form_input(page, "生效日期", _today())
    fill_form_textarea(page, "提交备注", remark)


def _submit_drawing_without_pdf(page: Page, sample_dwg: Path, suffix: str) -> str:
    file_name = f"{T2_PREFIX}NO_PDF_{suffix}.dwg"
    _goto_upload(page)
    _fill_upload_metadata(
        page,
        category_name="技术文件-DMR",
        file_name=file_name,
        file_number=f"CODEX_E2E-T2-NOPDF-{suffix}",
        product_code=_product_code(),
        remark="CODEX_E2E_T2 missing drawing PDF negative path",
    )
    _upload_file_by_label(page, "受控文件", sample_dwg)
    page.get_by_role("button", name="提交审批").click()
    page.wait_for_timeout(2_000)
    if "/dcc/controlled-file/upload" not in page.url:
        raise AssertionError("AC-01 missing PDF path left the upload page unexpectedly")
    if _controlled_file_count(file_name) != 0:
        raise AssertionError("AC-01 missing PDF path created a controlled file unexpectedly")
    return file_name


def _submit_invalid_product_code(page: Page, suffix: str) -> str:
    file_name = f"{T2_PREFIX}BAD_PRODUCT_{suffix}.dwg"
    _goto_upload(page)
    _fill_upload_metadata(
        page,
        category_name="技术文件-DMR",
        file_name=file_name,
        file_number=f"CODEX_E2E-T2-BAD-PRODUCT-{suffix}",
        product_code="BAD-CODE!",
        remark="CODEX_E2E_T2 invalid product code negative path",
    )
    page.get_by_role("button", name="提交审批").click()
    page.wait_for_timeout(2_000)
    if "/dcc/controlled-file/upload" not in page.url:
        raise AssertionError("AC-02 invalid product code path left the upload page unexpectedly")
    if _controlled_file_count(file_name) != 0:
        raise AssertionError("AC-02 invalid product code path created a controlled file unexpectedly")
    return file_name


def _reject_unsupported_source_file(page: Page, sample_zip: Path, suffix: str) -> str:
    file_name = f"{T2_PREFIX}UNSUPPORTED_{suffix}.zip"
    upload_preview_requests: list[str] = []

    def on_request(request: Any) -> None:
        if request.method == "POST" and "/admin-api/dcc/controlled-files/upload-preview" in request.url:
            upload_preview_requests.append(request.url)

    page.on("request", on_request)
    _goto_upload(page)
    _fill_upload_metadata(
        page,
        category_name="技术文件-DMR",
        file_name=file_name,
        file_number=f"CODEX_E2E-T2-UNSUPPORTED-{suffix}",
        product_code=_product_code(),
        remark="CODEX_E2E_T2 unsupported source negative path",
    )
    form_item = page.locator(".el-form-item", has_text="受控文件").first
    form_item.locator("input[type=file]").set_input_files(str(sample_zip))
    expect(page.get_by_text("仅支持 doc、docx、xls、xlsx、dwg、sldprt、sldasm、slddrw").first).to_be_visible(
        timeout=10_000
    )
    page.wait_for_timeout(1_000)
    if upload_preview_requests:
        raise AssertionError(
            "R01 unsupported source file called upload-preview unexpectedly: "
            f"{json.dumps(upload_preview_requests, ensure_ascii=False)}"
        )
    if _controlled_file_count(file_name) != 0:
        raise AssertionError("R01 unsupported source path created a controlled file unexpectedly")
    return file_name


def _submit_drawing_with_pdf(page: Page, sample_dwg: Path, sample_pdf: Path, suffix: str) -> SubmittedUpload:
    file_name = f"{T2_PREFIX}WITH_PDF_{suffix}.dwg"
    _goto_upload(page)
    _fill_upload_metadata(
        page,
        category_name="技术文件-DMR",
        file_name=file_name,
        file_number=f"CODEX_E2E-T2-WITHPDF-{suffix}",
        product_code=_product_code(),
        remark="CODEX_E2E_T2 drawing PDF positive path",
    )
    _upload_file_by_label(page, "受控文件", sample_dwg)
    _upload_file_by_label(page, "图纸 PDF", sample_pdf)
    page.get_by_role("button", name="提交审批").click()
    page.wait_for_url("**/dcc/controlled-file/browser**", timeout=30_000)
    submitted = _wait_for_controlled_file(file_name)
    if submitted.drawing_pdf_file_id <= 0:
        raise AssertionError("AC-01 positive path did not persist drawing_pdf_file_id")
    if submitted.source_file_name != sample_dwg.name:
        raise AssertionError(
            "AC-01 positive path did not preserve uploaded source file name: "
            f"expected={sample_dwg.name}, actual={submitted.source_file_name}"
        )
    return submitted


def _assert_modifying_badge(page: Page, file: FixtureFile) -> None:
    page.goto(f"{FRONTEND_BASE_URL}/dcc/controlled-file/detail/{file.id}", wait_until="networkidle")
    expect(page.get_by_text(file.file_name).first).to_be_visible(timeout=30_000)
    expect(page.get_by_text("修改中")).to_be_visible(timeout=30_000)


def _download_from_detail(page: Page, file_id: int, expected_warning_issues: list[str], save_name: str) -> Path:
    page.goto(f"{FRONTEND_BASE_URL}/dcc/controlled-file/detail/{file_id}", wait_until="networkidle")
    expect(page.get_by_role("button", name="下载受控文件")).to_be_visible(timeout=30_000)
    download_dir = e2e_tmp_dir() / "downloads"
    download_dir.mkdir(parents=True, exist_ok=True)
    target = download_dir / save_name
    with page.expect_download(timeout=30_000) as download_info:
        page.get_by_role("button", name="下载受控文件").click()
        message_box = page.locator(".el-message-box").first
        expect(message_box).to_be_visible(timeout=10_000)
        prompt_text = message_box.inner_text(timeout=10_000)
        if "非受控" not in prompt_text:
            expected_warning_issues.append(
                "AC-04 product defect: download confirmation is visible but does not state the file "
                f"is non-controlled. prompt={prompt_text!r}"
            )
        message_box.get_by_role("button", name="确认下载").click()
    download_info.value.save_as(str(target))
    if target.stat().st_size <= 0:
        raise AssertionError("AC-04 download produced an empty artifact")
    return target


def _open_browser_with_filters(page: Page, category_id: int) -> None:
    page.goto(
        f"{FRONTEND_BASE_URL}/dcc/controlled-file/browser"
        f"?directoryId={E2E_DIRECTORY_ID}&categoryId={category_id}",
        wait_until="networkidle",
    )
    expect(page.get_by_text("刷新列表")).to_be_visible(timeout=30_000)


def _browser_row(page: Page, file_number: str) -> Any:
    row = page.locator(".el-table__body-wrapper tr", has_text=file_number).first
    expect(row).to_be_visible(timeout=30_000)
    return row


def _download_from_browser_row(page: Page, file: FixtureFile, expected_warning_issues: list[str]) -> Path:
    _open_browser_with_filters(page, DHF_CATEGORY_ID)
    row = _browser_row(page, file.file_number)
    download_button = row.get_by_role("button", name="下载")
    expect(download_button).to_be_visible(timeout=30_000)
    download_dir = e2e_tmp_dir() / "downloads"
    download_dir.mkdir(parents=True, exist_ok=True)
    target = download_dir / f"{file.file_number.replace('/', '_')}.bin"
    with page.expect_download(timeout=30_000) as download_info:
        download_button.click()
        message_box = page.locator(".el-message-box").first
        expect(message_box).to_be_visible(timeout=10_000)
        prompt_text = message_box.inner_text(timeout=10_000)
        if "非受控" not in prompt_text:
            expected_warning_issues.append(
                "AC-05 product defect: system-record download confirmation is visible but does not state "
                f"the file is non-controlled. prompt={prompt_text!r}"
            )
        message_box.get_by_role("button", name="确认下载").click()
    download_info.value.save_as(str(target))
    if target.stat().st_size <= 0:
        raise AssertionError("AC-05 system-record download produced an empty artifact")
    return target


def _assert_browser_row_has_no_download(page: Page, file: FixtureFile) -> None:
    _open_browser_with_filters(page, DHF_CATEGORY_ID)
    row = _browser_row(page, file.file_number)
    if row.get_by_role("button", name="下载").count() != 0:
        raise AssertionError(f"AC-05 rejected file {file.file_number} still exposes a download button")


def run_t2_upload_download_e2e(*, headless: bool = True) -> dict[str, Any]:
    assert_services_ready()
    ensure_e2e_baseline()
    _prepare_category_permission_preconditions()

    suffix = unique_code("").replace("CODEX_E2E_", "")
    samples = _sample_files()
    issues: list[str] = []
    evidence: dict[str, Any] = {
        "suffix": suffix,
        "frontend": FRONTEND_BASE_URL,
        "tenantId": TENANT_ID,
        "downloads": [],
    }

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=headless)
        context = browser.new_context(accept_downloads=True)
        page = context.new_page()
        api_errors = collect_api_errors(page)
        console_errors = collect_console_errors(page)

        login(page, APPLICANT, "/index")
        evidence["unsupportedSourceFileName"] = _reject_unsupported_source_file(page, samples["zip"], suffix)
        evidence["missingPdfFileName"] = _submit_drawing_without_pdf(page, samples["dwg"], suffix)
        evidence["invalidProductFileName"] = _submit_invalid_product_code(page, suffix)
        submitted = _submit_drawing_with_pdf(page, samples["dwg"], samples["pdf"], suffix)
        evidence["submittedUpload"] = {
            "id": submitted.controlled_file_id,
            "sourceFileId": submitted.source_file_id,
            "drawingPdfFileId": submitted.drawing_pdf_file_id,
            "sourceFileName": submitted.source_file_name,
        }

        modifying_file = _build_modifying_fixture(submitted, suffix)
        evidence["modifyingFileId"] = modifying_file.id
        _assert_modifying_badge(page, modifying_file)

        _activate_uploaded_file(submitted)
        before_download_logs = _download_access_log_count(submitted.controlled_file_id, APPLICANT.user_id)
        download_path = _download_from_detail(
            page,
            submitted.controlled_file_id,
            issues,
            f"CODEX_E2E_T2_detail_{suffix}.bin",
        )
        evidence["downloads"].append(str(download_path))
        after_download_logs = _download_access_log_count(submitted.controlled_file_id, APPLICANT.user_id)
        if after_download_logs <= before_download_logs:
            raise AssertionError("AC-04 confirmed detail download did not write an ALLOWED download access log")

        system_record = _build_fixture_file(
            category_id=DHF_CATEGORY_ID,
            file_name=f"{T2_PREFIX}SYSTEM_RECORD_{suffix}.pdf",
            file_number=f"INT/RE-CODEX_E2E-T2-{suffix}",
            source_file_id=submitted.source_file_id,
            drawing_pdf_file_id=submitted.drawing_pdf_file_id,
            requester_id=APPLICANT.user_id,
            status="ACTIVE",
            published=True,
            remark="CODEX_E2E_T2 INT/RE system record fixture",
        )
        nonmatching_file = _build_fixture_file(
            category_id=DHF_CATEGORY_ID,
            file_name=f"{T2_PREFIX}NONMATCH_{suffix}.pdf",
            file_number=f"CODEX_E2E-T2-NONMATCH-{suffix}",
            source_file_id=submitted.source_file_id,
            drawing_pdf_file_id=submitted.drawing_pdf_file_id,
            requester_id=APPLICANT.user_id,
            status="ACTIVE",
            published=True,
            remark="CODEX_E2E_T2 nonmatching record fixture",
        )
        unpublished_file = _build_fixture_file(
            category_id=DHF_CATEGORY_ID,
            file_name=f"{T2_PREFIX}UNPUBLISHED_{suffix}.pdf",
            file_number=f"INT/RE-CODEX_E2E-T2-DRAFT-{suffix}",
            source_file_id=submitted.source_file_id,
            drawing_pdf_file_id=submitted.drawing_pdf_file_id,
            requester_id=APPLICANT.user_id,
            status="PENDING_DOC_CONTROL_REVIEW",
            published=False,
            remark="CODEX_E2E_T2 unpublished system-record rejection fixture",
        )

        assert_no_unexpected_browser_errors(api_errors, console_errors)
        context.close()

        context = browser.new_context(accept_downloads=True)
        page = context.new_page()
        api_errors = collect_api_errors(page)
        console_errors = collect_console_errors(page)
        login(page, COMMON_USER, "/index")
        before_system_logs = _download_access_log_count(system_record.id, COMMON_USER.user_id)
        system_download_path = _download_from_browser_row(page, system_record, issues)
        evidence["downloads"].append(str(system_download_path))
        after_system_logs = _download_access_log_count(system_record.id, COMMON_USER.user_id)
        if after_system_logs <= before_system_logs:
            raise AssertionError("AC-05 INT/RE system-record download did not write an ALLOWED access log")
        _assert_browser_row_has_no_download(page, nonmatching_file)
        _assert_browser_row_has_no_download(page, unpublished_file)
        evidence["systemRecordFileId"] = system_record.id
        evidence["nonmatchingFileId"] = nonmatching_file.id
        evidence["unpublishedFileId"] = unpublished_file.id

        assert_no_unexpected_browser_errors(api_errors, console_errors)
        context.close()
        browser.close()

    evidence["issues"] = issues
    _evidence_path().write_text(json.dumps(evidence, ensure_ascii=False, indent=2), encoding="utf-8")
    if issues:
        raise AssertionError("\n".join(issues))
    return evidence


def main() -> int:
    parser = argparse.ArgumentParser(description="Run DCC screenshot T2 upload/download E2E.")
    parser.add_argument("--headed", action="store_true", help="Run Chromium headed for local debugging.")
    args = parser.parse_args()
    evidence = run_t2_upload_download_e2e(
        headless=not args.headed and os.environ.get("DCC_E2E_HEADLESS", "1") != "0"
    )
    print(json.dumps(evidence, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
