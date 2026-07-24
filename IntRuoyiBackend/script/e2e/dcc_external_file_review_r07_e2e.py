from __future__ import annotations

import json
import os
import re
import time
from pathlib import Path
from typing import Any

from playwright.sync_api import Page, sync_playwright

from script.e2e.dcc_screenshot_e2e_helpers import (
    APPLICANT,
    DccE2EError,
    FRONTEND_BASE_URL,
    TENANT_ID,
    assert_services_ready,
    ensure_e2e_baseline,
    login,
    mysql_rows,
    mysql_scalar,
    run_mysql,
)
from script.e2e.dcc_screenshot_workflow_actions_e2e import (
    SubmittedFile,
    _body_text,
    _current_action_dialog,
    _ensure_runtime_files,
    _fill_form_input,
    _fill_form_textarea,
    _form_item,
    _now_tag,
    _open_approval_task,
    _product_code,
    _select_form_option,
    _select_user_in_form,
    _upload_preview,
    _wait_quiet,
)


EXTERNAL_PROCESS_DEFINITION_KEY = "dcc-external-file-review"
ORDINARY_PROCESS_DEFINITION_KEY = "dcc-controlled-file-approval"


def _safe_sql(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "\\'")


def _ensure_external_review_table() -> None:
    run_mysql(
        """
CREATE TABLE IF NOT EXISTS `dcc_external_file_review` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `controlled_file_id` bigint NOT NULL COMMENT 'DCC 主记录编号',
  `external_source` varchar(128) NOT NULL COMMENT '外来来源',
  `external_owner` varchar(128) NOT NULL COMMENT '外来归属或责任方',
  `review_reason` varchar(500) NOT NULL COMMENT '评审原因',
  `participant_user_ids` varchar(500) NOT NULL COMMENT '参与人用户编号，逗号分隔',
  `review_conclusion` varchar(64) DEFAULT NULL COMMENT '评审结论',
  `conclusion_comment` varchar(1000) DEFAULT NULL COMMENT '结论说明',
  `output_file_id` bigint DEFAULT NULL COMMENT '输出物文件编号',
  `closed_time` datetime DEFAULT NULL COMMENT '闭环时间',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建者',
  `updater` varchar(64) DEFAULT NULL COMMENT '更新者',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dcc_external_file_review_file` (`controlled_file_id`, `tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DCC 外来文件评审扩展';
""",
        batch=False,
    )
    exists = mysql_scalar(
        """
SELECT COUNT(*) AS c
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'dcc_external_file_review';
"""
    )
    if exists != "1":
        raise DccE2EError("missing required table dcc_external_file_review in E2E database")


