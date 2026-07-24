# 执行日志：修复测试服 system_users 缺失 password_update_time 字段

## 记录

- BDD: 测试服用户查询字段存在 -> Given 测试服后端使用当前代码查询 `system_users.password_update_time` / When 访问登录或用户查询路径 / Then 数据库表必须存在该字段，不应抛出 SQLSyntaxErrorException。
- BDD: 密码更新时间历史数据可读 -> Given 既有用户行在迁移前没有 `password_update_time` / When 执行字段补齐和回填 / Then 字段值应使用 `update_time`、`create_time` 或当前时间补齐，保证密码策略逻辑可判断。

## 待执行

- RED: `ssh root@172.30.30.58 "docker exec intruoyi-mysql mysql -uroot -p123456 -D ruoyi-vue-pro -N -e SHOW\ COLUMNS\ FROM\ system_users\ LIKE\ \'password_update_time\'\;"` -> FAIL, expected reason: 命令只返回 mysql 密码警告和 SSH socket 噪声，没有返回字段行，证明测试库缺少 `password_update_time`。
- RED: `ssh root@172.30.30.58 "docker exec intruoyi-mysql mysql -uroot -p123456 -D ruoyi-vue-pro -N -e SELECT\ COUNT\(\*\)\ FROM\ system_users\;"` -> PASS for reproduction context, result: `2147` rows.
- GREEN: `scp sql/mysql/20260525_system_password_policy.sql root@172.30.30.58:/tmp/20260525_system_password_policy.sql && ssh root@172.30.30.58 "docker exec -i intruoyi-mysql mysql -uroot -p123456 -D ruoyi-vue-pro < /tmp/20260525_system_password_policy.sql"` -> PASS, applied the existing non-destructive migration script to the test MySQL database.
- GREEN: `ssh root@172.30.30.58 "docker exec intruoyi-mysql mysql -uroot -p123456 -D ruoyi-vue-pro -N -e SHOW\ COLUMNS\ FROM\ system_users\ LIKE\ \'password_update_time\'\;"` -> PASS, result: `password_update_time datetime YES NULL`.
- GREEN: `ssh root@172.30.30.58 "docker exec intruoyi-mysql mysql -uroot -p123456 -D ruoyi-vue-pro -N -e SELECT\ COUNT\(\*\)\ FROM\ system_users\ WHERE\ password_update_time\ IS\ NULL\;"` -> PASS, result: `0`.
- GREEN: `ssh root@172.30.30.58 "curl -fsS http://127.0.0.1:48081/actuator/health"` -> PASS, result: `{"status":"UP"}`.
- GREEN: `python -m pytest script/tests/test_system_password_policy_sql.py` -> PASS, 2 tests.
- GREEN: `Invoke-RestMethod POST http://172.30.30.58:48081/admin-api/system/auth/login tenant-id=122 username=aoteman` -> PASS, result: `code=0`, `userId=113`, access token returned.
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260526-test-password-update-time-schema-fix/bug-regression-evidence.md` -> PASS, evidence valid.
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260526-test-password-update-time-schema-fix/database-schema-evidence.md` -> PASS, evidence valid.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260526-test-password-update-time-schema-fix --mode preview` -> PASS, status ready; keep `task.md` and `execution-log.md`; delete evidence docs; blocked none.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260526-test-password-update-time-schema-fix --mode apply` -> PASS, deleted temporary evidence docs and kept core task records.
