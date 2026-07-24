# Execution Log: Move production schedule calendar under scheduling management

BDD: Single scheduling calendar entry -> Given the system already has `排班管理` and the production schedule calendar page, When the operator opens scheduling-related menus, Then `生产排程日历` appears under `排班管理` as the single clear business entry for schedule-calendar operations.

## Evidence

- M1/M2: Completed. Previous backend task blocker was acknowledged and this task document was created before backend menu data changes.
- RED: previous menu state -> FAIL, `排班管理` had no `生产排程日历` menu row.
- GREEN: local `system_menu` / `system_role_menu` verification -> PASS
- GREEN: authenticated `GET /admin-api/system/auth/get-permission-info` contains `生产排程日历` -> PASS
