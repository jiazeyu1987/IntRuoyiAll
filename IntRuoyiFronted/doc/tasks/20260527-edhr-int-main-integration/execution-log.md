# Execution Log

## BDD

BDD: 前端融合后执行详情语义优先 -> Given eDHR 执行详情来自 `executionSnapshotJson`，When 页面展示工艺路线、工序、工作站和批记录报表，Then 旧模板字段只能作为兼容信息出现，不再作为主语义。

BDD: 前端融合后审批归档闸门可见 -> Given 执行记录未审批关闭，When 用户进入列表或详情页，Then 前端不得开放生成归档入口或调用归档 API。

BDD: 前端融合后字段审计链可追踪 -> Given 用户在执行详情修改字段，When 输入原因并完成签名，Then 前端必须调用字段审计保存接口并展示 old/new、原因、签名和 hash 链证据。

## TDD Evidence

RED: `node --test scripts\edhr-approval-page-contract.test.mjs scripts\edhr-tracking-signature-contract.test.mjs scripts\edhr-approval-archive-gate.test.mjs scripts\edhr-field-audit-api-contract.test.mjs scripts\edhr-field-audit-ui-contract.test.mjs scripts\edhr-execution-page.test.mjs scripts\edhr-execution-submit.test.mjs scripts\edhr-v1-feedback-entry.test.mjs` -> FAIL, `edhr-v1-feedback-entry.test.mjs` 断言执行详情必须包含 `兼容信息`，当前页面仍显示 `模板信息`。

GREEN: `node --test scripts\edhr-approval-page-contract.test.mjs scripts\edhr-tracking-signature-contract.test.mjs scripts\edhr-approval-archive-gate.test.mjs scripts\edhr-field-audit-api-contract.test.mjs scripts\edhr-field-audit-ui-contract.test.mjs scripts\edhr-execution-page.test.mjs scripts\edhr-execution-submit.test.mjs scripts\edhr-v1-feedback-entry.test.mjs` -> PASS, 24 tests passed.

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=16384'; pnpm ts:check` -> PASS.

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=16384'; pnpm build:local` -> PASS, Vite build successful.
