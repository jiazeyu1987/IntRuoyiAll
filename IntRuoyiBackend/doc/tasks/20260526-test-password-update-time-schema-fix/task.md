# 任务：修复测试服 system_users 缺失 password_update_time 字段

## 任务目标

- 修复测试服务器登录/用户查询时报错 `Unknown column 'password_update_time' in 'field list'`。
- 核对测试库 `system_users` 表结构与当前后端代码、既有 MySQL 迁移脚本的一致性。
- 在不引入 fallback、不吞异常、不切换数据源的前提下，只补齐缺失 schema 并验证真实测试服恢复。

## 前序任务检查

- 后端上一任务目录：`doc/tasks/20260525-automation-2-ebr-visual-fidelity/`
- 状态：该任务文档已记录 closeout/自动合并阻塞，阻塞原因是 worktree/合并状态，不影响本次测试服 schema 修复启动。
- 处理策略：本任务仅处理 `system_users.password_update_time` 测试服缺失问题，变更与验证独立记录、独立提交。

## BDD 场景

- BDD: 测试服用户查询字段存在 -> Given 测试服后端使用当前代码查询 `system_users.password_update_time` / When 访问登录或用户查询路径 / Then 数据库表必须存在该字段，不应抛出 SQLSyntaxErrorException。
- BDD: 密码更新时间历史数据可读 -> Given 既有用户行在迁移前没有 `password_update_time` / When 执行字段补齐和回填 / Then 字段值应使用 `update_time`、`create_time` 或当前时间补齐，保证密码策略逻辑可判断。

## 里程碑

- [x] M1：确认代码和既有 SQL 迁移对 `password_update_time` 的定义。
- [x] M2：在测试服真实数据库复现字段缺失。
- [x] M3：执行最小非破坏性 schema 修复并回填历史数据。
- [x] M4：验证测试服字段存在、后端健康状态正常，记录证据。
- [x] M5：运行 task-closeout-cleanup 预览并按范围提交本任务文档。

## 预期验证

- RED：测试服 MySQL `SHOW COLUMNS FROM system_users LIKE 'password_update_time'` 返回空，证明当前错误的数据库前置条件缺失。
- GREEN：测试服 MySQL `SHOW COLUMNS FROM system_users LIKE 'password_update_time'` 返回字段定义。
- GREEN：测试服 MySQL 回填后 `password_update_time IS NULL` 的用户数为 0。
- GREEN：`http://172.30.30.58:48081/actuator/health` 返回可用状态。
- GREEN：测试服真实登录 API 使用测试租户 `122`、用户 `aoteman` 返回 `code=0`。
- GREEN：本地 SQL 回归测试 `python -m pytest script/tests/test_system_password_policy_sql.py` 通过。

## 当前状态

- 状态：已完成。
- 已完成：确认 `AdminUserDO`、密码策略代码、H2 测试表和 `sql/mysql/20260525_system_password_policy.sql` 均已声明 `password_update_time`；测试服真实 MySQL 字段探测为空，`system_users` 当前 2147 行；已执行既有迁移脚本并验证字段存在、空值数为 0、后端健康和真实登录 API 正常。
- 当前阻塞：无。

## Current Status

completed: test server schema was repaired with the existing migration, verification passed, and closeout cleanup preview found no blockers.
