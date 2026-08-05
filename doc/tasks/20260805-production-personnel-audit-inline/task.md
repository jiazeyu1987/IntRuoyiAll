# 生产人员档案操作追溯移入表单日志

## Task Goal

移除生产组长“人员管理/生产人员档案”页面中的独立“操作追溯”列表；人员操作追溯由已有“表单日志”能力承载，不在人员档案页单独渲染一张追溯表。

## Milestones

- [ ] M1: 定位生产人员档案页面、现有审计列表和相关测试契约。
- [ ] M2: 编写 RED 静态合同，证明当前页面仍有独立操作追溯列表。
- [ ] M3: 移除独立操作追溯列表及只为该列表服务的前端状态/请求。
- [ ] M4: 运行目标静态合同、相邻合同和 TypeScript 检查。
- [ ] M5: 更新验证报告、执行 cleanup、提交并推送。

## Expected Verification

- `node tests/e2e/production-personnel-audit-inline-static.spec.cjs` 先 RED 后 GREEN。
- 相邻生产人员档案或班组长静态合同通过。
- `pnpm ts:check` 通过。
- 不修改后端 API，不新增 fallback/mock，不隐藏错误。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，前端移除重复审计列表，统一由表单日志承载追溯查看。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 待读取 `docs/experience-index.md` 后补充适用门禁。
