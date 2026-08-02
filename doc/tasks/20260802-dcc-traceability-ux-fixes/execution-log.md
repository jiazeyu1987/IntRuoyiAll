# DCC 签核追溯 UX 修复执行日志

## User Intent

- 修复签核追溯真实页面中 5 个 UX 问题。
- 修复后进行 E2E 验证。
- 不使用 admin，不用 API-only/SQL 改状态，不顺手修其它场景。
- 密码仅通过环境变量注入，文档和日志不记录明文。

## BDD

- BDD: 签名留痕权限提示业务化 -> Given 查看账号没有 DCC 电子签名管理权限 When 打开受控文件详情 Then 页面提示当前仍可查看签核追溯摘要，高级签名留痕需要额外权限。
- BDD: 操作日志空态闭环 -> Given 目标文件操作日志接口返回成功但无行 When 用户从目标文件进入操作日志 Then 页面显示暂无操作日志，并指引签核证据见签核追溯/生命周期。
- BDD: 追溯节点合并审批意见和签名证据 -> Given 已发布受控文件存在四级审批签名 When 用户查看签核追溯 Then 每个审批节点同时显示审批意见、签名时间、签名方式、证据状态、文件 hash 和文件证据。
- BDD: 发布盖章文件可点击验证 -> Given 受控文件已生成 publishedFileId/stampedFileId When 用户查看签核追溯 Then 页面提供查看盖章/发布文件入口，而不是只显示 ID。
- BDD: 签名失败诊断可操作 -> Given 用户输入错误签名密码或缺少签名授权 When 提交签名 Then 弹窗明确显示原因、处理建议和责任入口。

## RED/GREEN

- RED: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> FAIL，期望失败原因：当前实现仍显示“签名留痕无法加载；审批任务加载不受影响”。
- GREEN: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `node tests/e2e/dcc-controlled-file-logs-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-browser-file-number-detail-entry-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-controlled-file-detail-retired-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-detail-approval-render-safety-static.spec.js` -> PASS。

## Implementation Evidence

- `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue`
  - 签核追溯新增审批意见列和文件证据列。
  - 文件证据列展示 `publishedFileId`、`stampedFileId`，并提供“查看盖章/发布文件”按钮。
  - 签核追溯导出 CSV 和打印 HTML 同步包含审批意见、文件证据。
  - 高级签名留痕权限提示改为“当前可查看签核追溯摘要；高级签名留痕需 DCC 电子签名管理权限。”
- `IntRuoyiFronted/src/views/dcc/controlled-file/logs/index.vue`
  - 当 URL 带 `controlledFileId` 且无日志行时，空态显示“暂无操作日志，签核证据请见签核追溯/生命周期。”
- `IntRuoyiFronted/src/views/dcc/controlled-file/detail/approval-actions.ts`
  - 错误密码、缺授权、签名图片、证据快照失败均包含原因、处理建议、责任入口。

## Real E2E

- Command: `node doc/tasks/20260802-dcc-traceability-ux-fixes/traceability-ux-real-e2e.cjs` with `DCC_E2E_PASSWORD` injected by PowerShell expression.
- Final result: `traceability-ux-real-e2e-result-20260802112712.json` -> PASS。
- Reused controlled file: `CODX-DCC-ORIG-20260802101521` / `2054545668044070287` / `V1.0`。
- Viewer account: `wangsiyu`，非 admin。
- Page evidence:
  - `traceability-ux-detail-20260802112712.png`
  - `traceability-ux-file-evidence-viewer-20260802112712.png`
  - `traceability-ux-operation-logs-20260802112712.png`
  - `signature-trace-ux-export-20260802112712.csv`
- Read-only API verification:
  - `responseCode=0`
  - 文件 ID、文件编号、版本、状态、`publishedFileId`、`stampedFileId`、签名数量、审批意见均与只读详情响应一致。
  - `dccWriteRequests=[]`，未产生 DCC 写请求。

## Resumed E2E Blocker Resolution

- BDD: 低权限签名留痕提示真实触发 -> Given 非 admin `zhaojie` 可以在受控浏览看到任务自有 ACTIVE 文件且没有 `dcc:controlled-file:signature:manage` When 打开文件编号追溯详情 Then 页面展示“当前可查看签核追溯摘要；高级签名留痕需 DCC 电子签名管理权限。”
- BDD: 错误密码签名失败诊断真实触发 -> Given 任务自有原版文件处于首个待审批签名节点 When `zhaohaichen` 输入错误签名密码 Then 页面展示签名失败原因、处理建议、责任入口，且流程不推进；随后输入正确密码继续四级审批到 ACTIVE。
- GREEN: `node --check doc/tasks/20260802-dcc-traceability-ux-fixes/dcc-original-release-with-wrong-password-e2e.cjs` -> PASS。
- GREEN: `node --check doc/tasks/20260802-dcc-traceability-ux-fixes/traceability-ux-real-e2e.cjs` -> PASS。
- GREEN: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `node doc/tasks/20260802-dcc-traceability-ux-fixes/dcc-original-release-with-wrong-password-e2e.cjs` -> PASS；真实页面创建 `CODX-DCC-TRACE-DIAG-20260802115503` / `2054545668044070299`，先错误密码诊断，再四级审批签名至 `ACTIVE`。
- GREEN: `node doc/tasks/20260802-dcc-traceability-ux-fixes/traceability-ux-real-e2e.cjs` -> `traceability-ux-real-e2e-result-20260802120622.json` PASS；`dccWriteRequests=[]`，低权限提示 PASS，错误密码诊断来源 PASS。
- Runtime fix: `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue` 原有 `})const openControlledBrowserLocation` 语法错误导致 Vite 动态模块 500，已做最小换行修复；复验 `http://127.0.0.1:8081/src/views/dcc/controlled-file/detail/index.vue` -> 200。

## Former E2E Gaps Resolved

- 签名失败诊断真实错误密码：PASS。`zhaohaichen` 首节点错误密码响应 `1080000022`，页面显示“签名失败原因 / 当前密码错误 / 处理建议 / 责任入口”，随后正确密码继续主链路。
- 低权限权限提示真实触发：PASS。`zhaojie` 无高级签名管理权限，能看到目标文件并进入追溯详情，页面显示业务化权限提示且旧误导文案不可见。

## Secret Handling

- Password injection command used environment variable only.
- Secret scan result: `NO_PASSWORD_LITERAL_FOUND`。

## Closeout Cleanup

- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260802-dcc-traceability-ux-fixes --mode preview` -> PASS，`status=ready`，`blocked=<none>`，`warnings=<none>`。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260802-dcc-traceability-ux-fixes --mode apply` -> PASS，删除旧轮次重复截图/CSV/JSON 和已归档 `frontend-feature-evidence.md`，保留最终 E2E 脚本、最终 JSON、截图、CSV、`task.md`、`execution-log.md`、`verification-report.md`。
- GREEN: post-cleanup password literal scan -> `NO_PASSWORD_LITERAL_FOUND`。
- GREEN: scoped `git diff --check` for task implementation, tests, and task records -> PASS。
- BLOCKED: commit/push closeout is not executed because the shared `E:\IntRuoyi` worktree contains many non-task dirty changes; current task remains `ready_for_closeout` until shared worktree commit policy is handled.

## Current Status

ready_for_closeout
