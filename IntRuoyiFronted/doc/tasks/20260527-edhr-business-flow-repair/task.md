# 任务：eDHR 非发布功能前端修复

## 任务目标

- 在前端独立 worktree 中修复测试租户可验证的 eDHR 非发布功能问题。
- 聚焦字段审计页权限提示、执行页字段审计保存、提交审批、审批关闭、追踪与归档入口一致性。

## 里程碑

- [x] M1：创建前端任务文档。
- [x] M2：补充失败合同测试或 Playwright 复现。
- [x] M3：最小化修复前端交互、权限判断或接口契约。
- [x] M4：运行目标 Node/ts/build 与 Playwright 回归。

## 预期验证

- `node --test scripts\edhr-approval-page-contract.test.mjs scripts\edhr-tracking-signature-contract.test.mjs scripts\edhr-approval-archive-gate.test.mjs scripts\edhr-field-audit-api-contract.test.mjs scripts\edhr-field-audit-ui-contract.test.mjs scripts\edhr-execution-page.test.mjs scripts\edhr-execution-submit.test.mjs scripts\edhr-v1-feedback-entry.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=16384'; pnpm ts:check`
- Playwright 真实测试租户 E2E。

## 当前状态

completed

## 结果

- 前端源码合同已覆盖字段审计、审批、追踪、签名和归档入口；本轮未发现需要改动生产前端源码的问题。
- 测试服此前失败的主要原因是运行镜像缺后端接口与字段审计角色权限未合入。
- 当前 worktree 前端构建产物已部署到测试服标签 `20260527_edhr_business_flow_repair` 并通过真实测试租户只读 E2E。
