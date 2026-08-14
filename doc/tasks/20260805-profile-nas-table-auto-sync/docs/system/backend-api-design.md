# NAS 表格自动同步 Backend API Design

## Purpose and Scope

后端提供 ERP NAS 表格自动同步计划、运行日志、测试写入和立即执行接口。业务计划存储在 ERP 模块表中，调度复用 `infra_job` 与 Quartz，不新增第二套调度器。由于 `infra_job.handler_name` 是全局唯一，`infra_job.handler_param` 不承载业务配置；全局 Job 由 `@TenantJob` 逐租户扫描并执行当前租户已启用计划。

## Evidence Reviewed

- `ErpKingdeeSyncController` 使用 `/erp/kingdee-sync` 和 `@PreAuthorize("@ss.hasPermission('erp:kingdee-sync:query')")` 管控 ERP 同步查询。
- `JobService` 暴露 `createJob`、`updateJob`、`updateJobStatus`、`triggerJob`，`JobDO` 是 `@TenantIgnore` 的全局调度定义。
- 现有 ERP JobHandler 通过 `@Component("kingdeeStockSyncJob")` 等注册。
- 个人工作台配置页签既有后端设置接口使用 `mes:pro-batch-record-execution:golden-finger`，与前端配置页签可见权限一致。

## Modules

- ERP 模块新增 `controller.admin.nastablesync`、`service.nastablesync`、`dal.dataobject.nastablesync`、`dal.mysql.nastablesync`。
- Infra 模块扩展 NAS 浏览服务或新增 NAS 写入服务，提供正式 `writeFile` 能力。
- ERP 模块新增 JobHandler `erpNasTableAutoSyncJob`，负责在当前租户上下文中执行已启用计划。

## API Contracts

- `GET /erp/nas-table-sync/plan/get`：返回当前租户计划；不存在时返回默认 disabled 计划形态但不创建 Job。
- `PUT /erp/nas-table-sync/plan/save`：保存计划、计划明细和调度 Job，返回计划响应。
- `GET /erp/nas-table-sync/sync-types`：返回支持导出的 ERP 表类型，包括类型编码、中文名称和默认 sheet 名。
- `POST /erp/nas-table-sync/plan/test-nas-write`：按当前计划或请求中的 NAS 相对目录写入测试文件，返回目标路径。
- `POST /erp/nas-table-sync/plan/run-once`：手动执行当前计划，返回 runId 与执行状态。
- `GET /erp/nas-table-sync/run/page`：分页查看执行日志。

所有接口使用 `@PreAuthorize("@ss.hasPermission('mes:pro-batch-record-execution:golden-finger')")`，确保只有能看“配置”页签的人可访问。

## Error Model

- 计划不存在且执行手动同步：返回“NAS 表格自动同步计划不存在”。
- 未启用或未选择 ERP 表：保存允许 disabled 草稿；启用保存和执行必须 fail fast。
- NAS 配置缺失、认证失败、路径不可写、共享不存在：透传 infra NAS 明确错误。
- ERP 表类型不在支持清单中：返回 unsupported sync type 错误，不做空文件。
- JobHandler 不存在或 `infra_job` 保存失败：保存接口失败，计划不标记为已调度。

## Transactions and Idempotency

- 保存计划在事务内 upsert 主表和明细；Job 创建/更新失败时整体失败。
- 同一租户只允许一个 active plan；明细按 `plan_id + sync_type` 唯一。
- 执行 run 先插入 RUNNING，完成后更新 SUCCESS/FAILED；失败必须记录 `failure_message`。
- JobHandler 使用空 `handlerParam`，业务配置只从当前租户的 `erp_nas_table_sync_plan` 与明细表读取，避免多个租户共享同一业务配置。

## Open Questions

- 无开放问题；本轮先覆盖当前支持的 ERP 同步类型，新增类型需显式加入导出器清单。

## Design Blockers

- 如果无法实现正式 NAS 上传接口，不能用现有 `writeFileTo`，因为该方法语义是从 NAS 读出到输出流。
- 如果目标 ERP 表没有可审计导出器，不能用空 workbook 或 mock 行数冒充成功。
