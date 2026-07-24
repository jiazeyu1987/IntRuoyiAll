# Task: Report Management Six-Route Recognition Frontend

## Goal

Extend the global report-management JimuReport page with an extra tab that
exposes six recognition buttons for the fixed pilot `.doc` sample, and show
route-isolated generated report lists for each recognition route.

## Scope

- Check the latest frontend task and explicitly block it before switching scope.
- Create this task package before production code changes.
- Keep the existing JimuReport iframe entry available as the default tab.
- Add a second tab in `report/jmreport/index.vue` with six route buttons `A-F`.
- Show generated reports grouped or filterable by route so results do not
  overwrite each other in the UI.
- Update frontend API bindings and focused regression tests only for this page.
- Do not change unrelated report-management routes or redesign the global report
  shell outside the needed tabbed layout.

## Previous Task Check

- Previous frontend task: `doc/tasks/20260516-pro-schedule-calendar-summary-card-detail/task.md`
- Status before this task: blocked due to explicit user reprioritization.
- Impact: the old schedule-calendar detail task remains paused and does not
  block this report-management recognition feature.

## Milestones

- [x] M1: Block the unfinished previous frontend task and create this task package.
- [x] M2: Record BDD scenarios and RED verification for the missing report-management recognition tab.
- [x] M3: Implement the tab shell, six buttons, route-aware API bindings, and result list behavior.
- [x] M4: Run focused verification, update evidence, and mark the task completed.
- [x] M5: Commit only the frontend files produced by this task.

## Expected Verification

- `node scripts/report-management-six-route-page.test.mjs`
- `pnpm exec eslint src/views/report/jmreport/index.vue src/api/mes/pro/batchrecordreport/index.ts`

## Current Status

Completed for implementation and focused verification. The global
`report/jmreport/index.vue` page now keeps the original JimuReport iframe as the
default tab, adds a second `六路识别` tab with six fixed-route buttons, and
shows route-isolated generated report lists. The legacy electronic batch-record
page now queries `routeKey=LEGACY` so the new A-F recognition outputs do not
pollute its existing list.

## Final Verification Result

- `node scripts/report-management-six-route-page.test.mjs` -> PASS
- `node scripts/electronic-batch-record-report-page.test.mjs` -> PASS
- `pnpm exec eslint src/views/report/jmreport/index.vue src/api/mes/pro/batchrecordreport/index.ts src/views/mes/pro/batchrecordtemplate/index.vue` -> PASS
- `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS, local frontend `http://127.0.0.1:8081` and backend docs `http://127.0.0.1:48081/v3/api-docs` both returned HTTP `200`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm run ts:check` -> FAIL, the repository still has unrelated pre-existing TypeScript errors in non-owned files such as `src/components/bpmnProcessDesigner/package/designer/ProcessViewer.vue`, `src/config/axios/service.ts`, and other legacy screens; no new type error was reported against the files changed for this task.
- `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename ...\\verify-report-management-six-route-smoke.mjs` -> PASS after local runtime repair, the real page rendered both tabs, route `A` returned HTTP `200`, and the list showed `15` route-A reports.
- authenticated real-runtime route matrix:
  - route `B` -> PASS, browser-context fetch returned HTTP `200`, `importedCount=15`, elapsed about `40s`
  - route `C` -> PASS, browser-context fetch returned HTTP `200`, `importedCount=15`, elapsed about `3.5s`
  - route `F` -> PASS, browser-context fetch returned HTTP `200`, `importedCount=15`, elapsed under `1s`
  - route `D` -> FAIL, real page click returned business `500`, message `route_d_pdf_reflow_converter_failed ... 'NoneType' object has no attribute 'SaveAs2'`
  - route `E` -> FAIL, backend runtime logs show `Codex CLI 图片识别超时`

## Blocker And Impact

- Blocker: none after the scoped frontend commit.
- Impact: none for the frontend slice; unrelated dirty files remain outside this
  task's staged/committed scope.
