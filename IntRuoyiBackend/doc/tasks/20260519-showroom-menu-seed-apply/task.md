# 任务：执行展厅菜单种子到本地数据库

## 目标

将 `sql/showroom/20260519_showroom_menu_seed.sql` 执行到当前本地 `ruoyi-vue-pro` 数据库，并确认“展厅”父菜单及后台子页签已真正落库。

## 前置任务检查

- 上一个相关任务：`doc/tasks/20260519-showroom-menu-management-seed/task.md`
- 状态：completed，菜单种子文件与回归测试已补齐。

## 里程碑

- [x] 记录 BDD/TDD 目标
- [x] 连接本地数据库并执行 RED 预检查
- [x] 执行展厅菜单种子 SQL
- [x] 运行 GREEN 回查并更新记录

## 范围

- 只执行 `sql/showroom/20260519_showroom_menu_seed.sql`
- 只查询并验证 `system_menu` 中展厅菜单记录
- 更新本任务文档与执行日志

## 非范围

- 不修改其他数据库表
- 不追加 `system_role_menu` 授权
- 不改后端/前端源码

## 预期验证

- 本地数据库连接成功
- 执行前 `system_menu` 中不存在 `980100` 到 `980108` 的展厅菜单种子，或与目标状态不一致
- 执行后 `system_menu` 中存在 9 条目标菜单记录

## 当前状态

completed: 已执行本地展厅菜单种子，`system_menu` 中落库 9 条展厅菜单记录，且未改写 `system_role_menu`。

## 最终验证结果

- PASS: `SELECT id, name, path, component_name FROM system_menu WHERE id BETWEEN 980100 AND 980108 ORDER BY id` 返回 9 条展厅菜单记录。
- PASS: `SELECT COUNT(*) FROM system_role_menu WHERE menu_id BETWEEN 980100 AND 980108` 执行前后均为 `0`。
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260519-showroom-menu-seed-apply --mode preview`
