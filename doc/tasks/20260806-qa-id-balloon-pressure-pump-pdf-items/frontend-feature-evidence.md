# Feature

Goal: map `ID / 球囊扩张压力泵 / 112` to its formal QA regulation source `PQC-ID-001 (G/0) （椎体）球囊扩张压力泵组装过程检验规程`, with 17 PDF `5.1 检验内容` rows available in the QA regulation item table.

Non-goals: no backend API changes, no DCC project-code selector redesign, no change to the existing `IDI / PQC-IDI-001` 22-row template.

Owned files: `IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue`, QA static contracts under `IntRuoyiFronted/tests/e2e/`, and task evidence under `doc/tasks/20260806-qa-id-balloon-pressure-pump-pdf-items/`.

## Acceptance

- AC1: selecting a DCC project whose project code is `ID` and formal product ID is `112` loads `PQC-ID-001`, version `G/0`, effective date `2025-09-30`.
- AC2: `ID` product rule loading uses its own product-id cache and does not reuse `IDI / PQC-IDI-001` rows.
- AC3: the `PQC-ID-001` item template contains all 17 rows verified from PDF document pages 4-7.
- AC4: existing `IDI / PQC-IDI-001` 22-row contracts remain passing after adding the new `ID` template.

## BDD

- BDD: ID 产品使用独立 PQC-ID-001 规程 -> Given 用户在 QA 规程配置选择 `ID / 球囊扩张压力泵 / 112`, When 页面初始化或复制该产品 QA 检验项目, Then 系统应使用 `PQC-ID-001 (G/0)` 的检验项目、标准、方法、器具和抽样方案，而不是 `PQC-IDI-001` 的按压式压力泵模板。
- BDD: ID 与 IDI 模板互不串用 -> Given 系统同时支持 `ID` 和 `IDI` 两个压力泵类产品, When 用户分别选择两个产品, Then 两个产品的 QA 检验项目来源 PDF、产品代码和逐页项目合同应可区分并独立验证。

## RED

- RED: `node tests/e2e/qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs` -> FAIL, expected reason `ID balloon pressure-pump draft template must exist.`

## GREEN

- GREEN: `node tests/e2e/qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs` -> PASS
- GREEN: `node tests/e2e/qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs` -> PASS
- GREEN: `node tests/e2e/qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs` -> PASS
- GREEN: `node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs` -> PASS
- GREEN: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs IntRuoyiFronted/tests/e2e/qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs IntRuoyiFronted/tests/e2e/qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs doc/tasks/20260806-qa-id-balloon-pressure-pump-pdf-items` -> PASS
- GREEN: `node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS

## Verification

- Route/component: QA 规程配置 page `MesProProcessPoolQaRegulation`, component `QaRegulationPage.vue`.
- API contracts: unchanged; the frontend still reads formal DCC project-code data and resolves formal product IDs from `productMasterId`.
- Data states: `ID` and `IDI` products now have separate in-memory product rule drafts keyed by formal product ID.
- Loading/empty/error states: unchanged from existing QA page behavior; products without formal product ID still receive explicit empty rule/item profiles.
- Accessibility/responsive: unchanged UI layout; no template or style changes were made.
- Permission path: `role-matrix-qa-regulation-tab-static.spec.cjs` passed.

## Blockers

- `pnpm ts:check` through the bundled wrapper did not reach type checking because it attempted a non-interactive dependency-directory purge confirmation. Direct project script-equivalent `vue-tsc --noEmit -p tsconfig.relaxed.json` passed.
