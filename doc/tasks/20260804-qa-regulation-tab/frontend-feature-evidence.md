# Frontend Feature Evidence

## Feature Goal and Non-Goals

- Goal: Provide a standalone `QA 规程配置` route page so QA can define PQC process-inspection rules from a pressure-pump regulation draft without being embedded in the production/PQC workbench internal tabs.
- Non-goal: Do not connect QA regulations to DCC classification, controlled-file upload, document-control approval, or any fake save/publish success.

## Requirements and Acceptance IDs

- Source plan: `doc/tasks/20260801-role-requirement-matrix-excel/development-plan.md`.
- Relevant acceptance: `AC-M09`, `AC-D15`, `AC-D16`, `AC-D17`, `AC-D18`, `AC-D19`, `AC-D20`, `AC-D21`, `AC-D22`, `AC-D23`.
- User correction: QA defines rules for PQC; QA has no relationship with DCC.

## UI Entry Points, Routes, Components, and Owned Files

- Entry point: Standalone route `/mes/pro/process-pool/qa-regulation`.
- Workbench boundary: `TeamLeaderWorkbenchPage.vue` keeps only `生产组长` and `PQC 组长` internal tabs.
- Owned frontend files:
  - `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
  - `IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue`
  - `IntRuoyiFronted/src/router/modules/remaining.ts`
  - `IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`

## API Contracts and Data States

- Current UI uses local draft state because no formal save/publish API is exposed in the inspected frontend page.
- The standalone page visibly states `正式保存/发布接口未接入` and `未写入后台`.
- QA draft lifecycle is shown as `DRAFT`; published immutable behavior remains a backend/API integration follow-up.

## BDD Scenarios

- BDD: QA 配置过程检验规程 -> Given QA 打开 `/mes/pro/process-pool/qa-regulation` When 独立 QA 页面加载 Then 页面展示规程元数据、适用范围、首检/巡检/末检规则和检验项目配置能力。
- BDD: 压力泵 PDF 初始化 -> Given QA 查看压力泵过程检验规程示例 When 页面加载 Then 能看到 `PQC-IDI-001`、`B/0`、`2026-01-04`、`按压式球囊扩充压力泵组装过程检验规程` 等 PDF 来源信息。
- BDD: QA/PQC 边界 -> Given PQC 只执行 QA 发布规则 When QA 独立页面展示配置能力 Then 页面不出现 DCC 文件分类、受控文件上传或文控审批语义。
- BDD: 发布完整性检查 -> Given QA 规程尚未正式接入发布接口 When 查看独立页面 Then 页面提示发布前必须完成范围、项目、抽样规则、判定标准和版本冻结检查，不伪造保存成功。
- BDD: 检验项目原文依据 -> Given QA 查看解析后的检验项目 When QA 需要复核判定标准 Then 页面展示该项目相关的短原文摘录、页码、原文项目名和检验方法摘录。
- BDD: QA 独立页面入口 -> Given QA 需要独立工作入口 When QA 打开 `/mes/pro/process-pool/qa-regulation` Then 页面直接展示 QA 规程配置，生产/PQC 工作台内部不再展示 `QA 规程` tab。

## BDD to TDD Mapping

| BDD Scenario | RED Expected Failure | Minimal GREEN Target | Regression / Refactor Check |
| --- | --- | --- | --- |
| QA 配置过程检验规程 | No standalone QA route/page or stable QA selectors exist. | Add `QaRegulationPage.vue`, stable root selector, scope, rules, items, completeness, and PQC preview blocks. | New static contract and `ts:check` pass. |
| 压力泵 PDF 初始化 | Source metadata is absent. | Show title, code, version, effective date, and QA type from reliable PDF filename/cover metadata. | New static contract checks source metadata. |
| QA/PQC 边界 | QA block could drift into DCC classification or controlled-file language. | Add explicit QA/PQC ownership copy and negative DCC coupling assertions. | Static contract forbids DCC/file-classification terms inside QA block. |
| 发布完整性检查 | Page could imply successful persistence despite missing formal API. | Show API-not-wired warning, local draft messaging, and publish precheck blockers. | Evidence validator and static contract pass. |
| 检验项目原文依据 | Item rows only show parsed standards without the corresponding PDF original wording. | Add item-level source page, source item, acceptance-standard excerpt, and method excerpt UI. | Static contract and real browser E2E verify excerpts are present, scoped to the item, and read-only. |
| QA 独立页面入口 | QA remains embedded as an `el-tab-pane` in `TeamLeaderWorkbenchPage.vue`; no standalone route/page exists. | Add `QaRegulationPage.vue`, route `/mes/pro/process-pool/qa-regulation`, and remove QA from workbench tabs. | Static contract and real browser E2E open the standalone route directly. |

## RED Command and Expected Failure

- RED: `node IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL, expected reason: existing workbench did not expose the required QA configuration entry; later standalone contract failed until `QaRegulationPage.vue` and `/mes/pro/process-pool/qa-regulation` were added.

