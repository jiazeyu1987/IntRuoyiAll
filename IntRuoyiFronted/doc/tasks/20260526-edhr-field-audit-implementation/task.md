# 任务：eDHR 字段级不可篡改审计前端实现

## 任务目标

在本前端 worktree 中实现字段审计 API、执行页保存门禁、原因/签名弹窗、审计查询/详情/校验/导出 UI，严格遵循根任务包与已放行设计文档。

## 里程碑

- [x] T3：前端 API、UI 路径、合同测试。
- [x] T4：配合独立验证修复前端缺陷。

## 预期验证

- node 合同测试 GREEN。
- `pnpm ts:check` GREEN。
- Playwright 真实路径可修改字段、保存、查询、详情、校验链、导出。
- 无旧 `saveEdhrExecutionDraft` 字段变更绕过。

## 当前状态

- 状态：completed
- 分支：`task/20260526-edhr-field-audit-implementation`
- 基线：`task/20260526-edhr-approval-tracking-implementation`

## 本轮结果

- 已完成：T3 前端 API helper、执行页 pending diff/原因/签名保存门禁、字段审计分页/详情/校验/导出 UI、前端合同测试。
- 已完成修正：字段审计响应字段改为 `previousHash/auditHash`，执行页 draft/baseline/hash 状态改用 `fieldPath + fieldKey + rowIndex + columnIndex` 组合 identity，缺 `fieldKey/fieldPath` 与 NUMBER 非数字默认值改为 fail-fast。
- 已完成修正：恢复 `src/types/auto-imports.d.ts`，使 `tsconfig.relaxed.json` 能加载与 `build/vite/index.ts` AutoImport 配置一致的全局类型声明；`.gitignore` 已对该必需声明文件放行，避免后续 worktree 丢失。
- 已验证：`NODE_OPTIONS=--max-old-space-size=16384 pnpm ts:check` GREEN；字段审计 API/UI 合同测试 GREEN；eDHR 执行页、提交、追踪签名相关 node 回归 GREEN。
- 已验证：真实 Playwright E2E 从当前前端 worktree `http://127.0.0.1:8086` 登录测试租户，完成字段修改、原因、FIELD_CHANGE 签名、列表、verify-chain、导出、详情；DB_HASH executionId=19 GREEN。
- 剩余阻塞：无。
