from __future__ import annotations

import json
import re
import subprocess
import time
import zipfile
from pathlib import Path
from typing import Any

from playwright.sync_api import Error as PlaywrightError, Page, TimeoutError as PlaywrightTimeoutError, sync_playwright

from script.e2e.dcc_screenshot_e2e_helpers import (
    APPLICANT,
    COMMON_USER,
    DccE2EError,
    FRONTEND_BASE_URL,
    BACKEND_BASE_URL,
    TENANT_ID,
    assert_services_ready,
    e2e_tmp_dir,
    ensure_e2e_baseline,
    login,
    run_mysql,
)
from script.e2e.dcc_screenshot_workflow_actions_e2e import (
    SubmittedFile,
    _approve_current_task,
    _expect_route_preview,
    _fill_form_input,
    _fill_form_textarea,
    _form_item,
    _now_tag,
    _product_code,
    _select_form_option,
    _upload_preview,
    _wait_quiet,
)


PRINT_TEMPLATE_PERMISSION = "dcc:controlled-file:print-template:manage"


def _safe_sql(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "\\'")


def _write_docx(path: Path, document_xml: str) -> None:
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as docx:
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
        docx.writestr("word/document.xml", document_xml)


def _ensure_template_files() -> dict[str, Path]:
    tmp = e2e_tmp_dir()
    valid = tmp / "codex-r12-approval-template.docx"
    missing_placeholder = tmp / "codex-r12-missing-placeholder.docx"
    _write_docx(
        valid,
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p><w:r><w:t>编号 {{fileNumber}}</w:t></w:r></w:p>
    <w:p><w:r><w:t>名称 {{fileName}}</w:t></w:r></w:p>
    <w:p><w:r><w:t>版本 {{versionNo}}</w:t></w:r></w:p>
    <w:p><w:r><w:t>记录 {{approvalRecords}}</w:t></w:r></w:p>
  </w:body>
</w:document>""",
    )
    _write_docx(
        missing_placeholder,
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p><w:r><w:t>编号 {{fileNumber}}</w:t></w:r></w:p>
    <w:p><w:r><w:t>名称 {{fileName}}</w:t></w:r></w:p>
    <w:p><w:r><w:t>版本 {{versionNo}}</w:t></w:r></w:p>
  </w:body>
</w:document>""",
    )
    return {"valid": valid, "missing_placeholder": missing_placeholder}


def _ensure_r12_database_prerequisites() -> None:
    run_mysql(
        f"""
CREATE TABLE IF NOT EXISTS `dcc_approval_print_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `template_file_id` bigint NOT NULL COMMENT '模板文件编号',
  `template_file_name` varchar(255) NOT NULL COMMENT '模板文件名',
  `template_file_content_type` varchar(255) DEFAULT NULL COMMENT '模板文件类型',
  `active` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建者',
  `updater` varchar(64) DEFAULT NULL COMMENT '更新者',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_dcc_approval_print_template_active` (`tenant_id`, `active`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC 审批打印 Word 模板';

INSERT INTO `system_menu`
  (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '模板配置', '{PRINT_TEMPLATE_PERMISSION}', 2, 14, 6800, 'controlled-file/print-template', 'ep:document-copy',
       'dcc/controlled-file/print-template/index', 'DccApprovalPrintTemplate', 0, b'1', b'1', b'1',
       'codex-e2e', NOW(), 'codex-e2e', NOW(), b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu`
  WHERE `permission`='{PRINT_TEMPLATE_PERMISSION}' OR `path`='controlled-file/print-template'
);

SET @r12_print_template_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `permission`='{PRINT_TEMPLATE_PERMISSION}' OR `path`='controlled-file/print-template'
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_role_menu`
  (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT ur.role_id, @r12_print_template_menu_id, 'codex-e2e', NOW(), 'codex-e2e', NOW(), b'0', ur.tenant_id
FROM `system_user_role` ur
WHERE ur.user_id={APPLICANT.user_id}
  AND ur.tenant_id={TENANT_ID}
  AND ur.deleted=b'0'
  AND @r12_print_template_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm
    WHERE rm.role_id=ur.role_id AND rm.menu_id=@r12_print_template_menu_id AND rm.tenant_id=ur.tenant_id AND rm.deleted=b'0'
  );

UPDATE `dcc_file_category_permission_rule`
SET `active`=0, `deleted`=1, `updater`='codex-e2e-r12', `update_time`=NOW()
WHERE `tenant_id`={TENANT_ID}
  AND `category_id`=906103
  AND `subject_type`='USER'
  AND `subject_id`={COMMON_USER.user_id};
""",
        batch=False,
    )
    subprocess.run(
        [
            "docker",
            "exec",
            "int-ruoyi-redis",
            "redis-cli",
            "DEL",
            f"permission_menu_ids:{PRINT_TEMPLATE_PERMISSION}",
            f"user_role_ids:{APPLICANT.user_id}",
            f"user_role_ids:{COMMON_USER.user_id}",
        ],
        text=True,
        encoding="utf-8",
        capture_output=True,
        check=False,
    )


