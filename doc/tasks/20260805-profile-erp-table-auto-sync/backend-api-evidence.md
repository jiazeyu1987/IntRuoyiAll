# Backend API Evidence

## Scope

- Controller: `ErpKingdeeTableAutoSyncController`
- Service: `ErpKingdeeTableAutoSyncService`
- Dispatcher Job: `erpKingdeeTableAutoSyncJob`
- API:
  - `GET /erp/kingdee-table-auto-sync/plan/get`
  - `PUT /erp/kingdee-table-auto-sync/plan/save`
  - `GET /erp/kingdee-table-auto-sync/sync-types`
  - `POST /erp/kingdee-table-auto-sync/plan/run-once`
  - `GET /erp/kingdee-table-auto-sync/run/page`
  - `GET /erp/kingdee-table-auto-sync/watermark/list`

## API And Data Contract

- 配置按租户保存启用状态、每日开始时间、CRON、关联 Job、最近执行状态和所选同步类型。
- 正式同步类型来自 `ErpKingdeeTableAutoSyncTypeEnum`，支持 `PRODUCT`、`STOCK`、`PURCHASE_ORDER`、`SALE_ORDER`、`PRODUCTION_ORDER`、`PRODUCTION_MATERIAL_LIST`、`BOM`。
- 同步执行复用已有 Kingdee JobHandler、`erp_kingdee_sync_run` 和 `erp_kingdee_sync_watermark`，不复制 ERP 数据同步逻辑。
- 新增配置表 `erp_kingdee_table_auto_sync_plan`、`erp_kingdee_table_auto_sync_plan_item`，并注册 dispatcher Job 5609。
- 自动执行只有在全部所选 handler 成功后才写入 `lastAutoRunDate`，失败当天允许再次调度重试。

## Auth And Validation

- 权限：`mes:pro-batch-record-execution:golden-finger`。
- 启用配置时必须提供每日开始时间并至少选择一个正式同步类型。
- 未知同步类型、缺失 JobHandler、数据库迁移缺失或执行失败时 fail fast，不返回默认成功。
- dispatcher 在租户配置禁用、未到执行时间或当天已成功执行时返回明确 skipped 结果，不触发 ERP handler。

## Required Runtime

- 本地 MySQL：`127.0.0.1:23306/ruoyi-vue-pro`。
- 本地 Redis：`127.0.0.1:26379`。
- 迁移：`IntRuoyiBackend/sql/mysql/20260805_erp_kingdee_table_auto_sync.sql`。
- 已有 Kingdee 同步运行表、水位表和七个正式 JobHandler。

## BDD

- BDD: 保存 ERP 表格自动同步配置 -> Given 用户具备权限，When 保存时间和所选 ERP 表格，Then 系统按租户持久化并返回正式配置。
- BDD: 只执行所选类型 -> Given 配置选择商品和库存，When 到达调度时间，Then 只触发对应正式 JobHandler。
- BDD: 自动失败可重试 -> Given 某个所选 handler 失败，When 当天再次轮询，Then 不因错误写入 `lastAutoRunDate` 而跳过重试。
- BDD: 权限与输入失败 -> Given 用户无权限或提交未知类型，When 调用配置接口，Then 系统明确拒绝且不写入配置。

## TDD And Verification

- RED: Maven 合同测试到达目标测试后失败，原因是 controller、service、job、类型枚举和成功后日期语义尚未实现。
- GREEN: `mvn -pl yudao-module-erp -am "-Dtest=cn.iocoder.yudao.module.erp.kingdeeautosync.ErpKingdeeTableAutoSyncContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，4 tests / 0 failures / 0 errors。
- GREEN: `python -X utf8 -m pytest script\tests\test_erp_kingdee_table_auto_sync_sql.py` -> PASS，4 passed。
- REGRESSION: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS，生成包含本次 ERP 模块的 `yudao-server-exec.jar`。
- RUNTIME: `http://127.0.0.1:48083/actuator/health` -> `UP`。
- DATABASE: 本机开发库确认两张配置表、两个唯一键和 Job 5609 均存在；页面保存后确认租户 1 的时间、所选类型和 CRON 与页面一致。

## Observability

- 配置表记录最近自动日期、最近运行时间、状态和信息。
- 正式运行记录继续写入 `erp_kingdee_sync_run`，水位继续写入 `erp_kingdee_sync_watermark`。
- 后端启动日志确认 `erpKingdeeTableAutoSyncJob` 已同步到 Quartz。

## Blockers And Downstream Needs

- 无后端交付 blocker。
- 真实 E2E 未点击“立即执行一次”，避免对真实 Kingdee 连接产生数据拉取副作用；该接口由后端合同测试覆盖，不属于配置保存 E2E 的完成门禁。