def _external_review_bpmn_xml() -> str:
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
             xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
             targetNamespace="http://yudao.iocoder.cn/dcc">
  <process id="{EXTERNAL_PROCESS_DEFINITION_KEY}" name="DCC 外来文件评审" isExecutable="true">
    <startEvent id="StartEvent" name="开始" />
    <sequenceFlow id="flow_start_doc_control_review" sourceRef="StartEvent" targetRef="DOC_CONTROL_REVIEW" />
    <userTask id="DOC_CONTROL_REVIEW" name="文控审核" flowable:candidateStrategy="35" />
    <sequenceFlow id="flow_doc_control_review_matrix_review" sourceRef="DOC_CONTROL_REVIEW" targetRef="MATRIX_REVIEW" />
    <userTask id="MATRIX_REVIEW" name="审核会签" flowable:candidateStrategy="34" />
    <sequenceFlow id="flow_matrix_review_matrix_approval" sourceRef="MATRIX_REVIEW" targetRef="MATRIX_APPROVAL" />
    <userTask id="MATRIX_APPROVAL" name="批准" flowable:candidateStrategy="34" />
    <sequenceFlow id="flow_matrix_approval_doc_control_approval" sourceRef="MATRIX_APPROVAL" targetRef="DOC_CONTROL_APPROVAL" />
    <userTask id="DOC_CONTROL_APPROVAL" name="文控批准" flowable:candidateStrategy="34" />
    <sequenceFlow id="flow_doc_control_approval_end" sourceRef="DOC_CONTROL_APPROVAL" targetRef="EndEvent" />
    <endEvent id="EndEvent" name="结束" />
  </process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_{EXTERNAL_PROCESS_DEFINITION_KEY}">
    <bpmndi:BPMNPlane id="BPMNPlane_{EXTERNAL_PROCESS_DEFINITION_KEY}" bpmnElement="{EXTERNAL_PROCESS_DEFINITION_KEY}">
      <bpmndi:BPMNShape id="StartEvent_di" bpmnElement="StartEvent">
        <omgdc:Bounds x="160" y="130" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="DOC_CONTROL_REVIEW_di" bpmnElement="DOC_CONTROL_REVIEW">
        <omgdc:Bounds x="250" y="108" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="MATRIX_REVIEW_di" bpmnElement="MATRIX_REVIEW">
        <omgdc:Bounds x="410" y="108" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="MATRIX_APPROVAL_di" bpmnElement="MATRIX_APPROVAL">
        <omgdc:Bounds x="570" y="108" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="DOC_CONTROL_APPROVAL_di" bpmnElement="DOC_CONTROL_APPROVAL">
        <omgdc:Bounds x="730" y="108" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="EndEvent_di" bpmnElement="EndEvent">
        <omgdc:Bounds x="890" y="130" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="flow_start_doc_control_review_di" bpmnElement="flow_start_doc_control_review">
        <omgdi:waypoint x="196" y="148" />
        <omgdi:waypoint x="250" y="148" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="flow_doc_control_review_matrix_review_di" bpmnElement="flow_doc_control_review_matrix_review">
        <omgdi:waypoint x="350" y="148" />
        <omgdi:waypoint x="410" y="148" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="flow_matrix_review_matrix_approval_di" bpmnElement="flow_matrix_review_matrix_approval">
        <omgdi:waypoint x="510" y="148" />
        <omgdi:waypoint x="570" y="148" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="flow_matrix_approval_doc_control_approval_di" bpmnElement="flow_matrix_approval_doc_control_approval">
        <omgdi:waypoint x="670" y="148" />
        <omgdi:waypoint x="730" y="148" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="flow_doc_control_approval_end_di" bpmnElement="flow_doc_control_approval_end">
        <omgdi:waypoint x="830" y="148" />
        <omgdi:waypoint x="890" y="148" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>
"""


def _api_request(page: Page, method: str, url: str, data: dict[str, Any] | None = None) -> dict[str, Any]:
    result = page.evaluate(
        """async ({ method, url, data }) => {
          const readCacheValue = (key) => {
            const raw = localStorage.getItem(key)
            if (!raw) return ''
            try {
              const parsed = JSON.parse(raw)
              if (parsed && typeof parsed === 'object') {
                const inner = parsed.v ?? parsed.value ?? parsed.data ?? ''
                if (typeof inner === 'string') {
                  try {
                    return JSON.parse(inner)
                  } catch (error) {
                    return inner
                  }
                }
                return inner
              }
              return parsed || ''
            } catch (error) {
              return raw
            }
          }
          const accessToken = readCacheValue('ACCESS_TOKEN')
          const tenantId = readCacheValue('tenantId') || '122'
          const headers = { 'Content-Type': 'application/json' }
          if (accessToken) headers.Authorization = `Bearer ${accessToken}`
          if (tenantId) {
            headers['tenant-id'] = String(tenantId)
            headers['visit-tenant-id'] = String(tenantId)
          }
          const response = await fetch(url, {
            method,
            headers,
            body: data ? JSON.stringify(data) : undefined
          })
          const payload = await response.json().catch(() => ({}))
          return { status: response.status, payload }
        }""",
        {"method": method, "url": url, "data": data},
    )
    if result["status"] >= 400 or result["payload"].get("code") != 0:
        raise DccE2EError(f"BPM fixture API failed: {method} {url}; result={result}")
    return result["payload"]


def _reset_external_review_model_for_test_tenant() -> None:
    run_mysql(
        f"""
SET @tenant_id := '{TENANT_ID}';
SET @process_key := '{EXTERNAL_PROCESS_DEFINITION_KEY}';