def _submit_file_in_category(page: Page, assets: dict[str, Path], *, scenario: str, category: str) -> SubmittedFile:
    tag = _now_tag()
    file_name = f"CODEX_E2E_{scenario}_{tag}"
    file_number = f"CODEX-E2E-{scenario}-{tag[-7:]}"
    product_code = _product_code(tag)

    page.goto(f"{FRONTEND_BASE_URL}/dcc/controlled-file/upload", wait_until="domcontentloaded", timeout=30000)
    _wait_quiet(page, 30000)
    _select_form_option(page, "文件类别", category)
    _fill_form_input(page, "文件名称", file_name)
    _fill_form_input(page, "文件编号", file_number)
    _fill_form_input(page, "产品编号", product_code)
    _fill_form_input(page, "版本号", "V1.0")
    date_input = _form_item(page, "生效日期").locator("input").first
    date_input.fill("2026-05-25")
    date_input.press("Enter")
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
        need_training=False,
    )


def _configure_template(page: Page, template_path: Path, *, expect_success: bool) -> dict[str, Any]:
    page.goto(f"{FRONTEND_BASE_URL}/dcc/controlled-file/print-template", wait_until="domcontentloaded", timeout=30000)
    _wait_quiet(page, 30000)
    file_input = page.locator("input[type='file']").first
    file_input.set_input_files(str(template_path))
    _wait_quiet(page, 30000)

    with page.expect_response(
        lambda response: response.request.method == "POST"
        and "/admin-api/dcc/approval-print-template/save" in response.url,
        timeout=60000,
    ) as response_info:
        page.locator("button, .el-button").filter(has_text=re.compile("保存|启用")).first.click()
    response = response_info.value
    payload = response.json()
    body_text = re.sub(r"\s+", " ", page.locator("body").inner_text(timeout=10000)).strip()
    if expect_success:
        if not response.ok or payload.get("code") != 0:
            raise DccE2EError(f"valid template save failed: status={response.status}; payload={payload}; body={body_text}")
    else:
        if response.ok and payload.get("code") == 0:
            raise DccE2EError(f"invalid template unexpectedly saved: payload={payload}")
        if "占位符" not in body_text and "placeholder" not in body_text.lower():
            raise DccE2EError(f"missing-placeholder error is not visible: payload={payload}; body={body_text}")
    return {"status": response.status, "payload": payload, "body": body_text}


def _read_docx_document_xml(path: Path) -> str:
    with zipfile.ZipFile(path) as docx:
        return docx.read("word/document.xml").decode("utf-8")


