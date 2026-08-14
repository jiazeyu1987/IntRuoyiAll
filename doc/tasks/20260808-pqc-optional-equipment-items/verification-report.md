# Verification Report

## Result

PASS. 一线 PQC 现在按发布 QA 检验项目的 `equipmentRequired` 判断设备是否必填：无设备项目不显示/不强制设备选择，提交载荷可省略设备 ID/编号，后端保存逐件明细时设备快照为空；设备必填项目仍按正式项目设备选项校验。

## Scope Verified

- 前端映射：`FrontlineFixedTemplatePanel.vue` 不再把所有 QA 项目映射为 `equipmentRequired: true`，只在 `item.equipmentRequired !== false` 时要求设备。
- 前端提交：`FrontlinePqcItemResultSubmitReqVO.selectedEquipmentId/selectedEquipmentNumber` 改为可选；提交 payload 只在存在正式匹配设备时写入设备字段。
- 后端请求：`MesFrontlinePqcSubmitReqVO.ItemResult` 不再用 Bean Validation 全局强制设备字段必填。
- 后端服务：`MesFrontlinePqcContextServiceImpl` 对 `equipmentRequired=false` 且未提交设备的项目返回空设备快照；提交了设备字段时仍必须匹配正式设备选项。
- 结构合同：schema 测试锁定现有 nullable 迁移，确保 PQC 明细设备快照列可为空。

## Commands

- RED: `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js` -> FAIL, 前端类型仍要求设备字段。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldSubmitPqcInspectionWithoutEquipmentForOptionalQaItems,MesQaPqcSchemaTest#pqcSubmitContractAndPieceSchemaMustFreezeItemEquipmentStandardSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 请求 VO 与服务层仍要求设备字段。
- GREEN: `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js` -> PASS.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldSubmitPqcInspectionWithoutEquipmentForOptionalQaItems,MesQaPqcSchemaTest#pqcSubmitContractAndPieceSchemaMustFreezeItemEquipmentStandardSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- Regression: `node tests/e2e/frontline-pqc-formal-submit-static.spec.js` -> PASS.
- Regression: `node tests/e2e/mes-frontline-pqc-submit-to-leader-chain-static.spec.js` -> PASS.
- Regression: `node tests/e2e/pqc-leader-list-fill-form-parity-static.spec.js` -> PASS.
- Regression: `pnpm ts:check` -> PASS.
- Check: `git diff --check -- <task-owned paths>` -> PASS.
- Closeout: `task_closeout.py --mode preview` -> PASS, no delete/blocked/warnings.
- Closeout: `task_closeout.py --mode apply` -> PASS, no deleted paths.

## Notes

- 未执行真实写入型 Playwright E2E；本次已有前端静态合同、后端服务回归、请求/schema 合同和类型检查覆盖该规则。
- 当前工作区存在大量非本任务既有脏改动；本任务未回退、暂存或提交它们。
