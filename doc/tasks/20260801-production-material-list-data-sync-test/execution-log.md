# Execution Log

## Intent

- 用户确认执行数据-only方案：先只读核对本地和测试服表结构/租户/目标表行数，再备份测试服相关表，最后做白名单表级 upsert，并抽样验证生产用料清单页面/API。

## BDD

- BDD: 生产用料清单数据同步 -> Given 本地库存在生产用料清单数据且测试服 schema 能承载，When 备份测试服目标表并执行白名单业务键 upsert，Then 测试服目标租户的生产用料清单行数、业务键集合和关键字段与本地一致。
- BDD: 缺失前置阻塞 -> Given 测试服缺表、缺列、缺唯一键、缺目标租户或备份失败，When 准备执行 upsert，Then 必须停止且不得写入远端数据库。

## Gate Log

- 2026-08-01：读取 `database-schema-delivery`、`backup-disaster-recovery-readiness` 技能及其 evidence contract。
- 2026-08-01：读取 `docs/server-access.md`、`docs/database-rules.md`、`docs/release-backup-restore.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/login-access.md`。
- 2026-08-01：确认本轮不打包、不发布代码；仅授权测试服务器 `172.30.30.58` 的数据同步链路。

## Read-Only Discovery

- 白名单初判：`mes_kingdee_production_material_list` 是生产用料清单业务数据表；`system_menu`、`system_role_menu`、`infra_job` 是功能入口/同步任务前置核对对象，不先纳入业务数据 upsert。

## Verification Evidence

- Pending.

## Blockers

- Pending read-only schema/tenant/count probe.

