# BDD Scenarios

## Purpose and Scope

本功能在个人工作台“配置”页签下新增“ERP表格自动同步”，用于管理 ERP/Kingdee 表格拉取同步计划。范围只包含同步计划配置、选择正式同步类型、触发现有同步 JobHandler、查看现有同步运行结果；不包含 NAS 导出、不新增 ERP 数据同步业务逻辑。

## Feature Scenarios

### 保存 ERP 表格自动同步配置

Given 用户具备查看个人工作台配置页签的权限  
When 用户打开“ERP表格自动同步”，选择每日开始时间，并勾选 ERP 商品、库存等同步类型后保存  
Then 系统保存启用状态、每日开始时间、所选同步类型和排序，并在重新打开页面时按正式配置回显

### 按所选类型执行自动同步

Given “ERP表格自动同步”已启用并选择了 ERP 商品和库存  
When 调度任务到达每日开始时间  
Then 系统只触发 `kingdeeProductItemSyncJob` 和 `kingdeeStockSyncJob`，并复用 `erp_kingdee_sync_run` 记录每类同步结果

### 与 NAS 自动同步隔离

Given 系统同时存在“NAS表格自动同步”和“ERP表格自动同步”  
When 用户分别配置两个页签  
Then NAS 页签只影响 NAS 导出表，ERP 页签只影响 Kingdee/ERP 拉取同步计划，两者不共用类型枚举和业务配置表

## Failure Scenarios

- Given 启用自动同步但缺少每日开始时间，When 保存配置，Then 后端返回业务错误，不保存为启用状态。
- Given 启用自动同步但未选择任何 ERP 表格，When 保存配置，Then 后端返回业务错误，不创建可执行调度计划。
- Given 提交不存在的同步类型，When 保存或执行计划，Then 后端 fail fast，不静默忽略或替换成默认类型。
- Given 对应 JobHandler 未加载，When 调度任务执行，Then 后端 fail fast 并记录失败状态，不返回默认成功。

## Boundary Scenarios

- 已禁用配置：dispatcher job 每分钟执行时返回 skipped，不触发任何 ERP 同步 JobHandler。
- 同一天重复执行：今天已自动执行后再次轮询返回 skipped，不重复触发所选同步类型。

## Open Questions

- 暂无阻塞型问题；默认使用个人工作台配置页签现有 `golden-finger` 权限边界。

## Test Blockers

- 若本地前端依赖、后端 Jar、数据库迁移或登录态缺失，真实 E2E 必须记录 blocker，不能用 API-only 替代页面验证。
