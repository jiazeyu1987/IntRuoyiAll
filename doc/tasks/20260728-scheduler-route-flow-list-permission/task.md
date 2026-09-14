# 20260728-scheduler-route-flow-list-permission

## Task Goal

让排产员具备工艺流程列表页的可操作列表权限，确保页面“操作”列按正式权限展示可用操作。

## Milestones

- [x] 确认工艺流程列表权限、菜单权限和排产员角色绑定的正式来源。
- [x] 先用回归用例复现排产员缺少工艺流程列表操作权限的问题。
- [x] 实施最小权限补齐方案，不引入 fallback、降级或吞异常。
- [x] 运行目标验证并记录 RED/GREEN/REGRESSION 证据。
- [ ] 完成收尾记录、清理和提交推送。

## Expected Verification

- 静态或后端回归测试覆盖排产员角色必须包含工艺流程列表操作所需权限。
- 如涉及 SQL/菜单权限，核对 `system_menu` 权限来源和目标角色绑定来源。
- 运行受影响测试命令并记录结果。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是补齐正式菜单/角色权限链路。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 权限/菜单变更必须同时核对前端组件、`system_menu` 路径/组件/权限、目标角色菜单绑定和登录后权限响应。
- 写 SQL 或迁移前必须先从当前迁移文件、Mapper、夹具或真实 schema 核对目标表结构；不得凭记忆编写菜单权限 SQL。
- PowerShell 和中文文档读写必须显式 UTF-8；不得使用默认 `Set-Content` / `Add-Content` / `Out-File` 写中文。

## Implementation Summary

- 前端 `IntRuoyiFronted/src/views/mes/pro/route/index.vue` 的工艺流程列表“操作”列使用 `mes:pro-route:update` 显示“产品/编辑”，使用 `mes:pro-route:version-query` 显示“版本”。
- 已在 `IntRuoyiBackend/sql/mysql/20260629_mes_smart_scheduling_role_scope.sql` 的排产员规范角色范围中加入菜单 `5723` 和 `5730`，并把 baseline 校验从 18 项更新为 20 项。
- 已新增 `IntRuoyiBackend/sql/mysql/20260728_mes_scheduler_route_flow_list_permission.sql`，幂等恢复排产员角色菜单绑定，并同步把包含工艺流程父菜单 `5720` 的租户套餐补齐 `5723/5730`。
- 未授予 `mes:pro-route:delete` / 菜单 `5724` 删除权限，避免扩大到破坏性操作。

## Verification Summary

- RED：`python -X utf8 -m pytest script/tests/test_mes_smart_scheduling_role_scope_sql.py script/tests/test_mes_scheduler_route_flow_list_permission_sql.py` -> FAIL，缺少 `5723/5730` 且新增迁移文件不存在。
- GREEN：同一命令 -> PASS，`29 passed in 2.33s`。
- REGRESSION：`python -X utf8 -m pytest script/tests/test_mes_route_version_permission_menu_sql.py script/tests/test_mes_route_flow_config_migration_sql.py` -> PASS，`11 passed in 0.35s`。
- 迁移门禁：全量 `run-release-migration-policy-gate.py --sql-root sql/mysql` 被既有 `20260725_mes_edhr_recordbook_global_setting.sql: config-seed` 阻塞；本次 SQL 及依赖链选择性门禁 PASS，`migrationCount=10`。
- Cleanup：`task_closeout.py --task-id 20260728-scheduler-route-flow-list-permission --mode preview/apply` -> PASS，keep 7 项，delete/blocked/warnings 均为 none。

## Experience Consolidation

- 已核对 `docs/database-rules.md#租户和菜单权限` 与 `docs/experience-index.md` 的菜单/角色绑定门禁；本次经验已被现有长期门禁覆盖，无需新增长期经验文档。

## Cleanup Keep

- doc/tasks/20260728-scheduler-route-flow-list-permission/bug-regression-evidence.md
- doc/tasks/20260728-scheduler-route-flow-list-permission/database-schema-evidence.md
- doc/tasks/20260728-scheduler-route-flow-list-permission/migration-policy-gate.json
- doc/tasks/20260728-scheduler-route-flow-list-permission/migration-policy-gate-targeted.json
