# ERP 表格自动同步改用 Job 同步链路

## Task Goal

将个人工作台配置页签下的 `ERP表格自动同步` 改为复用生产工单同款 ERP 增量同步方式：通过 `infra/job` 查询、更新、启停正式 Job，通过 `ErpKingdeeSyncApi.runIncrementalSyncJob(handlerName)` 立即执行，不再调用 `/erp/kingdee-table-auto-sync/**`。

## Milestones

- [x] 设计与 BDD/TDD 合同：记录用户行为、正式 API 边界和 RED 静态合同。
- [x] 前端实现：Profile 配置页按固定 ERP 表格类型展示 Job、每日时间、启停选择和立即执行。
- [x] 验证与回归：运行目标静态合同、相邻 NAS 页签合同、ERP 手动增量同步合同和证据校验。
- [x] 收尾：归档验证报告，确认无临时补丁、无 fallback、无旧接口调用。

## Expected Verification

- `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js`
- `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js`
- `node IntRuoyiFronted\tests\e2e\erp-manual-incremental-sync-buttons-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-profile-erp-table-sync-use-job-api/frontend-feature-evidence.md`
- `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-use-job-api`

## Current Status

completed

## 设计方案

- 配置入口：继续放在个人工作台 `配置` 页签下的 `ERP表格自动同步` 子页签，沿用能看到配置页签的权限边界。
- 数据来源：固定使用当前 ERP 增量同步面板已确认的 7 类表格和 handlerName，不再从旧 `kingdee-table-auto-sync` 接口获取类型列表。
- 调度配置：加载时按 handlerName 调用 `JobApi.getJobPage` 找正式 Job；缺失任一 Job 时 fail fast 显示错误。
- 保存配置：把所选每日开始时间转换为 Quartz Cron，调用 `JobApi.updateJob` 更新每个正式 Job 的 cron，再用 `JobApi.updateJobStatus` 启用所选表格、停用未选表格。
- 立即执行：对当前选中的 ERP 表格逐个调用 `ErpKingdeeSyncApi.runIncrementalSyncJob(handlerName)`，与生产工单页面保持同一提交 Job 的方式。
- 运行展示：运行记录与水位继续使用正式 `/erp/kingdee-sync/run/page` 和 `/erp/kingdee-sync/watermark/list`，并保持中文状态、中文触发类型和可读时间展示。

## BDD Scenarios

- BDD: Profile ERP table sync loads formal jobs -> Given 用户打开个人工作台配置页签的 ERP 表格自动同步, When 页面加载配置, Then 页面按 7 个正式 handlerName 查询 `infra/job` 并显示 Job 状态、水位和执行记录，且不访问 `/erp/kingdee-table-auto-sync/**`。
- BDD: Profile ERP table sync saves daily schedule -> Given 用户选择每日开始时间和 ERP 表格, When 点击保存配置, Then 系统更新对应正式 Job 的 cron，并启用所选 Job、停用未选 Job。
- BDD: Profile ERP table sync submits selected jobs once -> Given 用户已选择需要同步的 ERP 表格, When 点击立即执行一次, Then 系统通过 `ErpKingdeeSyncApi.runIncrementalSyncJob(handlerName)` 提交正式增量同步任务。
- BDD: Missing sync job fails fast -> Given 任一 ERP 同步 handlerName 没有正式 Job, When 页面加载或保存, Then 页面显示 `未找到同步任务处理器`，不得用旧接口或默认成功掩盖。

## TDD Sequence

- RED: 更新 `profile-erp-table-auto-sync-static.spec.js`，先要求组件使用 `@/api/erp/sync`、`@/api/infra/job`、`InfraJobStatusEnum`，并禁止旧 `/erp/kingdee-table-auto-sync/**`。
- GREEN: 修改 `ProfileErpTableAutoSyncSetting.vue`，实现 Job 查询、cron 更新、状态启停、立即执行和中文展示。
- REGRESSION: 复跑 Profile ERP 合同、NAS 页签合同和 ERP 手动增量同步合同。

## 经验门禁

- 前端静态契约隔离门禁：本任务使用最小静态合同证明当前组件旧接口 RED/GREEN，避免被无关全量 `ts:check` 历史问题干扰。
- 业务运行记录用户可读展示门禁：运行记录不得直出 `AUTO/MANUAL`、`10/20/30`、毫秒时间戳或英文内部字段名。
- ERP 表格同步 Job 链路门禁：ERP 表格同步配置入口必须复用 `ErpKingdeeSyncApi.runIncrementalSyncJob(handlerName)` 和 `infra/job`，不得调用旧 `/erp/kingdee-table-auto-sync/**`。
- 技能证据文件清理前归档门禁：`frontend-feature-evidence.md` 通过 validator 后，关键 PASS 和结论必须复制到 `verification-report.md`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺失正式 Job 或 API 报错时显示错误。
- `是否从根因和长期维护角度解决`：是；根因是前端调用了旧的 ERP 模块自动同步接口，改为复用正式 Job 同步链路。
- `是否存在临时补丁或绕过`：否。
