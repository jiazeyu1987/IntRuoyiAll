# DCC 签核追溯 UX 修复验证报告

## Scope

本报告仅覆盖 DCC 受控文件“签核追溯”页面可见性与相关 UX 修复验证，不扩展修复其它场景。

## Result

- Overall: PASS
- Static Contract: PASS
- Detail render safety contract: PASS
- Type Check: PASS
- Real Playwright E2E: PASS for task-owned original upload, wrong-password diagnostic, four-level approval/signature, publish ACTIVE, low-permission prompt, and traceability.
- DCC write requests during final E2E: 0
- Password literal scan: `NO_PASSWORD_LITERAL_FOUND`
- Closeout cleanup: PASS for task-owned duplicate evidence cleanup.
- Git closeout: PASS, commit `a88d00bda` pushed to `origin/int_main`.

## Evidence Matrix

| Item | Result | Evidence |
| --- | --- | --- |
| 签名留痕权限提示业务化 | PASS | `zhaojie` 非 admin、无高级签名管理权限，`traceability-ux-permission-prompt-20260802120622.png` 显示业务化提示，旧提示不可见。 |
| 操作日志空态闭环 | PASS | `traceability-ux-operation-logs-20260802120622.png`；接口 `code=0,total=0`，页面显示“暂无操作日志，签核证据请见签核追溯/生命周期。” |
| 审批意见与签名证据合并展示 | PASS | `traceability-ux-detail-20260802120622.png`；追溯行展示审批意见、签名时间、签名方式、证据状态、hash、文件证据。 |
| 发布/盖章文件可点击验证 | PASS | `traceability-ux-file-evidence-viewer-20260802120622.png`；按钮打开 `viewer=1&from=signature-trace` 受控预览页。 |
| 导出/打印入口和内容字段 | PASS | `signature-trace-ux-export-20260802120622.csv` 包含审批意见、文件证据、文件 hash、签名人和签名时间；打印按钮启用。 |
| 签名失败诊断可操作 | PASS | `dcc-original-release-wrong-password-20260802115503.json` 记录 `zhaohaichen` 错误密码响应 `1080000022` 且页面诊断四项 token 可见。 |
| 详情页渲染安全 | PASS | `node tests/e2e/dcc-detail-approval-render-safety-static.spec.js` 确认签核详情页无 `})const openControlledBrowserLocation` 拼接语法回归，审批弹窗状态均显式初始化。 |

## Main Chain Data

- File number: `CODX-DCC-TRACE-DIAG-20260802115503`
- File name: `Codex DCC 签核追溯诊断 20260802115503`
- Controlled file ID: `2054545668044070299`
- Version: `V1.0`
- Status: `ACTIVE`
- Uploader: `彭云凤 (pengyunfeng)`
- Approvers/signers:
  - `赵海辰` / `PASSWORD` / `VALID` / `292ae2db15a5` / `E2E V1.0 文控审核 同意 20260802115503`
  - `赵杰` / `PASSWORD` / `VALID` / `193a80a1e82d` / `E2E V1.0 会签审核 同意 20260802115503`
  - `赵明玉` / `PASSWORD` / `VALID` / `932f13991a16` / `E2E V1.0 会签批准 同意 20260802115503`
  - `王思雨` / `PASSWORD` / `VALID` / `be42bf5eb372` / `E2E V1.0 文控批准 同意 20260802115503`
- publishedFileId: `9198354916368`
- stampedFileId: `9198354916368`
- Wrong-password diagnostic: `zhaohaichen` / `文控审核` / response code `1080000022` / UI tokens visible: `签名失败原因`, `当前密码错误`, `处理建议`, `责任入口`

## Page Paths

- Detail from controlled browser: `/dcc/controlled-file/detail/2054545668044070299?traceability=1&from=browser`
- File evidence viewer: `/dcc/controlled-file/detail/2054545668044070299?viewer=1&from=signature-trace`
- Operation logs: `/dcc/controlled-file/logs?keyword=CODX-DCC-TRACE-DIAG-20260802115503&controlledFileId=2054545668044070299`

## Verification Commands

- `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS
- `pnpm ts:check` -> PASS
- `node tests/e2e/dcc-controlled-file-logs-static.spec.js` -> PASS
- `node tests/e2e/dcc-browser-file-number-detail-entry-static.spec.js` -> PASS
- `node tests/e2e/dcc-controlled-file-detail-retired-static.spec.js` -> PASS
- `node tests/e2e/dcc-detail-approval-render-safety-static.spec.js` -> PASS
- `node doc/tasks/20260802-dcc-traceability-ux-fixes/dcc-original-release-with-wrong-password-e2e.cjs` -> PASS
- `node doc/tasks/20260802-dcc-traceability-ux-fixes/traceability-ux-real-e2e.cjs` -> PASS (`traceability-ux-real-e2e-result-20260802120622.json`)
- `task-closeout-cleanup --mode preview/apply` -> PASS, `blocked=<none>`, `warnings=<none>`，仅清理旧轮次重复证据。
- `git -c http.https://github.com.proxy= push origin int_main` -> PASS, `a88d00bda` pushed.