def _export_template_word_from_detail(page: Page, submitted: SubmittedFile) -> dict[str, Any]:
    page.goto(f"{FRONTEND_BASE_URL}/dcc/controlled-file/detail/{submitted.id}", wait_until="domcontentloaded", timeout=30000)
    _wait_quiet(page, 30000)
    with page.expect_download(timeout=60000) as download_info:
        page.locator("button, .el-button").filter(has_text="流程导出 Word").first.click()
    download = download_info.value
    output = e2e_tmp_dir() / f"r12-{submitted.id}-approval-print.docx"
    download.save_as(str(output))
    document_xml = _read_docx_document_xml(output)
    expected_fragments = [
        submitted.file_number,
        submitted.file_name,
        "V1.0",
        "文控审核",
        "APPROVE",
    ]
    missing = [fragment for fragment in expected_fragments if fragment not in document_xml]
    if missing:
        raise DccE2EError(f"exported docx is missing data {missing}; xml={document_xml[:2000]}")
    if "{{" in document_xml:
        raise DccE2EError(f"exported docx still contains template placeholder: {document_xml[:2000]}")
    return {"path": str(output), "documentXml": document_xml}


def _print_template_from_detail(page: Page, submitted: SubmittedFile) -> dict[str, Any]:
    page.goto(f"{FRONTEND_BASE_URL}/dcc/controlled-file/detail/{submitted.id}", wait_until="domcontentloaded", timeout=30000)
    _wait_quiet(page, 30000)
    page.context.add_init_script("window.print = () => undefined")
    page.evaluate(
        """() => {
          const originalOpen = window.open.bind(window)
          window.open = (...args) => {
            const popup = originalOpen(...args)
            if (popup) {
              Object.defineProperty(popup, 'print', { value: () => undefined, configurable: true })
              Object.defineProperty(popup.Window.prototype, 'print', { value: () => undefined, configurable: true })
            }
            return popup
          }
        }"""
    )
    expected_fragments = [submitted.file_number, submitted.file_name, "审批记录"]
    with page.expect_popup(timeout=30000) as popup_info, page.expect_response(
        lambda response: response.request.method == "GET"
        and f"/admin-api/dcc/controlled-files/{submitted.id}/approval-print/print-html" in response.url,
        timeout=60000,
    ) as response_info:
        page.locator("button, .el-button").filter(has_text="流程打印").first.click()
    popup = popup_info.value
    print_response = response_info.value
    print_payload = print_response.json()
    print_data = print_payload.get("data") if isinstance(print_payload, dict) else None
    print_html = print_data.get("html") if isinstance(print_data, dict) else ""
    if not print_response.ok or print_payload.get("code") != 0 or not isinstance(print_html, str):
        raise DccE2EError(
            f"print-html request failed: status={print_response.status}; payload={print_payload}"
        )
    missing_from_response = [fragment for fragment in expected_fragments if fragment not in print_html]
    if missing_from_response:
        raise DccE2EError(
            f"print-html response is missing DCC data {missing_from_response}: {print_html[:2000]}"
        )
    popup.wait_for_load_state("domcontentloaded", timeout=15000)
    try:
        popup.wait_for_function(
            r"""fragments => {
              const rawText = (document.body && (document.body.innerText || document.body.textContent)) || ''
              const text = rawText.replace(/\s+/g, ' ').trim()
              return fragments.every((fragment) => text.includes(fragment))
            }""",
            arg=expected_fragments,
            timeout=60000,
        )
    except PlaywrightTimeoutError as error:
        current_text = popup.evaluate(
            r"""() => {
              const rawText = (document.body && (document.body.innerText || document.body.textContent)) || ''
              return rawText.replace(/\s+/g, ' ').trim()
            }"""
        )
        missing = [fragment for fragment in expected_fragments if fragment not in current_text]
        raise DccE2EError(
            f"custom print popup did not render DCC data after waiting for print-html: "
            f"missing={missing}; body={current_text[:2000]}"
        ) from error
    except PlaywrightError as error:
        if popup.is_closed():
            raise DccE2EError(
                "custom print popup closed before DCC data was rendered; "
                "frontend did not leave inspectable print-html content in the popup"
            ) from error
        raise
    text = re.sub(r"\s+", " ", popup.locator("body").inner_text(timeout=10000)).strip()
    missing = [fragment for fragment in expected_fragments if fragment not in text]
    if missing:
        raise DccE2EError(f"custom print popup is missing DCC data {missing}: {text[:2000]}")
    popup.close()
    return {"text": text}