UPDATE ACT_RE_PROCDEF
SET SUSPENSION_STATE_=2
WHERE KEY_=@process_key AND TENANT_ID_=@tenant_id;

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_r07_model_bytearray_ids AS
SELECT EDITOR_SOURCE_VALUE_ID_ AS id FROM ACT_RE_MODEL WHERE KEY_=@process_key AND TENANT_ID_=@tenant_id
UNION
SELECT EDITOR_SOURCE_EXTRA_VALUE_ID_ AS id FROM ACT_RE_MODEL WHERE KEY_=@process_key AND TENANT_ID_=@tenant_id;

DELETE FROM ACT_RE_MODEL WHERE KEY_=@process_key AND TENANT_ID_=@tenant_id;

DELETE b
FROM ACT_GE_BYTEARRAY b
JOIN tmp_r07_model_bytearray_ids ids ON ids.id = b.ID_;

DROP TEMPORARY TABLE IF EXISTS tmp_r07_model_bytearray_ids;
""",
        batch=False,
    )


def _ensure_external_review_process_definition(page: Page) -> dict[str, str]:
    _reset_external_review_model_for_test_tenant()
    model_payload = {
        "key": EXTERNAL_PROCESS_DEFINITION_KEY,
        "name": "DCC 外来文件评审",
        "category": "dcc",
        "description": "Codex R07 E2E test-tenant external file review definition",
        "type": 10,
        "formType": 20,
        "formCustomCreatePath": "/dcc/controlled-file/external-review",
        "formCustomViewPath": "/dcc/controlled-file/detail",
        "visible": True,
        "startUserIds": [APPLICANT.user_id],
        "managerUserIds": [APPLICANT.user_id],
        "allowCancelRunningProcess": True,
        "allowWithdrawTask": False,
        "processIdRule": {"enable": False, "prefix": "", "infix": "", "postfix": "", "length": 5},
        "autoApprovalType": 0,
        "titleSetting": {"enable": False, "title": ""},
        "summarySetting": {"enable": False, "summary": []},
        "printTemplateSetting": {"enable": False},
        "bpmnXml": _external_review_bpmn_xml(),
    }
    create_payload = _api_request(page, "POST", "/admin-api/bpm/model/create", model_payload)
    model_id = create_payload.get("data")
    if not model_id:
        raise DccE2EError(f"BPM model create returned no model id: {create_payload}")
    _api_request(page, "PUT", "/admin-api/bpm/model/update-bpmn", {"id": model_id, "bpmnXml": model_payload["bpmnXml"]})
    _api_request(page, "POST", f"/admin-api/bpm/model/deploy?id={model_id}")

    rows = mysql_rows(
        f"""
SELECT KEY_, TENANT_ID_, VERSION_, SUSPENSION_STATE_
FROM ACT_RE_PROCDEF
WHERE KEY_='{EXTERNAL_PROCESS_DEFINITION_KEY}' AND TENANT_ID_='{TENANT_ID}'
ORDER BY VERSION_ DESC
LIMIT 1;
"""
    )
    if not rows:
        raise DccE2EError(
            f"missing Flowable process definition key {EXTERNAL_PROCESS_DEFINITION_KEY} for tenant {TENANT_ID}"
        )
    latest = rows[0]
    if latest["SUSPENSION_STATE_"] != "1":
        raise DccE2EError(f"external review Flowable definition is not active: {latest}")
    return latest


def _controlled_file_business_row(file_id: int) -> dict[str, str]:
    rows = mysql_rows(
        f"""
SELECT
  f.id,
  f.status,
  f.process_type,
  f.process_definition_key,
  f.process_instance_id,
  r.external_source,
  r.external_owner,
  r.review_reason,
  r.participant_user_ids,
  r.review_conclusion,
  r.conclusion_comment,
  r.output_file_id,
  r.closed_time
FROM dcc_controlled_file f
LEFT JOIN dcc_external_file_review r
  ON r.controlled_file_id=f.id AND r.tenant_id=f.tenant_id AND r.deleted=0
WHERE f.tenant_id={TENANT_ID} AND f.id={file_id} AND f.deleted=0;
"""
    )
    if not rows:
        raise DccE2EError(f"controlled file {file_id} not found in tenant {TENANT_ID}")
    return rows[0]


def _process_definition_key_for_file(file_id: int) -> str:
    rows = mysql_rows(
        f"""
