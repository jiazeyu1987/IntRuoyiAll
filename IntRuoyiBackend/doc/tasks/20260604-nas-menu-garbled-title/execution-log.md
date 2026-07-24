# 执行日志：修复 NAS 菜单标题乱码

BDD: NAS 菜单标题可读 -> Given 后端 `system_menu` 已存在 NAS 菜单 / When 前端加载动态菜单 / Then 页面菜单标题显示 `NAS 管理`，不得显示 `NAS??`。

BDD: NAS 权限菜单标题可读 -> Given 后端存在 `infra:nas:*` 权限菜单 / When 管理员查看菜单树或角色权限 / Then 权限菜单显示 `NAS 配置查询`、`NAS 配置保存`、`NAS 连接测试`。

BDD: 本机重启修复菜单标题漂移 -> Given 本机运行库 NAS 菜单标题被写成问号 / When 执行本机重启脚本的必需 MySQL 迁移检查 / Then 脚本探针失败并重跑 NAS 菜单 SQL，恢复标准标题。

REPRO: `docker exec -e MYSQL_PWD=123456 int-ruoyi-mysql mysql -uroot -D ruoyi-vue-pro --default-character-set=utf8mb4 --batch --raw --skip-column-names -e "SELECT id, name, HEX(name), permission FROM system_menu WHERE id IN (5900,5901,5902,5903) ORDER BY id;"` -> FAIL，5900 为 `NAS??` / `4E41533F3F`，5901-5903 为 `NAS????` / `4E41533F3F3F3F`。

RED: `python -m pytest script/tests/test_system_nas_menu_sql.py script/tests/test_restart_int_ruoyi_local_schema.py -q` -> FAIL，原因：`20260520_system_nas_management_menu.sql` 不含 `NAS 管理`，`restart-int-ruoyi-local.ps1` 不含 `20260520_system_nas_management_menu.sql` 迁移探针。

GREEN: `python -m pytest script/tests/test_system_nas_menu_sql.py script/tests/test_restart_int_ruoyi_local_schema.py -q` -> PASS，4 tests。

GREEN: `docker cp ...20260520_system_nas_management_menu.sql int-ruoyi-mysql:/tmp/20260520_system_nas_management_menu.sql` + `docker exec ... mysql --default-character-set=utf8mb4 < /tmp/20260520_system_nas_management_menu.sql` -> PASS，本机运行库已重放标准菜单 SQL。

GREEN: `SELECT id, name, HEX(name), permission FROM system_menu WHERE id IN (5900,5901,5902,5903)` -> PASS，5900 为 `NAS 管理`，5901-5903 为 `NAS 配置查询` / `NAS 配置保存` / `NAS 连接测试`，十六进制为 UTF-8 中文字节且不含 `3F`。

GREEN: Playwright + 本机 Chrome 只读登录 `http://localhost:8081/system/nas` -> PASS，左侧菜单标题为 `NAS 管理`，未出现 `NAS??`。

SCAN: `python C:\Users\BJB110\.codex\skills\clear-frontend-copy\scripts\scan_frontend_copy.py --root D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\system\nas --format markdown` -> PASS，`garbled_text: 0`；其余 mixed_language 为 NAS/DCC/ACE 等必要技术术语与已有历史文案，不纳入本次乱码修复。

REGRESSION: `git diff --check -- sql\mysql\20260520_system_nas_management_menu.sql script\deploy\restart-int-ruoyi-local.ps1 script\tests\test_system_nas_menu_sql.py script\tests\test_restart_int_ruoyi_local_schema.py doc\tasks\20260604-nas-menu-garbled-title` -> PASS，仅 CRLF 工作区提示，无 whitespace error。

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260604-nas-menu-garbled-title\bug-regression-evidence.md` -> PASS。

CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260604-nas-menu-garbled-title --mode preview` -> READY，keep `task.md` / `execution-log.md` / `bug-regression-evidence.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。
