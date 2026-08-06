# Test Data

## Required Test Data

- 具备个人工作台配置页签权限的测试账号。
- 当前测试租户已应用 `erp_kingdee_sync_run`、`erp_kingdee_sync_watermark` 和本任务新增配置表迁移。
- 所选同步类型至少包含 `PRODUCT`、`STOCK`。
- 若执行“立即执行一次”，必须具备真实 Kingdee/ERP 测试连接配置和可同步数据。

## Reset Procedure

- 配置 E2E 结束后将自动同步计划改回禁用。
- 不直接删除正式运行记录和水位。
- 不清理其它租户或其它用户配置。
