# 生产人员档案操作追溯移入表单日志

## Task Goal

移除生产组长“人员管理/生产人员档案”页面中的独立“操作追溯”列表；人员操作追溯由已有“表单日志”能力承载，不在人员档案页单独渲染一张追溯表。

## Milestones

- [x] M1: 定位生产人员档案页面、现有审计列表和相关测试契约。
- [x] M2: 编写 RED 静态合同，证明当前页面仍有独立操作追溯列表。
- [x] M3: 移除独立操作追溯列表及只为该列表服务的前端状态/请求。
- [x] M4: 运行目标静态合同、相邻合同和 TypeScript 检查。
- [x] M5: 更新验证报告、执行 cleanup、提交并推送。

## Expected Verification

- `node tests/e2e/production-personnel-audit-inline-static.spec.cjs` 先 RED 后 GREEN。
- 相邻生产人员档案或班组长静态合同通过。
- `pnpm ts:check` 通过。
- 不修改后端 API，不新增 fallback/mock，不隐藏错误。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，前端移除重复审计列表，统一由表单日志承载追溯查看。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs/experience-index.md`。
- 适用门禁：前端静态契约隔离门禁、E2E 脚本与静态合同同步门禁、PowerShell 分号串联测试退出码门禁、共享分支并发基线提交门禁。
- 本任务采用专用静态合同先 RED 后 GREEN；真实 E2E 脚本同步为“无独立追溯表”语义并执行 `node --check`，未用 API-only 或 mock 代替页面行为。
