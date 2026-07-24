# Execution Log

## BDD

BDD: 字段审计页使用真实权限进入查询 -> Given 当前用户具备字段审计查询权限, When 打开字段审计页, Then 前端必须调用查询接口并展示结果，不显示权限不足。

BDD: 执行页字段变更保存要求原因与签名 -> Given 用户修改可审计字段, When 未填写原因或签名密码, Then 前端阻止保存；填写后调用字段审计保存接口。

BDD: 审批关闭与归档入口一致 -> Given eDHR 状态不是已关闭, When 查看执行或审批详情, Then 前端不得展示可执行归档动作；已关闭后才可生成归档。

## TDD Evidence

RED: `EDHR_E2E_BASE_URL=http://127.0.0.1:8099 EDHR_E2E_LABEL=local-worktree-test-backend-readonly-strict-red node doc\tasks\20260527-edhr-business-flow-repair\scripts\verify-edhr-test-tenant-readonly.cjs` -> FAIL, expected reason: 测试后端运行镜像缺少当前 eDHR 接口，字段审计、审批、追踪、签名接口返回 `No static resource admin-api/...`。

GREEN: `node --test scripts\edhr-approval-page-contract.test.mjs scripts\edhr-tracking-signature-contract.test.mjs scripts\edhr-approval-archive-gate.test.mjs scripts\edhr-field-audit-api-contract.test.mjs scripts\edhr-field-audit-ui-contract.test.mjs scripts\edhr-execution-page.test.mjs scripts\edhr-execution-submit.test.mjs scripts\edhr-v1-feedback-entry.test.mjs` -> PASS, 24 passed。

RED: `pnpm ts:check` -> FAIL, expected reason: Node 默认堆内存不足，`vue-tsc` 退出 `JavaScript heap out of memory`。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

GREEN: `node ...\vite\bin\vite.js build --mode test` with `VITE_BASE_URL=http://172.30.30.58:48081` and `VITE_OUT_DIR=dist-intruoyi-test` -> PASS。

GREEN: 测试服前端镜像更新 -> PASS, `intruoyi-frontend:20260527_edhr_business_flow_repair` 已在测试服启动。

GREEN: `EDHR_E2E_BASE_URL=http://172.30.30.58:8081 EDHR_E2E_LABEL=test-tenant-readonly-strict-green node doc\tasks\20260527-edhr-business-flow-repair\scripts\verify-edhr-test-tenant-readonly.cjs` -> PASS, 测试租户字段审计、审批、追踪、签名页面均无权限不足、404、`No static resource` 或接口业务错误。
