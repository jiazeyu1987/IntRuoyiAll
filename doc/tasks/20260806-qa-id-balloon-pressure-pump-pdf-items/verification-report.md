# Verification Report

## Scope

- Verified `ID / 球囊扩张压力泵 / 112` now uses a dedicated `PQC-ID-001 (G/0)` QA regulation template.
- Verified the new template contains 17 inspection rows from `PQC-ID-001` document pages 4-7 and does not reuse `PQC-IDI-001` item content.
- Verified the existing `IDI / 按压式球囊扩充压力泵` 22-row template remains separately validated.

## Results

- PASS: `node tests/e2e/qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs`
- PASS: `node tests/e2e/qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs`
- PASS: `node tests/e2e/qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs`
- PASS: `node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs`
- PASS: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- PASS: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs IntRuoyiFronted/tests/e2e/qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs IntRuoyiFronted/tests/e2e/qa-regulation-pressure-pump-pdf-field-alignment-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs doc/tasks/20260806-qa-id-balloon-pressure-pump-pdf-items`
- PASS: `node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- PASS: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260806-qa-id-balloon-pressure-pump-pdf-items\frontend-feature-evidence.md`
- PASS: `task_closeout.py --task-id 20260806-qa-id-balloon-pressure-pump-pdf-items --mode apply`

## Notes

- The source PDF is scanned/image-based; `pypdf` text extraction returned blank text, so PDF rows were verified from rendered PNG pages.
- The bundled `pnpm ts:check` wrapper failed before type checking because it attempted a non-interactive dependency-directory confirmation. The direct script-equivalent `vue-tsc` command passed.
- Commit/push was not performed because the shared `int_main` workspace has extensive unrelated dirty changes and some long-term docs already contained non-task modifications before this task.
- Verified at `2026-08-06 20:54:00 +08:00`.
