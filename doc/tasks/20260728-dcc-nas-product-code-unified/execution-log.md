# Execution Log

## User Intent

- 用户要求按已确认计划实施 DCC/NAS 产品编号统一口径：除展厅、MDM 自身管理模块外，DCC/NAS 产品编号只来自 DCC 项目代码。
- 用户确认保留历史 `productMasterId/product_master_id` 字段用于旧数据展示和兼容读取，但所有 DCC/NAS 新写入必须写 `null`。

## Initial Environment

- 工作区：`E:\IntRuoyi`
- 分支：`int_main`
- 触发规则已读：`docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/backend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- 使用技能：`frontend-feature-delivery`、`backend-api-delivery`。
- `GREEN: experience-preflight -> PASS, 已读取 docs/experience-index.md，并应用严格无 fallback、前端静态契约隔离、E2E 真实路径和 Git 脏工作区基线门禁。`

## Git Baseline

- `BASELINE: git commit a8ad9591 -> PASS, chore: baseline existing dirty workspace before dcc nas product code work。`
- 基线文件：`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordRuntimeSnapshotSupport.java`、`doc/tasks/20260728-edhr-batch-record-design-docs/task.md`、`doc/tasks/20260728-rename-product-master-tab/task.md`、`doc/tasks/20260728-rename-product-master-tab/execution-log.md`。

## BDD Scenarios

- `BDD: DCC/NAS 新写入使用 DCC 项目代码 -> Given 用户在 DCC/NAS 写入链路选择 DCC 项目 / When 提交、更新、导入或转移文件 / Then 后端以项目 projectCode/projectName 写入 productCode/productName，productMasterId 写入 null。`
- `BDD: 前端不再选择产品主数据 -> Given 用户打开 DCC 外来评审、元数据弹窗或 NAS 导入 / When 需要产品编号 / Then 页面提供 DCC 项目选择，并只读自动生成产品编号，不加载 DCC product-options。`
- `BDD: 历史字段只读兼容 -> Given 旧记录存在 productMasterId / When 页面查看或接口返回历史记录 / Then 响应字段可保留，但新写请求不得把 productMasterId 作为输入来源。`

## RED Evidence

- Pending。

## GREEN Evidence

- Pending。

## Blockers

- Pending。
