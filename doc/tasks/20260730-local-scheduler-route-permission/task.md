# 本机排产员工艺流程操作权限修复

## Task Goal

修复本机 `排产员/mes_scheduler` 角色在“生产管理 > 工艺流程”页面缺少操作按钮的问题，使本机账号获得工艺流程列表的正式操作权限。

## Milestones

- [x] 建立任务记录并核对适用门禁
- [x] 只读复现本机 `排产员` 工艺流程权限缺口
- [x] 用正式菜单权限数据补齐本机角色绑定
- [x] 复验本机权限、菜单与迁移状态

## Expected Verification

- 只读 SQL 证明修复前本机 `排产员` 缺少 `mes:pro-route:update/version-query` 工艺流程列表非删除型操作权限。
- 本机 SQL 修复后，`system_menu` 存在工艺路线版本菜单 `5730-5734`，`system_role_menu` 中 `排产员` 对目标权限为启用。
- 不修改远端服务器、不重启服务、不改前后端代码。

## Applicable Gates

- 菜单与权限必须按 `system_menu.permission`、`system_role_menu` 和租户菜单包真实数据核对；不得把权限缺失误判为前端组件缺失。
- 适用既有任务口径：`doc/tasks/20260728-scheduler-route-flow-list-permission/bug-regression-evidence.md` 明确排产员只补工艺流程列表非删除型操作权限 `5723` 和 `5730`，不得默认授予删除权限。
- SQL 涉及中文和权限字符串时必须使用 UTF-8 路径，避免中文菜单名乱码。
- 本次是本机数据修复，不走远端发布、重启或测试服数据修改。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按正式菜单权限和角色绑定补齐本机数据。
- `是否存在临时补丁或绕过`：否；仅修复本机权限数据，不改前端隐藏逻辑或后端鉴权。

## Current Status

ready_for_closeout

## Verification Evidence

- 已执行本机正式 SQL：`IntRuoyiBackend/sql/mysql/20260716_mes_route_version_permission_menu.sql` 与 `IntRuoyiBackend/sql/mysql/20260728_mes_scheduler_route_flow_list_permission.sql`。
- 本机 Docker MySQL 复验：`tenant_id=1` 的 `排产员/mes_scheduler` 已启用 `5723/mes:pro-route:update` 与 `5730/mes:pro-route:version-query`。
- 删除权限未扩大：`5724/mes:pro-route:delete` 对 `排产员` 仍为 `deleted=1`。
- 静态迁移契约验证：`python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_scheduler_route_flow_list_permission_sql.py IntRuoyiBackend\script\tests\test_mes_route_version_permission_menu_sql.py -q` -> `9 passed in 0.28s`。

## Closeout Note

- 本机权限数据修复和验证已完成；仓库当前存在其它任务的未提交改动且分支 ahead/behind，未执行提交、推送或全局清理，避免纳入非本任务文件。
