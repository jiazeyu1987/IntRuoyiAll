# 执行日志：排产员工作台参数编辑权限修复并落测试服

BDD: 排产员角色保留工作台设置保存权限 -> Given 排产员角色拥有排产员工作台查询权限 / When 角色范围 SQL 收敛智能排产菜单 / Then 排产员仍保留 900170=mes:pro-scheduler-workbench:update，可编辑工作台参数。
BDD: 排产员角色不额外获得冒烟测试权限 -> Given 本次只修复参数编辑问题 / When 执行角色范围 SQL / Then 排产员默认不新增 900171=mes:pro-scheduler-workbench:smoke-test。
BDD: 本地与测试服应用同一正式 SQL 后行为一致 -> Given 本地库与测试服库都执行同一份幂等迁移 / When 回查角色菜单绑定 / Then 排产员角色在两端都拥有 5590 与 900170。
RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q` -> FAIL, 当前 `20260629_mes_smart_scheduling_role_scope.sql` 的排产员白名单未包含 `900170`，无法保证参数编辑权限。
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q` -> PASS，排产员白名单已补入 `900170`，且未额外加入 `900171`。
GREEN: `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260629_mes_smart_scheduling_role_scope.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro` -> PASS，本地运行库已应用同一份正式幂等 SQL。
GREEN: `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -N ruoyi-vue-pro < role-menu-query` -> PASS，本地 `排产员(role_id=910233)` 当前拥有 `5590` 与 `900170`，未拥有 `900171`。
GREEN: experience-preflight -> PASS，测试服写入前已读取 `docs/server-access.md` 与 `docs/release-backup-restore.md`，本次动作限定为对测试服务器执行同一份正式 SQL 并做只读回查。
BLOCKER-CLEARED: `ssh root@172.30.30.58 "docker exec -i int-ruoyi-mysql ..."` -> FAIL，测试服实际 MySQL 容器名不是 `int-ruoyi-mysql`，阻塞点为目标容器名判断错误，不是 SQL 或数据库失败。
GREEN: `ssh root@172.30.30.58 "docker ps --format '{{.Names}}\t{{.Image}}'"` -> PASS，确认测试服实际 MySQL 容器名为 `intruoyi-mysql`。
GREEN: `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260629_mes_smart_scheduling_role_scope.sql | ssh root@172.30.30.58 "docker exec -i intruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro"` -> PASS，测试服已应用同一份正式幂等 SQL。
GREEN: `ssh root@172.30.30.58 "docker exec -i intruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -N ruoyi-vue-pro < role-menu-query"` -> PASS，测试服 `排产员(role_id=910216)` 当前拥有 `5590` 与 `900170`，未拥有 `900171`。
