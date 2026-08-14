# NAS 表格自动同步 Data Model

## Purpose and Scope

本数据模型持久化每天自动导出的 ERP 表选择、NAS 输出规则、调度 Job 绑定和执行日志。数据模型位于 ERP 模块，NAS 账号密码继续由 infra NAS 配置保存。

## Evidence Reviewed

- `erp_kingdee_sync_run` 已记录 ERP 同步运行历史，但字段用于 Kingdee 增量同步，不适合承载 NAS 文件输出路径和逐表导出结果。
- `infra_job` 存在全局调度定义，适合被业务计划绑定。
- 现有 ERP 同步类型枚举为 `PRODUCT`、`STOCK`、`PURCHASE_ORDER`、`SALE_ORDER`、`PRODUCTION_ORDER`、`PRODUCTION_MATERIAL_LIST`、`BOM`。

## Entities

- `erp_nas_table_sync_plan`
  - `id`、`tenant_id`、`enabled`、`daily_start_time`、`cron_expression`、`nas_directory`、`file_name_pattern`、`job_id`、`last_run_id`、`last_status`、审计字段。
- `erp_nas_table_sync_plan_item`
  - `id`、`tenant_id`、`plan_id`、`sync_type`、`enabled`、`sort_order`、`sheet_name`、审计字段。
- `erp_nas_table_sync_run`
  - `id`、`tenant_id`、`plan_id`、`trigger_type`、`status`、`started_at`、`ended_at`、`output_path`、`total_table_count`、`success_table_count`、`failed_table_count`、`failure_message`、审计字段。
- `erp_nas_table_sync_run_item`
  - `id`、`tenant_id`、`run_id`、`sync_type`、`status`、`sheet_name`、`row_count`、`failure_message`、审计字段。

## Relationships

- 一个租户最多一个有效 plan。
- 一个 plan 包含多个 plan item，每个 `sync_type` 唯一。
- 一个 plan 可产生多个 run。
- 一个 run 包含多个 run item，每个导出表一条结果。
- plan 的 `job_id` 指向 `infra_job.id`，但 `infra_job` 不保存业务明细。

## State Models

- Plan：disabled、enabled；启用时必须具备每日开始时间、NAS 目录和至少一个 enabled item。
- Run：RUNNING、SUCCESS、FAILED。
- Run item：SUCCESS、FAILED。

## Migration Notes

- 新增 SQL 放在 `IntRuoyiBackend/sql/mysql/20260805_erp_nas_table_auto_sync.sql`。
- SQL 必须有 release migration metadata，包含 schema 与 job 变更说明。
- `infra_job` 种子写入 `erpNasTableAutoSyncJob` 时默认停用，用户保存启用计划后由服务更新为启用和目标 cron。

## Data Integrity Rules

- `tenant_id + deleted` 范围内最多一个未删除 plan。
- `plan_id + sync_type + deleted` 防重复。
- `run_id + sync_type + deleted` 防重复。
- `daily_start_time` 由后端校验为 HH:mm:ss；`cron_expression` 从时间派生，不接受前端任意 cron。

## Open Questions

- 无开放问题；多租户按当前请求租户隔离配置与运行日志。

## Design Blockers

- 未核对正式迁移结构或缺少迁移测试时，不能创建 DO/Mapper 后直接宣称 schema 完成。
