# Execution Log

## User Intent

用户要求将 eDHR 批记录单元格链接改为“创建/打开执行记录时自动落库预填值”，并先进行文档设计。

## Evidence Reviewed

- 截图显示：批记录单元格链接配置页已把生产工单“生产批号”链接到粗洗工序生产记录的“生产批号”目标格；批次详情/预览中目标格仍为空。
- 问题分层结论：来源批号和链接规则均存在，异常点在创建/打开执行记录时没有把链接计算结果物化进 `cell_values_json`，因此只读预览和执行页读不到正式值。
- 后端现状：`MesProBatchRecordCellLinkServiceImpl#getPrefill` 已能按规则计算 `APPLICABLE` 和冲突状态，但只是返回预填结果。
- 后端现状：`MesProBatchRecordExecutionServiceImpl#openOrCreateByContext` 新建执行记录时 `cellValuesJson("[]")`，未把链接值物化进 `cell_values_json`。
- 前端现状：`ExecutionPage.vue#hydrateDraftState` 仅在 DRAFT 执行页把预填结果写入本地 draft 状态，未自动调用字段审计保存。
- 只读现状：`EdhrExecutionReadonlyForm.vue` 只读取 `formViewModel.cellValuesJson`，因此未落库的预填不会显示。

## BDD Scenarios

- `BDD: Auto-persist work order batch code on execution open -> Given` 生产工单 `batchCode` 有值且启用 `PRODUCTION_WORK_ORDER.batchCode` 到目标单元格的链接规则，`When` 创建或打开对应 DRAFT 执行记录，`Then` 目标单元格值写入 `cell_values_json`，字段审计哈希链更新，批次详情只读预览显示同一值。
- `BDD: Existing manual target value is not overwritten -> Given` 目标单元格已有人为保存值且规则 `overwritePolicy=ONLY_WHEN_EMPTY`，`When` 再次打开执行记录，`Then` 自动落库不覆盖原值，返回或记录 `TARGET_ALREADY_MANUAL` 冲突。
- `BDD: Missing production batch code fails fast -> Given` 链接规则启用但生产工单 `batchCode` 为空，`When` 创建或打开需要自动落库的执行记录，`Then` 后端返回明确缺源值错误或阻断状态，不写入空值、不用默认值。
- `BDD: Existing empty draft is repaired on task open -> Given` 历史 DRAFT 执行记录 `cell_values_json=[]` 且当前规则仍适用，`When` 用户通过工序任务打开该记录，`Then` 后端补齐空目标格并保持幂等。
- `BDD: Repeated open is idempotent -> Given` 同一规则版本和来源值已经自动落库，`When` 多次打开同一执行记录，`Then` 不重复追加审计批次、不改变目标值、不产生哈希链冲突。

## Design Decisions

- 将自动预填落库放在后端服务层，不再依赖前端 draft hydrate 作为保存来源。
- 自动落库必须走字段审计链语义，更新 `cell_values_json`、`cell_values_hash`、`field_audit_revision`、`field_audit_head_hash` 和字段审计明细。
- 系统来源自动预填不要求人工电子签名密码，但必须记录系统原因、规则 ID、规则版本、来源字段和触发点。
- `ONLY_WHEN_EMPTY` 保持为硬约束，已有目标值不覆盖。
- 生产工单来源批号缺失是正式前置条件缺失，不能写空字符串、不能继续展示为成功。
- 只读查询接口不承担写库副作用；创建执行记录和打开工序任务是阶段一明确写边界。

## Future RED/GREEN Plan

- `RED: mvn -pl yudao-module-mes -am -Dtest=MesProBatchRecordCellLinkAutoPersistServiceImplTest test -> FAIL, service not implemented and execution cell_values_json remains []`
- `GREEN: mvn -pl yudao-module-mes -am -Dtest=MesProBatchRecordCellLinkAutoPersistServiceImplTest test -> PASS`
- `GREEN: mvn -pl yudao-module-mes -am -Dtest=MesProBatchRecordExecutionServiceImplTest,MesProEdhrBatchExecutionServiceTest test -> PASS`
- `GREEN: node tests/e2e/edhr-cell-link-auto-persist-static.spec.js -> PASS`
- `GREEN: real Playwright eDHR batch execution path -> PASS, created/opened execution persists production batch code and read-only preview shows it`

## Verification Log

- `GREEN: python C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root E:\IntRuoyi\doc\tasks\20260727-edhr-cell-link-auto-persist-design -> PASS`
- `GREEN: git diff --check -- doc/tasks/20260727-edhr-cell-link-auto-persist-design -> PASS`
- `GREEN: project-experience-consolidation -> PASS, merged reusable lesson into docs/backend-development.md#批记录单元格链接预填落库边界 and indexed it in docs/experience-index.md`

## Blockers

- No blocker for design documentation.
- Implementation is intentionally outside this design task phase; any current worktree implementation diffs must be tracked and verified by the separate implementation task.
