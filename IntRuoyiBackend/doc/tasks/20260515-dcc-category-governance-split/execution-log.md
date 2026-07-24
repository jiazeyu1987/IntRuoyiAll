# Execution Log: DCC 分类治理页签菜单拆分

BDD: DCC 文控中心包含独立下发与培训页签 -> Given DCC 前端已拆出独立 `DCC下发` 与 `DCC培训` 页面 / When 本地或后续环境应用 DCC 菜单种子 / Then `DCC文控中心` 菜单可加载这两个新页签并映射到对应前端组件。

BDD: 原有类别与审批路线页签继续可达 -> Given DCC 菜单种子已补齐新页签 / When 用户打开 `DCC文件类别` 与 `DCC审批路线` / Then 原有路径与组件映射保持可用。

RED: existing DCC seed SQL contained menu items for categories, positions, routes, upload, browser, mine, and approval-tasks, but no `controlled-file/distribution` or `controlled-file/training` entries.

GREEN: added `sql/mysql/20260515_dcc_governance_split_menu.sql` and applied the same insert/update set to the local `system_menu` table, after which the real frontend sidebar showed `DCC下发` and `DCC培训` under `DCC文控中心`.