## GREEN Command and Passing Result

- GREEN: `node IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted run e2e:role-matrix-qa-regulation:static` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted run e2e:role-matrix-pqc-dynamic-form:static` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted ts:check` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-qa-regulation-tab/frontend-feature-evidence.md` -> PASS.
- RED/GREEN: `node IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` covers original-source excerpt fields and UI.
- GREEN: `E:\IntRuoyi\IntRuoyiFronted` `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS after adding item-level original-source excerpts.
- GREEN: `E:\IntRuoyi\IntRuoyiFronted` `pnpm run e2e:role-matrix-qa-regulation:static` -> PASS after adding item-level original-source excerpts.
- GREEN: `E:\IntRuoyi\IntRuoyiFronted` `pnpm run e2e:role-matrix-pqc-dynamic-form:static` -> PASS after adding item-level original-source excerpts.
- GREEN: `E:\IntRuoyi\IntRuoyiFronted` `pnpm run ts:check` -> PASS after adding item-level original-source excerpts.
- GREEN: `E:\IntRuoyi\IntRuoyiFronted` `node --check tests\e2e\role-matrix-qa-regulation-original-excerpt-real.e2e.cjs` -> PASS.
- GREEN: `E:\IntRuoyi\IntRuoyiFronted` `node tests\e2e\role-matrix-qa-regulation-original-excerpt-real.e2e.cjs` -> PASS, `sourceExcerptCount=5`, `writeRequests=[]`, `consoleErrors=[]`, `pageErrors=[]`.
- GREEN: standalone split contract -> PASS; `TeamLeaderWorkbenchPage.vue` no longer contains a `QA 规程` internal tab, and `/mes/pro/process-pool/qa-regulation` loads `QaRegulationPage.vue`.

## Test Data

- Pressure-pump PDF path: `C:/Users/BJB110/Desktop/文档/1/PQC-IDI-001（B 0）按压式球囊扩充压力泵组装过程检验规程--2026.01.04生效.pdf`.
- Reliable source metadata: `PQC-IDI-001`, `B/0`, `2026-01-04`, `按压式球囊扩充压力泵组装过程检验规程`.
- QA editable defaults: appearance, assembly completeness, seal/leak, pressure display/holding, label/batch confirmation.
- Sampling sample: order quantity `301`, patrol ratio `5%`, rounded-up quantity `16`.
- Original excerpts: manually transcribed from visually rendered scanned PDF pages 3, 6, 7, and 8; direct text extraction returned empty text, so excerpts remain short and QA-reviewable.

## Responsive, Accessibility, Loading, Empty, Error, and Permission Checks

- Responsive: QA layout uses two-column grid and collapses to one column under 1180px.
- Accessibility: form labels, table headers, tags, alerts, and stable data selectors are present for the standalone page.
- Loading: QA local draft page does not trigger existing production/PQC workbench list loading.
- Empty/error: publish precheck lists missing scope, version, rules, items, and numeric limits instead of silently succeeding.
- Permission: standalone route currently reuses the existing process-pool query permission until a formal QA regulation permission/menu is defined.

## E2E or Component Verification Path

- Static contract verifies the standalone page structure, pressure-pump source, rule sections, item model fields, publish checks, PQC task preview, and no DCC coupling.
- Static contract verifies the `原文依据` column, source page/item/excerpt/method fields, and representative source excerpts from scanned PDF pages 3, 6, 7, and 8.
- Real browser E2E was not run in `D:\IntRuoyiWorktree\2020804_qa` because runtime slot reservation failed for profile `int_main`: no available slot in range `1..19`.
- Refreshed local `E:\IntRuoyi` browser E2E for the new source-excerpt column passed on `8081/48081`; it verified PDF page/source item/excerpt/method content, no DCC coupling terms, no backend write requests, and no browser errors.

## Blockers and Follow-Up Skills

- Formal persistence/publish API is not wired in this UI slice; the standalone page states this explicitly and does not fake backend success.
- Worktree browser/runtime verification is blocked until an `int_main` worktree slot is released or formally assigned.
