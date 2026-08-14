# Execution Log

## Intent

- 用户指出一线 PQC 页面当前卡片主标题显示 `AO5 final inspection`，实际应显示“目视检验”。

## BDD

- BDD: 当前检验标题显示检验方法 -> Given PQC 当前检验项目为 AO5 终检且正式检验方法为 Visual inspection/目视检验, When 一线 PQC 当前检验卡片渲染, Then 主标题显示“目视检验”，不显示检验项名称 `AO5 final inspection`。
- BDD: 提交身份不被标题展示修改 -> Given 当前检验项目有正式 itemCode/itemName 和 inspectionMethod, When 构造提交明细, Then itemCode/itemName 仍按正式检验项目传递，inspectionMethod 单独传递。

## Evidence

- Task directory created before source/test changes.
- Applied gates copied from `docs/experience-index.md`, `docs/frontend-development.md`, and `docs/backend-development.md`.
- Copy scan: `python -X utf8 C:\Users\BJB110\.codex\skills\clear-frontend-copy\scripts\scan_frontend_copy.py --root E:\IntRuoyi\IntRuoyiFronted\src\views\mes\pro\feedback --format json` -> scanned 10 files; existing unrelated findings remain, current task scoped only to PQC active title and method display.
- RED: `node tests/e2e/pqc-active-title-method-display-static.spec.cjs` -> FAIL, expected reason: active panel title rendered `<h3>{{ activePqcTabItem.label }}</h3>`.
- Implementation: `FrontlineFixedTemplatePanel.vue` active panel title and aria label now use `formatPqcInspectionTitle(activePqcTabItem)`; `formatPqcInspectionTitle` reads the same normalized `inspectionMethod` used by the 检验方法 card; `Visual inspection` displays as `目视检验`.
- Implementation hardening: normalization also covers case variants such as `Visual Inspection` by comparing the trimmed lower-case method label.
- Identity preservation: `pqcInspectionItems` still maps `key` from `item.itemCode` and `label` from `item.itemName || item.itemCode`; tab labels and `buildPqcItemDetailsPayload` still pass itemCode/itemName separately from inspectionMethod.
- GREEN: `node tests/e2e/pqc-active-title-method-display-static.spec.cjs` -> PASS.
- REGRESSION: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js` -> PASS.
- REGRESSION: `node tests/e2e/frontline-formal-submit-static.spec.cjs` -> PASS.
- REGRESSION: `node tests/e2e/frontline-pqc-formal-submit-static.spec.js` -> PASS.
- DIFF CHECK: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-active-title-method-display-static.spec.cjs doc/tasks/20260807-pqc-active-title-method-display` -> PASS; only warning is existing LF-to-CRLF notice for the already dirty Vue file.
- BLOCKED UNRELATED: `node tests/e2e/role-matrix-qa-regulation-static.spec.cjs` -> FAIL at existing M6 fixture assertion `must freeze the task-owned PQC task ids before resetting them to PENDING`; failure is outside current title display change.
- Project experience consolidation: existing `docs/frontend-development.md` and `docs/backend-development.md` gates already cover user-visible display vs internal identity and PQC formal method facts; no new long-term document was created, avoiding concurrent edits to already dirty long-term docs.
- CLEANUP PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260807-pqc-active-title-method-display --mode preview` -> PASS; keep task.md, execution-log.md, verification-report.md; delete/blocked/warnings all `<none>`.
- CLEANUP APPLY: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260807-pqc-active-title-method-display --mode apply` -> PASS; no deleted paths, blocked or warnings.
- FINAL RECHECK: `node tests/e2e/pqc-active-title-method-display-static.spec.cjs` -> PASS; `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js` -> PASS; `node tests/e2e/frontline-pqc-formal-submit-static.spec.js` -> PASS; final `git diff --check -- ...` -> PASS with only the existing LF-to-CRLF notice.
- FINAL STATUS: task marked `completed`; Git commit/push not performed because project Git policy requires explicit user request.
