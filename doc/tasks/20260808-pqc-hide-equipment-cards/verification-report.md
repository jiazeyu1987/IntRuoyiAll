# Verification Report

## Summary

- Result: PASS for targeted frontend static contract, TypeScript check, formatting check, and real browser E2E.
- Scope: Show PQC equipment and equipment-number cards when the inspection method has formal equipment options; hide them when it does not.

## Commands

- `node tests\e2e\pqc-item-equipment-standard-method-static.spec.js` -> RED FAIL before fix because the page did not expose `data-pqc-equipment-select`.
- `node tests\e2e\pqc-item-equipment-standard-method-static.spec.js` -> GREEN PASS after fix.
- `pnpm ts:check` -> PASS.
- `git diff --check -- IntRuoyiFronted\src\views\mes\pro\feedback\FrontlineFixedTemplatePanel.vue IntRuoyiFronted\tests\e2e\pqc-item-equipment-standard-method-static.spec.js docs\frontend-development.md docs\experience-index.md doc\tasks\20260808-pqc-hide-equipment-cards` -> PASS.
- `rg -n 'v-if="hasPqcEquipmentOptions\(activePqcTabItem\)"|data-pqc-equipment-card|data-pqc-equipment-number-card' IntRuoyiFronted\src\views\mes\pro\feedback\FrontlineFixedTemplatePanel.vue` -> PASS.
- `rg -n '无需检验设备|无需设备编号' IntRuoyiFronted\src\views\mes\pro\feedback\FrontlineFixedTemplatePanel.vue` -> PASS by no matches.
- `rg -n "有设备显示设备卡|无设备隐藏设备卡|20260808-pqc-hide-equipment-cards" docs\experience-index.md docs\frontend-development.md` -> PASS.
- `node --check doc\tasks\20260808-pqc-hide-equipment-cards\pqc-equipment-cards-real.e2e.cjs` -> PASS.
- `node doc\tasks\20260808-pqc-hide-equipment-cards\pqc-equipment-cards-real.e2e.cjs` -> PASS.
- `node tests\e2e\pqc-item-equipment-standard-method-static.spec.js` -> PASS after E2E script addition.
- `git diff --check -- IntRuoyiFronted\src\views\mes\pro\feedback\FrontlineFixedTemplatePanel.vue IntRuoyiFronted\tests\e2e\pqc-item-equipment-standard-method-static.spec.js docs\frontend-development.md docs\experience-index.md doc\tasks\20260808-pqc-hide-equipment-cards` -> PASS after E2E script addition, with LF/CRLF warnings only.

## Behavior Verified

- No-equipment inspection item: equipment and equipment-number cards are not rendered because both labels are guarded by `hasPqcEquipmentOptions(activePqcTabItem)`.
- Equipment-backed inspection item: equipment and equipment-number cards render because `hasPqcEquipmentOptions` returns true when formal `equipmentOptions.length > 0`.
- Placeholder removal: static contract rejects `无需检验设备` and `无需设备编号` in the fill page source.

## Real E2E

- Environment: local frontend `http://127.0.0.1:8081`, backend `http://127.0.0.1:48081`, actor label `芋道源码/admin`.
- Data source: the Playwright script used the page's own `active-orders` and `active-order/processes` responses, not API-only substitution; activeOrderCount=7, selected workOrderCode=`881MO090889`, processCount=14, inspectionItemCount=96.
- Equipment-backed sample: `RRM-PPV21-QA-001-RP928609 / 清洗-外观-抽检`, process `粗洗工序`, `PqcTaskId=289`, `PATROL 第 1 次`, `equipmentOptionCount=1`; UI showed both `data-pqc-equipment-card` and `data-pqc-equipment-number-card`.
- No-equipment sample: `RRM-PPV21-QA-FIRST-01-RP928609 / 粗洗-默认首检规则`, process `粗洗工序`, `PqcTaskId=288`, `FIRST 首检`, `equipmentOptionCount=0`; UI rendered no equipment cards/selects and no placeholder text.
- Network result: `pageErrors=[]`, `consoleErrors=[]`, target `requestFailures=[]`, target bad responses `[]`, and no `pqc/submit` writes. Observed `pqc/switch-employee` requests are page initialization employee-lock requests, not PQC submit.
- Evidence: `output\playwright\20260808-pqc-hide-equipment-cards\pqc-equipment-cards-real-e2e.json`, plus screenshots `withEquipment-881MO090889-RRM-PPV21-QA-001-RP928609.png` and `withoutEquipment-881MO090889-RRM-PPV21-QA-FIRST-01-RP928609.png`.
