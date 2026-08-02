# DCC 签核追溯 UX 修复验证报告

## Scope

本报告仅覆盖 DCC 受控文件“签核追溯”页面可见性与相关 UX 修复验证，不扩展修复其它场景。

## Result

- Overall: PASS_WITH_BLOCKED_DIAGNOSTICS
- Static Contract: PASS
- Type Check: PASS
- Real Playwright E2E: PASS for main traceability path; BLOCKED for low-permission prompt trigger and wrong-password diagnostic trigger.
- DCC write requests during final E2E: 0
- Password literal scan: `NO_PASSWORD_LITERAL_FOUND`

## Evidence Matrix

| Item | Result | Evidence |
| --- | --- | --- |
| 签名留痕权限提示业务化 | PASS static / BLOCKED real trigger | 代码静态合同 PASS；`pengyunfeng` 看不到目标文件行，无法触发真实提示。 |
| 操作日志空态闭环 | PASS | `traceability-ux-operation-logs-20260802112712.png`；接口 `code=0,total=0`，页面显示“暂无操作日志，签核证据请见签核追溯/生命周期。” |
| 审批意见与签名证据合并展示 | PASS | `traceability-ux-detail-20260802112712.png`；追溯行展示审批意见、签名时间、签名方式、证据状态、hash、文件证据。 |
| 发布/盖章文件可点击验证 | PASS | `traceability-ux-file-evidence-viewer-20260802112712.png`；按钮打开 `viewer=1&from=signature-trace` 受控预览页。 |
| 导出/打印入口和内容字段 | PASS | `signature-trace-ux-export-20260802112712.csv` 包含审批意见、文件证据、文件 hash、签名人和签名时间；打印按钮启用。 |
| 签名失败诊断可操作 | PASS static / BLOCKED real trigger | 文案静态合同 PASS；复用文件已 ACTIVE，无待办签名按钮，未创建新任务做错误密码写入诊断。 |

## Main Chain Data

- File number: `CODX-DCC-ORIG-20260802101521`
- File name: `Codex DCC 原版上传链路 20260802101521`
- Controlled file ID: `2054545668044070287`
- Version: `V1.0`
- Status: `ACTIVE`
- Uploader: `彭云凤 (pengyunfeng)`
- Approvers/signers:
  - `赵海辰` / `PASSWORD` / `VALID` / `a8e02bcf2e1e` / `E2E V1.0 文控审核 同意 20260802101521`
  - `赵杰` / `PASSWORD` / `VALID` / `5f21347c67b0` / `E2E V1.0 会签审核 同意 20260802101521`
  - `赵明玉` / `PASSWORD` / `VALID` / `c726be0e39ac` / `E2E V1.0 会签批准 同意 20260802101521`
  - `王思雨` / `PASSWORD` / `VALID` / `1ce0d9220a7b` / `E2E V1.0 文控批准 同意 20260802101521`
- publishedFileId: `9198354916366`
- stampedFileId: `9198354916366`

## Page Paths

- Detail from controlled browser: `/dcc/controlled-file/detail/2054545668044070287?traceability=1&from=browser`
- File evidence viewer: `/dcc/controlled-file/detail/2054545668044070287?viewer=1&from=signature-trace`
- Operation logs: `/dcc/controlled-file/logs?keyword=CODX-DCC-ORIG-20260802101521&controlledFileId=2054545668044070287`

## Verification Commands

- `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS
- `pnpm ts:check` -> PASS
- `node tests/e2e/dcc-controlled-file-logs-static.spec.js` -> PASS
- `node tests/e2e/dcc-browser-file-number-detail-entry-static.spec.js` -> PASS
- `node tests/e2e/dcc-controlled-file-detail-retired-static.spec.js` -> PASS
- `node doc/tasks/20260802-dcc-traceability-ux-fixes/traceability-ux-real-e2e.cjs` -> PASS
