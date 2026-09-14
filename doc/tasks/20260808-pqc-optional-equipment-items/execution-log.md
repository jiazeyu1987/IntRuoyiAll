# Execution Log

## User Intent

用户确认“一线 PQC 红框内容”需要与 QA 检验项目按产品和工序对应；进一步明确“有的检验项目是没有设备的”。因此正式规则是：PQC 检验项目必须按 QA 规程映射到对应产品/工序；其中无设备检验项目不得被强制选择设备。

## BDD

BDD: PQC 无设备检验项目正式提交 -> Given 已发布 QA 规程中某 PQC 检验项目属于当前产品、路线版本、路线工序和工序，且 `equipmentRequired=false`、设备选项为空；When 一线 PQC 页面展示并提交该检验项目；Then 页面不提示设备必填，提交载荷不强制设备 ID/编号，后端接受提交并保存设备快照为空的检验明细。

BDD: PQC 设备必填检验项目仍严格校验 -> Given 已发布 QA 规程中某 PQC 检验项目 `equipmentRequired=true` 且存在正式设备选项；When 一线 PQC 正式提交未选择设备或设备编号不匹配；Then 前端提交前阻断，后端服务也拒绝不合法设备身份。

## Evidence

- Source mapping inspected before this task: event `170` maps to work order `980026`, route `980091`, route process `980631`, process `922985`, active order `39`, route version `622`, product `924008` / `IDI` / `按压式球囊扩充压力泵`, process `粗洗工序`.
- QA regulation inspected before this task: regulation/version `36` is `PUBLISHED` and matches by product, route, route version, route process and process.
- QA items inspected before this task: `CODX-AO5-QA-FIRST`, `CODX-AO5-QA-PATROL`, `CODX-AO5-QA-FINAL` all have `equipmentRequired=false` and zero equipment options.

## RED

- RED: `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js` (workdir `IntRuoyiFronted`) -> FAIL, `FrontlinePqcItemResultSubmitReqVO` still requires `selectedEquipmentId: number` and `selectedEquipmentNumber: string`, so no-device QA items cannot omit equipment fields.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldSubmitPqcInspectionWithoutEquipmentForOptionalQaItems,MesQaPqcSchemaTest#pqcSubmitContractAndPieceSchemaMustFreezeItemEquipmentStandardSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false" test` (workdir `IntRuoyiBackend`) -> FAIL, request VO still has `@NotNull` on `ItemResult.selectedEquipmentId` and service throws `itemResults.pressure.selectedEquipmentId` for optional no-device QA items.

## GREEN

- GREEN: `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js` (workdir `IntRuoyiFronted`) -> PASS.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldSubmitPqcInspectionWithoutEquipmentForOptionalQaItems,MesQaPqcSchemaTest#pqcSubmitContractAndPieceSchemaMustFreezeItemEquipmentStandardSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false" test` (workdir `IntRuoyiBackend`) -> PASS, 2 tests, 0 failures.
- REGRESSION: `node tests/e2e/frontline-pqc-formal-submit-static.spec.js` -> PASS.
- REGRESSION: `node tests/e2e/mes-frontline-pqc-submit-to-leader-chain-static.spec.js` -> PASS.
- REGRESSION: `node tests/e2e/pqc-leader-list-fill-form-parity-static.spec.js` -> PASS.
- REGRESSION: `pnpm ts:check` (workdir `IntRuoyiFronted`) -> PASS.
- CHECK: `git diff --check -- <task-owned paths>` -> PASS.
- EXPERIENCE: Updated `docs/frontend-development.md`, `docs/backend-development.md`, and `docs/experience-index.md` with the `equipmentRequired=false` / no-device PQC project gate; `rg -n "equipmentRequired=false|无设备检验项目|无需设备" ...` found the new entries.
- CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-pqc-optional-equipment-items --mode preview` -> PASS, keep only `task.md`, `execution-log.md`, `verification-report.md`, no delete/blocked/warnings.
- CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-pqc-optional-equipment-items --mode apply` -> PASS, no deleted paths.

## Blockers

- None currently.