def _api_get_with_current_token(page: Page, path: str) -> dict[str, Any]:
    return page.evaluate(
        """async ({ baseUrl, path }) => {
          const readCacheValue = (key) => {
            const raw = localStorage.getItem(key)
            if (!raw) return ''
            try {
              const parsed = JSON.parse(raw)
              const inner = parsed && typeof parsed === 'object' ? (parsed.v ?? parsed.value ?? parsed.data ?? '') : parsed
              if (typeof inner === 'string') {
                try { return JSON.parse(inner) } catch (error) { return inner }
              }
              return inner || ''
            } catch (error) {
              return raw
            }
          }
          const token = readCacheValue('ACCESS_TOKEN')
          const tenantId = readCacheValue('tenantId') || '122'
          const response = await fetch(`${baseUrl}${path}`, {
            headers: {
              Authorization: token ? `Bearer ${token}` : '',
              'tenant-id': String(tenantId),
              'visit-tenant-id': String(tenantId)
            }
          })
          const contentType = response.headers.get('content-type') || ''
          const payload = contentType.includes('application/json') ? await response.json() : { raw: await response.text() }
          return { status: response.status, contentType, payload }
        }""",
        {"baseUrl": BACKEND_BASE_URL, "path": path},
    )


def _verify_no_permission_export_fails(page: Page, submitted: SubmittedFile) -> dict[str, Any]:
    login(page, COMMON_USER, "/index")
    result = _api_get_with_current_token(
        page,
        f"/admin-api/dcc/controlled-files/{submitted.id}/approval-print/export-word",
    )
    payload = result.get("payload") or {}
    if result["status"] < 400 and payload.get("code") == 0:
        raise DccE2EError(f"no-permission export unexpectedly succeeded: {result}")
    message = str(payload.get("msg") or payload.get("message") or payload)
    if "access" not in message.lower() and "权限" not in message and "cannot" not in message.lower():
        raise DccE2EError(f"no-permission failure message is not explicit: {result}")
    return result


def run_r12_approval_print_template_e2e() -> dict[str, Any]:
    assert_services_ready()
    ensure_e2e_baseline()
    _ensure_r12_database_prerequisites()
    from script.e2e.dcc_screenshot_workflow_actions_e2e import _ensure_runtime_files

    assets = _ensure_runtime_files()
    templates = _ensure_template_files()
    result: dict[str, Any] = {"checks": {}, "files": {}}

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1440, "height": 1000}, accept_downloads=True)
        page = context.new_page()
        login(page, APPLICANT, "/dcc/controlled-file/print-template")

        result["checks"]["invalidTemplate"] = _configure_template(page, templates["missing_placeholder"], expect_success=False)
        result["checks"]["validTemplate"] = _configure_template(page, templates["valid"], expect_success=True)

        exported = _submit_file_in_category(page, assets, scenario="R12PRINT", category="体系文件")
        _approve_current_task(page, exported)
        result["files"]["exported"] = exported.__dict__
        result["checks"]["exportWord"] = _export_template_word_from_detail(page, exported)
        result["checks"]["printHtml"] = _print_template_from_detail(page, exported)

        restricted = _submit_file_in_category(page, assets, scenario="R12DENY", category="技术文件-DMR")
        result["files"]["restricted"] = restricted.__dict__
        result["checks"]["noPermission"] = _verify_no_permission_export_fails(page, restricted)

        context.close()
        browser.close()

    return result


def main() -> None:
    result = run_r12_approval_print_template_e2e()
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
