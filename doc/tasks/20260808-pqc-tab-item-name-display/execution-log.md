# Execution Log

## Intent

- 用户指出一线 PQC 页面底部红框 tab 仍显示检验项目代码，应改为显示检验项目名称。

## BDD

- BDD: PQC tab 显示检验项目名称 -> Given PQC 规程项目同时包含 `itemCode` 和正式 `itemName`, When 一线 PQC 底部检验项目 tab 渲染, Then tab 主标题显示 `itemName`，不得显示 `itemCode`。
- BDD: PQC tab 展示不影响提交身份 -> Given 用户切换底部检验项目 tab 并提交, When 前端构造提交明细, Then `itemCode` 仍作为 key 和提交身份保留，`itemName` 单独作为项目名称传递。

## Evidence

- Task directory created for this additional tab display fix.
- Applied gates: user-visible description vs internal code, PQC formal item snapshot, static contract isolation.
- RED: `node tests/e2e/pqc-tab-item-name-display-static.spec.cjs` -> FAIL, expected because bottom tab rendered `item.label` and the mapping still allowed `itemCode` to become the visible label.
- Implemented: bottom PQC tab now renders `formatPqcInspectionItemTabLabel(item)`, stores normalized `itemName`, uses `未配置检验项目名称` only for missing names, and keeps `itemCode` only as tab key / submitted identity.
- GREEN: `node tests/e2e/pqc-tab-item-name-display-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/pqc-active-title-method-display-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/frontline-pqc-formal-submit-static.spec.js` -> PASS.
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-tab-item-name-display-static.spec.cjs IntRuoyiFronted/tests/e2e/pqc-active-title-method-display-static.spec.cjs doc/tasks/20260808-pqc-tab-item-name-display` -> PASS, only existing LF-to-CRLF warning on the Vue file.

## Closeout

- task-closeout-cleanup preview -> PASS, keep `task.md`, `execution-log.md`, `verification-report.md`; delete none; blocked none; warnings none.
- task-closeout-cleanup apply -> PASS, deleted none; linked worktree false; no merge or worktree removal needed.
- Project experience consolidation -> existing user-visible description/internal-code gate already applies; no new long-term document created or modified because `docs/frontend-development.md` and `docs/experience-index.md` already had unrelated dirty changes.
