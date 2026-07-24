# 执行日志：角色管理三分改名与导航重组菜单迁移

## 2026-06-26

- 初始化任务：创建后端任务台账，记录门禁、设计约束与 BDD。
- BDD: 原角色管理改为目录父菜单 -> Given `system_menu` 中存在 101 号角色管理菜单 / When 执行迁移 SQL / Then 101 必须变为目录型父菜单并保留在系统管理原排序位置。
- BDD: 旧角色管理权限迁移到新权限角色子菜单 -> Given 角色已经绑定旧 101 菜单或其按钮权限 / When 执行迁移 SQL / Then 系统新增权限角色子菜单并把原角色可见性迁移到新子菜单，不丢失菜单树。
- BDD: 组织角色和审批角色收拢到角色管理下 -> Given 角色已经绑定旧 104 或 6804 菜单 / When 执行迁移 SQL / Then 两个菜单迁移到 101 号父菜单下，且其绑定角色自动补齐父目录 101。
- BDD: 租户套餐补齐新权限角色菜单 -> Given `system_tenant_package.menu_ids` 已包含旧 101 菜单 / When 执行迁移 SQL / Then 套餐菜单集合补齐新的权限角色子菜单编号，避免后续套餐同步遗漏该入口。
- RED: python -X utf8 -m pytest script/tests/test_role_management_split_sql_contract.py -q -> FAIL，初始基线 SQL 和增量 SQL 不包含 `900183`、`organization-role`、`approval-role` 与角色绑定补齐契约。
- GREEN: apply_patch -> PASS，已更新 `sql/mysql/ruoyi-vue-pro.sql` 与 `sql/mysql/20260513_dcc_base_schema.sql`，并新增 `sql/mysql/20260626_role_management_split_rename_navigation.sql`。
- GREEN: apply_patch -> PASS，已新增 `script/tests/test_role_management_split_sql_contract.py`，固定 `101 / 900183 / 104 / 6804` 菜单结构、按钮父级迁移与 `system_tenant_package.menu_ids` 补齐契约。
- GREEN: python -X utf8 -m pytest script/tests/test_role_management_split_sql_contract.py -q -> PASS。
- GREEN: python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-role-management-split-rename-navigation\database-schema-evidence.md -> PASS。
- GREEN: python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-role-management-split-rename-navigation --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro -> PASS。
