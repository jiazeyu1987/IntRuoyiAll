# 生产组长员工管理与签名人员档案

## Task Goal

在隔离 worktree 中实现生产组长工作台独立“员工管理”Tab：支持关联正式工、录入临时工、禁用/启用员工、重置临时工电子签名密码、操作留痕，并确保生产填写员工卡片只显示当前生产组长关联且未禁用的员工。

## Milestones

- [x] 创建隔离 worktree 并确认任务前置规则。
- [x] 编写 BDD + TDD 设计文档。
- [ ] 完成数据库 schema / 迁移 / 约束测试。
- [ ] 完成后端 API、服务、权限和审计测试。
- [ ] 完成前端员工管理 Tab、标准列表模板接入和静态合同测试。
- [ ] 完成生产填写员工卡片数据源联动和回归测试。
- [ ] 完成真实 E2E 验收或记录正式阻塞。
- [ ] 完成 closeout、提交和推送。

## Expected Verification

- 后端：目标 JUnit RED/GREEN 覆盖正式工关联、临时工录入、重名拒绝、禁用后不可选、临时工签名密码重置、正式工禁止组长重置签名密码、审计留痕、权限与租户隔离。
- 数据库：迁移结构、唯一约束、字段安全和回滚说明通过脚本或 schema 测试验证。
- 前端：员工管理 Tab 使用标准列表模板；新增正式工输入搜索下拉、临时工录入弹窗、禁用/启用、临时工签名密码重置、审计记录入口均有静态合同。
- 生产填写：员工卡片只读取当前生产组长关联且未禁用员工，且不暴露全系统用户列表。
- E2E：用真实页面覆盖用户指定的八条验收口径；若 worktree 端口槽位未释放，记录为正式 E2E 环境 blocker，不用 API-only 替代。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，采用正式生产人员档案与组长关联模型，统一电子签名主体。
- `是否存在临时补丁或绕过`：否。

## Worktree

- Path: `D:\IntRuoyiWorktree\20260805-production-personnel-management`
- Branch: `codex/20260805-production-personnel-management`
- Base: `1d145ff957461c6d9dcb11877258b80924419e1e`
- Runtime slot: BLOCKED，`reserve-worktree-slot.ps1` 报 `No available runtime slot for profile 'int_main' in range 1..19.`

## Cleanup Keep

- doc/tasks/20260805-production-personnel-management/bdd-tdd-design.md
