# 20260726 Standard List Template

## Task Goal

将截图黄色框内的执行记录列表调整为项目标准列表模板，拆分到新的“测试记录”页签；“测试记录”页签位于系统管理菜单中“测试管理”和“备份计划”之间，保持现有业务功能、接口契约和无 fallback 行为不变。

## Milestones

- [x] 定位页面入口、组件、菜单 SQL 和现有标准列表模板
- [x] 记录 BDD 场景并执行 RED 验证
- [x] 按标准列表模板拆分测试记录前端页面并移除测试管理内嵌执行记录
- [x] 新增测试记录菜单迁移，保证位于测试管理与备份计划之间
- [x] 执行 GREEN 与回归验证
- [x] 更新证据、清理并完成收尾

## Expected Verification

- 受影响前端静态或组件验证能证明测试记录使用 `UnifiedListTemplate` 标准模板。
- 菜单迁移静态验证能证明测试记录菜单存在、组件路径正确、排序在测试管理与备份计划之间。
- 相关 TypeScript / lint / 构建验证通过，或记录明确阻塞。
- 不引入 fallback、吞异常、默认成功或兼容降级。

## Current Status

completed

## Experience Gates

- Trigger: 前端页面、表格、样式调整命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；新增动态菜单命中 `docs/database-rules.md`。
- Preflight check: 已读取 IntPP 标准列表参考，确认目标模板为 `UnifiedListTemplate`，要求工具栏贴表格、稳定 `table-key`、快速过滤、列配置、紧凑表格和分页；已读取数据库规则，动态菜单页面必须核对组件文件、`system_menu.path`、`component`、`component_name`、`permission`、租户套餐和角色菜单绑定。
- Blocker: 如果页面入口、标准模板组件、列配置 hook、菜单 SQL 或静态合同缺失，必须停止，不得用旧 `ContentWrap + el-form + Pagination` 伪装完成。
- Verification: 使用 `pnpm e2e:system:codex-test-management:static`、菜单迁移静态断言和必要的 `pnpm ts:check` 验证。
- Forbidden action: 不引入 mock、placeholder data、fallback、降级或吞异常。
- Evidence: `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` 与本任务静态合同。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是复用项目标准列表模板。
- `是否存在临时补丁或绕过`：否。

## Closeout Notes

- 实现、验证与 cleanup apply 已完成；当前仅剩本任务提交/推送。
- 2026-07-26 复验 Git 状态时未发现并发任务文件阻塞。
- Cleanup preview/apply 均无删除项、无阻塞、无警告，保留 `frontend-feature-evidence.md`。

## Cleanup Keep

- `doc/tasks/20260726-standard-list-template/frontend-feature-evidence.md`