SELECT d.KEY_ AS processDefinitionKey
FROM dcc_controlled_file f
JOIN ACT_HI_PROCINST h ON h.PROC_INST_ID_=f.process_instance_id
JOIN ACT_RE_PROCDEF d ON d.ID_=h.PROC_DEF_ID_
WHERE f.tenant_id={TENANT_ID} AND f.id={file_id}
LIMIT 1;
"""
    )
    if not rows:
        raise DccE2EError(f"process definition key not found for controlled file {file_id}")
    return rows[0]["processDefinitionKey"]


def _wait_for_status_change(file_id: int, previous_status: str, timeout_seconds: int = 90) -> dict[str, str]:
    deadline = time.time() + timeout_seconds
    current = _controlled_file_business_row(file_id)
    while time.time() < deadline:
        current = _controlled_file_business_row(file_id)
        if current["status"] != previous_status:
            return current
        time.sleep(1)
    raise DccE2EError(f"file {file_id} status did not change from {previous_status}: {current}")


def _wait_for_closed(file_id: int, timeout_seconds: int = 90) -> dict[str, str]:
    deadline = time.time() + timeout_seconds
    current = _controlled_file_business_row(file_id)
    while time.time() < deadline:
        current = _controlled_file_business_row(file_id)
        if current["closed_time"] and current["status"] == "APPROVED":
            return current
        time.sleep(1)
    raise DccE2EError(f"external review did not close for file {file_id}: {current}")


def _submit_external_review(page: Page, assets: dict[str, Path]) -> SubmittedFile:
    tag = _now_tag()
    file_name = f"CODEX_R07_EXTERNAL_{tag}"
    file_number = f"CODEX-R07-EXT-{tag[-7:]}"
    product_code = _product_code(tag)

    page.goto(f"{FRONTEND_BASE_URL}/dcc/controlled-file/external-review", wait_until="domcontentloaded", timeout=30000)
    _wait_quiet(page, 30000)
    if "外来文件评审" not in _body_text(page):
        raise DccE2EError("external review frontend entry did not render the expected page")
    _select_form_option(page, "文件类别", "体系文件")
    _fill_form_input(page, "外来来源", "CODEX R07 客户来图")
    _fill_form_input(page, "外来归属", "CODEX R07 客户质量部")
    _fill_form_textarea(page, "评审原因", "CODEX R07 外来文件真实路径评审")
    _select_user_in_form(page, "参与人", APPLICANT.username, multiple=True)
    _fill_form_input(page, "文件名称", file_name)
    _fill_form_input(page, "文件编号", file_number)
    _fill_form_input(page, "产品编号", product_code)
    _fill_form_input(page, "版本号", "V1.0")
    date_input = _form_item(page, "生效日期").locator("input").first
    date_input.fill("2026-05-26")
    date_input.press("Enter")
    _fill_form_textarea(page, "提交备注", "CODEX R07 external review E2E")
    _upload_preview(page, page, "外来文件", assets["source_docx"])

    with page.expect_response(
        lambda response: response.request.method == "POST"
        and "/admin-api/dcc/external-file-reviews/submit" in response.url,
        timeout=60000,
    ) as response_info:
        page.locator("button").filter(has_text="提交评审").first.click()
    response = response_info.value
    payload = response.json()
    if not response.ok or payload.get("code") != 0 or not payload.get("data"):
        raise DccE2EError(f"external review submit failed: status={response.status}; payload={payload}")
    _wait_quiet(page, 20000)
    return SubmittedFile(
        id=int(payload["data"]),
        file_name=file_name,
        file_number=file_number,
        product_code=product_code,
        need_training=False,
    )


def _submit_external_action_dialog(page: Page, endpoint: str) -> dict[str, Any]:
    dialog = _current_action_dialog(page)
    with page.expect_response(
        lambda response: response.request.method == "POST"
        and "/admin-api/dcc/external-file-reviews/" in response.url
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
    _wait_quiet(page, 20000)
    return payload


def _approve_external_current_task(
    page: Page,
    submitted: SubmittedFile,
    assets: dict[str, Path],
    *,
    final_node: bool,
) -> dict[str, Any]:
    previous_status = _controlled_file_business_row(submitted.id)["status"]
    row_text = _open_approval_task(page, submitted)
    page.locator("button, .el-button").filter(has_text=re.compile("审核通过|批准通过")).first.click()
    dialog = _current_action_dialog(page)
    dialog.locator("input[type='password']").first.fill(APPLICANT.password)
    if final_node:
        _form_item(dialog, "评审结论").locator(".el-select").first.click()
        page.get_by_text("接收", exact=True).last.click()
        _upload_preview(page, dialog, "输出文件", assets["source_docx"])
        _form_item(dialog, "结论说明").locator("textarea").first.fill("CODEX R07 外来文件评审接收")
    payload = _submit_external_action_dialog(page, "approve-task")
    changed = _wait_for_closed(submitted.id) if final_node else _wait_for_status_change(submitted.id, previous_status)
    return {"rowText": row_text, "payload": payload, "file": changed}


def _assert_submitted_external_review(submitted: SubmittedFile) -> dict[str, str]:
    row = _controlled_file_business_row(submitted.id)
    if row["process_type"] != "EXTERNAL_REVIEW":
        raise DccE2EError(f"external review did not persist process_type=EXTERNAL_REVIEW: {row}")
    if row["process_definition_key"] != EXTERNAL_PROCESS_DEFINITION_KEY:
        raise DccE2EError(f"external review used the wrong process definition key: {row}")
    definition_key = _process_definition_key_for_file(submitted.id)
    if definition_key != EXTERNAL_PROCESS_DEFINITION_KEY:
        raise DccE2EError(
            f"Flowable instance used {definition_key}, expected {EXTERNAL_PROCESS_DEFINITION_KEY}"
        )
    return row


def run_r07_external_file_review_e2e() -> dict[str, Any]:
    assert_services_ready()
    ensure_e2e_baseline()
    _ensure_external_review_table()
    assets = _ensure_runtime_files()
    headless = os.environ.get("DCC_E2E_HEADLESS", "1") != "0"
    result: dict[str, Any] = {"checks": {}, "files": {}}

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=headless)
        context = browser.new_context(viewport={"width": 1440, "height": 1000}, accept_downloads=True)
        try:
            page = context.new_page()
            login(page, APPLICANT, "/dcc/controlled-file/external-review")
            result["checks"]["processDefinition"] = _ensure_external_review_process_definition(page)

            submitted = _submit_external_review(page, assets)
            result["files"]["externalReview"] = submitted.__dict__
            result["checks"]["submitted"] = _assert_submitted_external_review(submitted)

            for _ in range(3):
                approval = _approve_external_current_task(page, submitted, assets, final_node=False)
                result.setdefault("approvals", []).append(approval)
            final_approval = _approve_external_current_task(page, submitted, assets, final_node=True)
            result.setdefault("approvals", []).append(final_approval)
            final_row = _wait_for_closed(submitted.id)

            page.goto(
                f"{FRONTEND_BASE_URL}/dcc/controlled-file/detail/{submitted.id}",
                wait_until="domcontentloaded",
                timeout=30000,
            )
            _wait_quiet(page, 30000)
            detail_body = _body_text(page)
            for expected in ("外来文件评审信息", "CODEX R07 客户来图", "CODEX R07 客户质量部", "ACCEPTED"):
                if expected not in detail_body:
                    raise DccE2EError(f"external review detail missing {expected}: {detail_body[:3000]}")

            definition_key = _process_definition_key_for_file(submitted.id)
            if definition_key == ORDINARY_PROCESS_DEFINITION_KEY:
                raise DccE2EError("R07 E2E used ordinary controlled-file BPM definition unexpectedly")
            if not final_row["output_file_id"]:
                raise DccE2EError(f"external review output file was not persisted: {final_row}")
            if final_row["review_conclusion"] != "ACCEPTED":
                raise DccE2EError(f"external review conclusion was not persisted: {final_row}")

            result.update(
                {
                    "processDefinitionKey": definition_key,
                    "ordinaryProcessDefinitionKey": ORDINARY_PROCESS_DEFINITION_KEY,
                    "closed": bool(final_row["closed_time"]),
                    "finalRow": final_row,
                }
            )
        finally:
            context.close()
            browser.close()

    return result


def main() -> None:
    print(json.dumps(run_r07_external_file_review_e2e(), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
