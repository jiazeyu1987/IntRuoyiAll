# 执行记录：执行展厅菜单种子到本地数据库

BDD: 菜单管理应出现展厅后台页签 -> Given 本地 `ruoyi-vue-pro` 数据库仍未写入展厅菜单种子 / When 执行 `sql/showroom/20260519_showroom_menu_seed.sql` / Then `system_menu` 中应存在统一后的“展厅”父菜单及 8 个后台子页签记录。

BDD: 执行菜单种子不应扩散到角色授权 -> Given 本次任务只执行菜单种子 / When SQL 落库完成 / Then `system_role_menu` 不应被本任务直接改写。

RED: `SELECT id, name, path, component_name FROM system_menu WHERE id BETWEEN 980100 AND 980108 ORDER BY id` -> FAIL，返回 `0` 条记录，当前本地库尚未写入展厅菜单种子。

GREEN: 执行 `sql/showroom/20260519_showroom_menu_seed.sql` -> PASS，`ROWCOUNT 9`。

GREEN: `SELECT id, name, path, component_name FROM system_menu WHERE id BETWEEN 980100 AND 980108 ORDER BY id` -> PASS，返回 9 条记录：
- `980100 展厅 showroom`
- `980101 展厅公司 company ShowroomAdminCompany`
- `980102 产品管理 product ShowroomAdminProduct`
- `980103 展厅管理 hall ShowroomAdminHall`
- `980104 审批中心 approval ShowroomAdminApproval`
- `980105 版本历史 history ShowroomAdminHistory`
- `980106 补充指派 assignment ShowroomAdminAssignment`
- `980107 产品讨论 discussion ShowroomAdminDiscussion`
- `980108 讲解工作台 narration-workbench ShowroomAdminNarration`

GREEN: `SELECT COUNT(*) FROM system_role_menu WHERE menu_id BETWEEN 980100 AND 980108` -> PASS，执行前后均为 `0`，本次任务未顺带写入角色授权。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260519-showroom-menu-seed-apply --mode preview` -> PASS，仅保留 `task.md` 与 `execution-log.md`，无额外清理项。
