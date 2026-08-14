# 20260802 DCC 受控打印 E2E Verification Report

## Summary

- Status: E2E BLOCKED
- Scope: DCC 文控“受控打印”真实 Playwright E2E 验证。
- Conclusion: 尚未 E2E PASS。当前系统真实页面没有可完成“受控打印/打印申请 -> 用途/份数/接收部门/使用位置 -> 审批或直接打印 -> 受控打印记录”的完整链路。
- Restriction observed: 未使用 admin 账号；未用 API-only/SQL 创建打印记录；未改文件状态；未 mock 上传或打印成功。

## Runtime And Command

- Frontend: `http://127.0.0.1:8081/` returned HTTP `200`.
- Backend: `http://127.0.0.1:48081/actuator/health` returned `UP`.
- Object storage: `http://127.0.0.1:9000/minio/health/ready` returned HTTP `200`.
- Browser: Playwright used local Chrome at `C:\Program Files\Google\Chrome\Application\chrome.exe`.
- Command: `node E:\IntRuoyi\doc\tasks\20260802-dcc-controlled-print-e2e\controlled-print-real-e2e.mjs` with password injected only through the approved PowerShell expression.
- Result file: `E:\IntRuoyi\doc\tasks\20260802-dcc-controlled-print-e2e\controlled-print-real-e2e-result.json`.

## Current Active File Evidence

- File ID: `2054545668044070287`.
- File number: `CODX-DCC-ORIG-20260802101521`.
- Version: `V1.0`.
- Status: `ACTIVE`.
- Master current active ID: `2054545668044070287`.
- Published/stamped file ID: `9198354916366`.
- Evidence: positive user browser page returned `browser-page` code `0`, total `1`, and showed the task-owned file row; read-only DB confirmed `status=ACTIVE`, `version_no=V1.0`, and `current_active_controlled_file_id=2054545668044070287`.

## Real Page Evidence

- Positive account: non-admin `wangsiyu` login succeeded.
- Controlled browse path: `/dcc/controlled-file/browser?scope=global&keyword=CODX-DCC-ORIG-20260802101521&pageNo=1&pageSize=20`.
- Browser row actions: `预览`, `下载`; `hasPrintLikeRowAction=false`.
- Detail entry: Playwright clicked `data-testid="dcc-browser-file-number-detail-link"` from the real browser row and landed on `/dcc/controlled-file/detail/2054545668044070287?traceability=1&from=browser...`.
- Detail page/API: detail API returned HTTP `200`, code `0`, `status=ACTIVE`, `versionNo=V1.0`, `currentActiveVersionNo=V1.0`, but the real traceability detail page did not render the file number and exposed only `返回`; no `受控打印`, `打印申请`, or `流程打印` entry was available on this real browser-origin path.
- Required form labels: `打印用途=false`, `份数=false`, `接收部门=false`, `使用位置=false`.
- Negative account: non-admin `zhangkeying` was granted only browse/preview capability for this task-owned file; same browser path returned code `0`, total `1`, row visible, actions `预览` only, and no print-like entry.

## Print Record Fields

- 打印记录 ID：未生成；无真实页面受控打印入口，未触发打印申请或最终打印。
- 文件版本：已证明当前有效版本为 `V1.0`，但未发生受控打印动作。
- 打印人：未生成；有权限账号仅完成浏览/详情尝试。
- 份数：未生成；页面没有份数字段。
- 审批人：未生成；无打印申请，未进入打印审批。
- 审批状态：未生成；schema 中未发现可核验的受控打印申请/审批记录表。

## Read-Only DB Evidence

- `dcc_controlled_file`: target file is `ACTIVE`, `V1.0`, tenant `1`, master current active version points to itself.
- `information_schema.tables LIKE 'dcc%print%'`: only `dcc_approval_print_template` was found.
- Print-related columns scan: found access/log/distribution/signature/template-related fields, but no DCC controlled-print request/application/record table containing copies, receiving department, use location, print person, print status, or approval state.
- Existing trace rows such as access events, watermark traces, access logs, and distribution rows were not treated as controlled-print records.

## Blockers

- Missing page entry: controlled browse row has no `受控打印/打印申请/打印` action.
- Missing form: detail/browser path does not show `打印用途`, `份数`, `接收部门`, or `使用位置`.
- Missing record chain: DB schema has approval process print template only, not a controlled-print request/record table.
- Missing final print evidence: no page path generated a controlled print number, print record ID, print person, copies, approval state, or watermarked controlled print output.

## Final Status

- E2E BLOCKED, not PASS.
- The blocker is product/runtime capability absence on the real page path, not a password, MinIO, frontend, backend, or Playwright environment failure.
