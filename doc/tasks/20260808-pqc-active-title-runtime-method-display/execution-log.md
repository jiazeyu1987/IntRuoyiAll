# Execution Log

## Intent

- 用户反馈红框里的 tab 仍看到 `AO5 final inspection`，而不是预期的 `目视检验`。

## BDD

- BDD: PQC 红框 tab 显示检验方法 -> Given 当前 PQC 检验项目具有内部项目代码或英文项目名以及正式检验方法, When 一线 PQC 红框 tab 渲染, Then tab 主标题显示正式检验方法名称 `目视检验`，不得显示 `AO5 final inspection`。
- BDD: 检验方法弹窗不显示项目英文名 -> Given 用户点击检验方法卡片, When 检验方法弹窗打开, Then 弹窗标题和正文显示规范化后的检验方法，不显示项目英文名。
- BDD: 展示不影响项目身份 -> Given 当前项目仍需要按项目编码切换、保存和提交, When 红框 tab 显示检验方法, Then `itemCode` 和 `itemName` 仍保留为内部身份和提交字段，不被改写为中文名称。

## Evidence

- Task directory created for runtime title display regression.
- Applied gates: user-visible description vs internal code, static contract isolation.
- RED: `node tests/e2e/pqc-tab-method-display-static.spec.cjs` -> FAIL, expected because `formatPqcInspectionItemTabLabel` still returned `item.itemName || '未配置检验项目名称'`.
- Implemented: `formatPqcInspectionItemTabLabel(item)` now returns `formatPqcMethodSummary(item)`, so the red-box tab displays `目视检验` instead of `AO5 final inspection`.
- Implemented: PQC method dialog aria label, title, and body now use `formatPqcMethodSummary(activePqcMethodItem)` instead of `activePqcMethodItem.label`.
- Updated adjacent static contracts so the current expectation is method display while preserving `itemCode` and `itemName` as structured identity fields.
- GREEN: `node tests/e2e/pqc-tab-method-display-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/pqc-active-title-method-display-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/pqc-tab-item-name-display-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/pqc-inspection-tabs-layout-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/frontline-pqc-formal-submit-static.spec.js` -> PASS.
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-tab-method-display-static.spec.cjs IntRuoyiFronted/tests/e2e/pqc-active-title-method-display-static.spec.cjs IntRuoyiFronted/tests/e2e/pqc-tab-item-name-display-static.spec.cjs doc/tasks/20260808-pqc-active-title-runtime-method-display` -> PASS, only existing LF-to-CRLF warning on the Vue file.

## Closeout

- task-closeout-cleanup preview -> PASS, keep `task.md`, `execution-log.md`, `verification-report.md`; delete none; blocked none; warnings none.
- task-closeout-cleanup apply -> PASS, deleted none; linked worktree false; no merge or worktree removal needed.
- Project experience consolidation -> existing user-visible description/internal-code gate already applies; no new long-term document created or modified because `docs/frontend-development.md` and `docs/experience-index.md` already had unrelated dirty changes.
