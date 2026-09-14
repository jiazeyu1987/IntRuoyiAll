# Bug Regression Evidence

## Bug Summary And Expected Behavior

点击 QA 规程“发布规程”时，外观项目的“检验器具及设备说明”填写为“目测”等文字说明，但没有正式设备台账选项。系统错误提示“外观已填写检验器具及设备说明，但未配置正式设备台账选项。”

Expected: 文字说明不等于正式设备必填。没有正式设备选项的项目应发布为 `equipmentRequired=false`；有正式设备选项的项目继续要求完整设备 ID、编码、名称和编号。

## Reproduction Command Or Path

Path: QA 规程配置页面 -> 检验项目 -> 外观项目填写“检验器具及设备说明”为“目测”且不添加正式设备 -> 点击“发布规程”。

Command: `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js`

## Root Cause

`IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue` 的 `buildQaRegulationItemEquipmentOptions` 将 `inspectionTool.trim()` 与正式设备台账选项数量绑定：只要工具说明非空且 `equipmentOptions` 为空就抛错。该逻辑把纯文字说明误当成正式设备必填条件。

## Regression Test Added Or Updated

Updated `IntRuoyiFronted/tests/e2e/pqc-item-equipment-standard-method-static.spec.js` with a contract that forbids publish validation from requiring formal equipment ledger options solely because the tool/equipment description is filled, and locks `equipmentRequired` to `equipmentOptions.length > 0`.

## RED Command And Expected Failure

RED: `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js` -> FAIL, expected reason: QA regulation publish still blocks when `inspectionTool.trim()` is filled and formal equipment options are empty.

## GREEN Command And Passing Result

GREEN: `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js` -> PASS.

## Verification

- `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js` -> PASS.
- QA regulation adjacent static tests -> PASS.
- `pnpm ts:check` -> PASS.
- `git diff --check` -> PASS with existing LF/CRLF warnings only.

## Risk And Regression Scope

Risk is limited to QA regulation publish serialization. Regression scope covers QA/PQC equipment option contracts, QA regulation adjacent static contracts, TypeScript type checking, and whitespace validation.

## Blockers And Follow-Up Actions

No blockers. No follow-up required for this bug.
