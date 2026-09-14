# eDHR 单元格链接自动落库预填设计

## Task Goal

将“生产批号到粗洗工序生产记录单元格链接”从前端草稿态临时预填，设计为创建/打开执行记录时由后端自动落库预填值，确保批次详情、只读预览、执行页和审计链读取同一份已保存的 `cell_values_json`。

## Scope

- 本任务只进行文档设计，不修改生产代码、不改数据库、不运行后端或前端测试。
- 若当前工作区存在并行实现改动，本设计任务不把这些改动作为已完成证据；实现、测试、提交和发布必须进入单独实现任务闭环。
- 设计覆盖生产工单来源字段 `batchCode` 到批记录目标单元格的自动落库路径，并兼容现有单元格链接规则模型。
- 设计不引入 fallback、默认成功、前端补丁式兜底或静默跳过。

## Current Findings

- 现象：批号链接规则配置后，创建批次执行记录时目标单元格仍为空，截图中粗洗工序生产记录“生产批号”目标格未显示来源批号。
- 已核对事实：批次 `EDHRB-1785115017218` 的批次执行 `batch_code` 为 `34126020001`，生产工单 `881MO090889` 的 `batch_code` 也为 `34126020001`，说明批号来源本身存在。
- 已核对事实：链接规则存在且启用，来源为 `PRODUCTION_WORK_ORDER.batchCode`，目标为粗洗工序生产记录第 3 行第 3 列。
- 根因判断：现有后端 `openOrCreateByContext` 创建执行记录时把 `cellValuesJson` 初始化为 `[]`；现有前端仅在 DRAFT 执行页调用 `/mes/pro/batch-record-cell-link/prefill` 并写入本地 draft 状态，未自动保存到数据库。
- 设计结论：这不是“批号本来没有”的问题，而是“链接计算结果没有在创建/打开执行记录写入正式 `cell_values_json`”的问题。

## Milestones

- [x] 复盘现状链路和根因：区分“批号不存在”和“链接未落库”。
- [x] 设计后端创建/打开执行记录自动落库边界。
- [x] 设计数据模型、审计链、幂等和冲突处理。
- [x] 设计前端展示和错误状态调整。
- [x] 输出系统设计文档：后端 API、数据模型、前端、配置安全部署。
- [ ] 进入实现前补 RED/GREEN 测试和真实 E2E 验证。

## Expected Verification

- 设计文档结构校验：`python C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root E:\IntRuoyi\doc\tasks\20260727-edhr-cell-link-auto-persist-design`
- 文档 diff 校验：`git diff --check -- doc/tasks/20260727-edhr-cell-link-auto-persist-design`
- 后续实现必须补充后端单元测试、字段审计链测试、前端静态契约和真实 eDHR 路径 E2E；设计建议的关键回归入口包括 `MesProBatchRecordCellLinkAutoPersistServiceImplTest`、`MesProBatchRecordExecutionServiceImplTest`、`MesProBatchRecordExecutionFieldAuditServiceTest`、`MesProEdhrBatchExecutionServiceTest`。

## Current Status

ready_for_design_review

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。设计要求缺失生产工单、缺失生产批号、缺失目标执行记录、审计链冲突均显式失败或显式返回冲突状态，不允许前端空值兜底。
- `是否从根因和长期维护角度解决`：是。根因在后端执行记录保存链路未物化链接值，设计将落库动作收敛到后端服务边界，并保持字段审计哈希链一致。
- `是否存在临时补丁或绕过`：否。设计不要求直接 SQL 修数据、不要求前端把临时 draft 当保存结果、不跳过字段审计。

## 经验门禁

- `docs/backend-development.md#edhr-详情回填门禁`：配置页有值但详情为空时，必须核对来源字段、任务快照、详情组装链路和后端正式规则来源；禁止只改前端显示。
- `docs/backend-development.md#edhr-批记录版本治理规则运行态门禁`：涉及 `openOrCreateByContext` 和运行态批记录时，必须保持已发布版本治理证据和运行态规则校验，不得跳过治理或把 Jimu 当前 JSON 当已确认版本。
- `docs/backend-development.md#批记录单元格链接预填落库边界`：来源值和链接规则存在但目标格为空时，必须核对 `cell_values_json`、创建/打开执行记录写边界和字段审计链；禁止把 `/prefill` 或前端 draft hydrate 当作落库完成。
- `docs/e2e-rules.md#schema-backed-e2e-迁移与字段可选态门禁`：涉及单元格链接、`source_type`、`source_field_code`、`sourceFields` 时，真实 E2E 必须核对 schema、可见态、可选态和写请求证据，禁止 API-only 冒充页面通过。

## Design Documents

- `doc/tasks/20260727-edhr-cell-link-auto-persist-design/docs/system/backend-api-design.md`
- `doc/tasks/20260727-edhr-cell-link-auto-persist-design/docs/system/data-model.md`
- `doc/tasks/20260727-edhr-cell-link-auto-persist-design/docs/system/frontend-design.md`
- `doc/tasks/20260727-edhr-cell-link-auto-persist-design/docs/system/config-security-deployment.md`
