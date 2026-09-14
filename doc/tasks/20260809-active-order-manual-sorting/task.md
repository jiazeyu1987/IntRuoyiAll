# 活跃订单手动排序

## Task Goal

在生产组长“活跃订单池”的操作列增加上移、下移按钮，按当前生产组长的活跃订单范围持久化人工顺序；刷新页面后顺序保持，首行不可上移、末行不可下移。

## Milestones

- [x] M1：确认现有页面、接口、排序与数据表边界。
- [x] M2：以 BDD + RED 测试冻结前端按钮、后端交换与迁移契约。
- [x] M3：实现正式排序字段、移动接口和前端交互。
- [x] M4：完成定向回归、类型检查及技能证据校验。
- [x] M5：完成任务清理、经验沉淀和最终状态更新。
- [x] M6：按用户授权更新 `48081` 后端运行包，完成移动路由运行态门禁和真实 Playwright 复验。
- [x] M7：将活跃订单列表的“生产订单ID”改为展示正式生产订单号，并完成静态合同、类型检查和真实页面复验。

## Expected Verification

- `node tests/e2e/team-leader-active-order-manual-sort-static.spec.cjs`
- `mvn -pl yudao-module-mes -am -Dtest=MesTeamLeaderActiveOrderManualSortTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_mes_active_order_manual_sort_sql.py`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260809-active-order-manual-sorting/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260809-active-order-manual-sorting/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260809-active-order-manual-sorting/database-schema-evidence.md`
- `git diff --check`
- `node tests/e2e/team-leader-active-order-number-display-static.spec.cjs`

## Experience Gate

- `docs/experience-index.md` 已存在。
- 本任务适用 `docs/frontend-development.md` 的前端按钮文案与行为一致性、前端写入成功与列表刷新失败分层门禁。
- 本任务适用 `docs/backend-development.md` 的活跃订单正式负责人范围和 fail-fast 约束；排序不得改变活跃订单路线、快照、放行或 FIFO 业务数据。
- 本任务适用 `docs/database-rules.md` 的 schema 核对、正式迁移和不可静默回填约束。

## Design Constraints Check / 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；顺序由数据库正式字段持久化，由后端在当前登录生产组长范围内原子交换。
- 是否存在临时补丁或绕过：是；用户明确授权更新并重启本机 `48081`。首次任务运行包虽通过结构门禁，但因混入并发 Controller 依赖导致启动失败，已停止且未作为成功版本；其风险仅限本机运行态，任务收尾时删除该失败运行包和脚本。最终验证使用完整、健康的 `int_main` 运行包，不保留运行时降级或兼容分支。

## Cleanup Result

- `task-closeout-cleanup` preview/apply 均通过，无 blocked 或 warnings。
- 已删除失败运行包、任务运行脚本、运行日志、浏览器临时快照和临时回归证据；保留三份核心任务文档、生产实现、迁移和正式测试。

## Current Status

completed

M7 已完成并收尾：活跃订单列表表头和单元格直接展示正式 `workOrderCode`，内部 `workOrderId` 继续仅用于结构化业务操作；聚焦静态合同、相邻回归、类型检查、真实页面复验和任务清理均通过。
