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

## BLOCKED Evidence

- 签名失败诊断真实错误密码：BLOCKED。复用文件已 ACTIVE，页面无待办签名按钮；为避免破坏主链路，未创建新审批任务做错误密码写入型诊断。静态合同已覆盖文案。
- 低权限权限提示真实触发：BLOCKED。`pengyunfeng` 在受控浏览入口未看到目标文件行，无法进入详情触发提示；当前主查看账号 `wangsiyu` 有高级访问能力，因此签名留痕区不展示权限提示。静态合同已覆盖文案且旧误导文案已移除。

## Secret Handling

- Password injection command used environment variable only.
- Secret scan result: `NO_PASSWORD_LITERAL_FOUND`。

## Current Status

ready_for_closeout
