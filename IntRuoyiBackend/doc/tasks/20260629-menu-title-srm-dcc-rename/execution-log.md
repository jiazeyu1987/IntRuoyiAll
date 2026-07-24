# Execution Log：SRM 与文控中心菜单改名（后端）

- BDD: 菜单 SQL 使用新标题 -> Given 菜单初始化与修正 SQL 负责提供正式导航名称 / When 执行相关 SQL 契约测试 / Then `991000` 菜单名称应为 `SRM`，`6800` 菜单名称应为 `文控中心`。
- GREEN: `python -X utf8` 读取 `sql/mysql/20260618_srm_d7_1_code_rule_baseline.sql` 与 `sql/mysql/20260513_dcc_base_schema.sql` -> PASS，确认新标题生效且旧标题已移除。
- BDD: 测试服既有菜单名可通过 migration 就地改名 -> Given 测试服已存在 `system_menu` 根菜单 `991000 / /srm` 与 `6800 / /dcc` / When 执行 `20260629_menu_title_srm_dcc_rename.sql` / Then 顶级菜单名称应分别更新为 `SRM`、`文控中心`。
