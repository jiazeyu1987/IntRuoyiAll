# 任务：补齐展厅菜单管理种子

## 目标

将展厅后台页签补进 `system_menu` 菜单种子，确保“菜单管理”里可以看到并维护统一后的“展厅”父菜单及其后台子页签。

## 前置任务检查

- 上一个后端任务：`doc/tasks/20260519-showroom-remediation-b2-content-display-contract/task.md`
- 状态：已显式标记为 `blocked`，原因是当前用户优先处理菜单管理缺口。

## 里程碑

- [x] 记录 BDD/TDD 目标
- [x] 补充失败测试，证明当前仓库缺少展厅菜单种子
- [x] 新增展厅菜单 SQL 种子
- [x] 运行测试并记录验证结果

## 范围

- 新增 `sql/showroom/20260519_showroom_menu_seed.sql`
- 新增或更新菜单种子回归测试
- 更新本任务执行记录

## 非范围

- 不改展厅业务表结构
- 不改展厅业务接口契约
- 不改前端页面布局
- 不在本次脚本里追加 `system_role_menu` 授权

## 预期验证

- `python -m pytest script/tests/test_showroom_sql_scripts.py -q`

## 当前状态

completed: 展厅后台页签菜单种子已补齐，菜单种子契约测试通过，且脚本未顺带写入角色授权。

## 最终验证结果

- PASS: `python -m pytest script/tests/test_showroom_sql_scripts.py -q`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260519-showroom-menu-management-seed --mode preview`
