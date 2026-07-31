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
- 2026-08-01：本地库 `ruoyi-vue-pro` 与测试服库 `ruoyi-vue-pro` 均为 MySQL 8.0.39。
- 2026-08-01：本地与测试服 `mes_kingdee_production_material_list` 均存在，列数 `32`、列 hash `a0c1daa82199770dfe2cd6b2b2a4fb76e9910e7390e1195a096ee44c49b54008`、索引 hash `90f6a7f2999db47e90697754ea936e34a0b0d9df18fbcc76bbc5e0e5f4a216df`，唯一键为 `tenant_id,source_bill_no,production_order_no,production_order_line_no,child_material_code`。
- 2026-08-01：租户一致：`1:芋道源码`、`121:小租户`、`122:测试租户`、`162:瑛泰医疗`。
- 2026-08-01：本地源表 `7983` 行，按租户为 `1=2593`、`121=1466`、`122=2458`、`162=1466`；测试服目标表 `633` 行，按租户为 `1=219`、`121=138`、`122=138`、`162=138`。
- 2026-08-01：测试服功能前置存在：菜单计数 `3`、角色菜单计数 `6`、同步 Job `5607:2:kingdeeProductionMaterialListSyncJob:0 5/10 * * * ?`。
- 2026-08-01：测试服目标表备份完成：`/var/lib/docker/intruoyi-data/runtime-data/task-backups/20260801-production-material-list-data-sync-test/mes_kingdee_production_material_list_before_20260801-005623.sql.gz`，SHA256 `a35afce295013118dab19761130eeeb553aced1800215b8d7043e7ee58752a7e`，大小 `22920` bytes，`gzip -t` 通过。
- 2026-08-01：本地源数据已导出为 task-owned staging SQL：`doc\tasks\20260801-production-material-list-data-sync-test\artifacts\codex_pml_stage_20260801.sql`，SHA256 `0BFA3CE9866B671FAB6953632CA03068DB6905729E7E83EB318039A25748B1E6`，大小 `5196012` bytes；导入测试服临时 staging 后行数与本地一致 `7983/7983`，按租户 hash 一致。
- 2026-08-01：测试服临时 staging 表已删除，`information_schema` 核对剩余计数 `0`。

## Verification Evidence

- GREEN: local-remote-schema-readonly -> PASS，目标表 schema/索引/唯一键一致。
- GREEN: remote-target-backup -> PASS，测试服目标表备份 gzip/hash/大小验证通过。
- GREEN: staging-load-verify -> PASS，staging 行数与本地源一致，且加载后已清理。
- RED: safe-direct-upsert-preflight -> FAIL，本地关联 ID 与测试服主数据大量不匹配，且测试服按编码重算存在重复/缺失依赖，直接复制全部字段或静默重算 ID 均不安全。

## Blockers

- 需要用户确认是否继续执行“只同步清单明细字段，不直接复制本地关联 ID；无法唯一解析的关联 ID 保持为空或保留目标已有映射”的安全 upsert 方案。
