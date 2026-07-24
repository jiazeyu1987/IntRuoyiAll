# 执行日志：给芋道源码 admin 绑定运维工程师角色

BDD: admin 通过运维工程师角色看到运行控制台运维按钮 -> Given 测试服 `芋道源码/admin` 已登录, When 账号绑定“运维工程师”角色且角色拥有 `infra:runtime-control:operate`, Then 前端运行控制台顶部显示发布、备份、回滚和恢复按钮。

BDD: 权限变更最小化 -> Given 运行控制台菜单权限已存在, When 新增或复用“运维工程师”角色并绑定 admin, Then 只插入缺失的 `system_role`、`system_role_menu`、`system_user_role` 关系，不改动用户基础资料。

RED: `scp red-query.sql root@172.30.30.58:/tmp/runtime-permission-red.sql && ssh root@172.30.30.58 "docker exec -i intruoyi-mysql mysql -uroot -p123456 --default-character-set=utf8mb4 ruoyi-vue-pro < /tmp/runtime-permission-red.sql"` -> FAIL, 测试服 `芋道源码/admin` 存在但未绑定“运维工程师”角色；`system_role` 中不存在 `name='运维工程师'` 或 `code='ops_engineer'`。

GREEN: `scp grant-ops-engineer.sql root@172.30.30.58:/tmp/grant-ops-engineer.sql && ssh root@172.30.30.58 "docker exec -i intruoyi-mysql mysql -uroot -p123456 --default-character-set=utf8mb4 ruoyi-vue-pro < /tmp/grant-ops-engineer.sql"` -> PASS, 创建或复用“运维工程师”角色并返回 `tenant_id=1, admin_user_id=1, ops_role_id=910211, runtime_menu_count=4`。

GREEN: `scp green-query.sql root@172.30.30.58:/tmp/runtime-permission-green.sql && ssh root@172.30.30.58 "docker exec -i intruoyi-mysql mysql -uroot -p123456 --default-character-set=utf8mb4 ruoyi-vue-pro < /tmp/runtime-permission-green.sql"` -> PASS, `admin` 已绑定 `运维工程师/ops_engineer`，该角色拥有 `infra:runtime-control:query`、`infra:runtime-control:restart`、`infra:runtime-control:operate`。

BLOCKED: Playwright real frontend path `http://172.30.30.58:8081/infra/monitors/runtime-control` with `芋道源码/admin` -> 数据库权限生效路径已具备，但当前测试服前端包仍是旧版，只显示“刷新/重启”，未包含新增的发布、备份、回滚、恢复按钮区域；需要发布最新前端包后退出重登再看。

GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260525-grant-runtime-control-admin-permission\database-schema-evidence.md` -> PASS.

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-grant-runtime-control-admin-permission --mode preview` -> PASS, only temporary SQL/evidence files were listed for deletion; no blockers or warnings.
