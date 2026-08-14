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
- 2026-08-01：用户确认安全 upsert 策略：只同步清单明细字段，不直接复制本地关联 ID；无法唯一解析的关联 ID 保持为空或保留测试服已有安全映射。
- 2026-08-01：白名单 upsert 成功，MySQL affected rows `8616`；目标表最终 `7983` 行、业务键去重 `7983` 个。
- 2026-08-01：跨租户物料关联复核发现 `child_material_id=4610` 行、`product_id=365` 行不满足 tenant-safe 关联口径；已先创建 post-upsert 修正前快照，再将这些无效物料关联置空。
- 2026-08-01：修正前快照：`/var/lib/docker/intruoyi-data/runtime-data/task-backups/20260801-production-material-list-data-sync-test/mes_kingdee_production_material_list_post_upsert_pre_linkage_fix_20260801-012651.sql.gz`，SHA256 `dd1f5aba90abc69337cccf787a6388f66fcf90da5220273c41ae1459ef9c0c7c`，大小 `283611` bytes，`gzip -t` 通过。
- 2026-08-01：最终测试服临时 staging 表已删除，`information_schema` 核对剩余计数 `0`。

## Verification Evidence

- GREEN: local-remote-schema-readonly -> PASS，目标表 schema/索引/唯一键一致。
- GREEN: remote-target-backup -> PASS，测试服目标表备份 gzip/hash/大小验证通过。
- GREEN: staging-load-verify -> PASS，staging 行数与本地源一致，且加载后已清理。
- RED: safe-direct-upsert-preflight -> FAIL，本地关联 ID 与测试服主数据大量不匹配，且测试服按编码重算存在重复/缺失依赖，直接复制全部字段或静默重算 ID 均不安全。
- GREEN: whitelist-upsert -> PASS，测试服目标表 `7983` 行、业务键 `7983` 个，租户 `1/121/122/162` 分别为 `2593/1466/2458/1466`。
- GREEN: source-target-business-hash -> PASS，租户 hash 分别为 `c38e445268fd70e9ec82fa84f2266c4aa55b5fe112325a5872bb82062079da04`、`a9942cf1fe163866ff24285aedb94116f3351b04e6130a6b93aa1e5fc740e243`、`316f54d06c5cef952ad46680c5169b10491dd580c9942c971b3437546fdf18f5`、`a9942cf1fe163866ff24285aedb94116f3351b04e6130a6b93aa1e5fc740e243`，与本地一致。
- GREEN: tenant-safe-linkage-fix -> PASS，清空跨租户或不存在的物料关联后，`orphan_work_order/orphan_child_item/orphan_product_item/orphan_work_order_bom/bom_mismatch` 均为 `0`。
- GREEN: remote-health -> PASS，`http://172.30.30.58:48081/actuator/health` 返回 `{"status":"UP"}`。
- GREEN: production-material-list-api-sample -> PASS，租户 `1`、用户标签 `admin`，`group-page` 返回 `total=236`，样本单据 `PPBOM00309005`，`detail-list` 返回 `29` 行，首个子项编码 `A001.02.070.105`。
- GREEN: readonly-page-render-with-api-token-bootstrap -> PASS，使用已认证 token 引导只读页面访问 `/erp/production/material-list`，页面展示生产用料清单列表并打开样本单据明细；该证据不等同于登录页 E2E。
- RED: official-login-page-preflight -> FAIL，测试服登录页启用了验证码，无法执行无人值守真实登录页 E2E；未用 API-only 冒充登录页验证通过。
- GREEN: python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260801-production-material-list-data-sync-test\database-schema-evidence.md -> PASS。
- GREEN: python C:\Users\BJB110\.codex\skills\backup-disaster-recovery-readiness\scripts\validate_backup_disaster_recovery.py --evidence doc\tasks\20260801-production-material-list-data-sync-test\recovery-evidence.md -> PASS。
- GREEN: project-experience-consolidation -> PASS，已将本次“生产用料清单跨环境白名单 upsert 门禁”合并到 `docs/database-rules.md` 并更新 `docs/experience-index.md` 路由。
- GREEN: git diff --check -- doc/tasks/20260801-production-material-list-data-sync-test docs/database-rules.md docs/experience-index.md -> PASS，仅有 CRLF conversion warning，无 whitespace error。
- GREEN: task-closeout-cleanup preview -> PASS，keep 为 `task.md`、`execution-log.md`、`verification-report.md`；delete 为 staging SQL、`database-schema-evidence.md`、`recovery-evidence.md`；blocked/warnings 均为 none。
- GREEN: task-closeout-cleanup apply -> PASS，已删除本任务 staging SQL 和临时 evidence 文件，保留三份核心任务记录。

## Blockers

- 2026-08-01：数据同步与验证已完成；官方登录页 E2E 因测试服验证码阻塞，只保留 API 抽样与 token-bootstrap 只读页面渲染证据。
- 2026-08-01：仓库 closeout 仍阻塞：当前分支 `int_main...origin/int_main [ahead 10]`，且存在多项非本任务未提交/已暂存改动；已完成 cleanup apply，但未提交/未推送本任务文档，因此任务保持 `ready_for_closeout`。
