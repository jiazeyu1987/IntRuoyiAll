# Execution Log

## User Intent

用户基于截图指出红框里的“返回表单模板”这类按钮需要统一成“返回”按钮，并要求检查其它前端页面是否存在类似按钮并统一修改。

## BDD / TDD Plan

- BDD: Header return button copy is unified -> Given a page/workspace header contains a left-arrow return control, When the control returns to the previous list/workspace, Then the visible label is the standard “返回” and the existing click handler remains unchanged.
- BDD: Similar return controls are scanned globally -> Given frontend pages may contain “返回xxx” controls, When the static contract scans scoped Vue pages, Then disallowed long header return labels are rejected unless they are non-header business copy.
- BDD: Business behavior is preserved -> Given the user clicks the unified return button, When the existing handler runs, Then route/API/permission/save/error behavior remains the same as before.

## Initial Notes

- Existing worktree has unrelated concurrent dirty changes and local commits. This task must use selective staging only and must not use `git add -A`.
- Protected files: backend, API wrappers, route guards, permission SQL/data, and unrelated task documents.

## Milestone Evidence

- BDD: Header return button copy is unified -> Given a page/workspace header contains a left-arrow return control, When the control returns to the previous list/workspace, Then the visible label is the standard “返回” and the existing click handler remains unchanged.
- BDD: Similar return controls are scanned globally -> Given frontend pages may contain “返回xxx” controls, When the static contract scans scoped Vue pages, Then disallowed long header return labels are rejected unless they are non-header business copy.
- BDD: Business behavior is preserved -> Given the user clicks the unified return button, When the existing handler runs, Then route/API/permission/save/error behavior remains the same as before.
- RED: `node tests/e2e/header-return-buttons-static.spec.js` -> FAIL, expected reason: `src/views/form-center/template/index.vue must not expose long header return label “返回表单模板”`.
- GREEN: `node tests/e2e/header-return-buttons-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/form-center-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/edhr-batch-template-simulate-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/edhr-batch-template-simulate-return-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/edhr-execution-list-removal-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/edhr-open-process-form-route-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mes-pro-route-edit-invalid-id-guard-static.spec.js` -> PASS.
- GREEN: `node --check tests/e2e/edhr-batch-process-companion-forms-real.e2e.js` -> PASS.
- GREEN: `node --check tests/e2e/edhr-field-audit-real-flow.e2e.js` -> PASS.
- GREEN: `node --check tests/e2e/smart-scheduling-clickable-coverage.e2e.js` -> PASS.
- GREEN: `pnpm e2e:basic-data:scheme-d-controls:static` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `rg -n "返回(表单模板|报表列表|排产|列表|审批列表|批次详情|批次执行|模板说明|批记录表单)|backToBatchLabel|backButtonLabel" IntRuoyiFronted/src/views IntRuoyiFronted/tests/e2e` -> PASS for page UI; remaining matches are comments or negative static assertions intentionally guarding old labels.
- GREEN: `git diff --check -- <task-owned files>` -> PASS; Git only reported CRLF normalization warnings.

## Changed Scope

- Source pages: FormCenter template page, JmReport page, schedule calendar, route edit page, eDHR approval/domain/field audit detail pages, eDHR execution page, eDHR batch template simulate/template pages, and batch record form list.
- Test scope: added `header-return-buttons-static.spec.js` and updated adjacent static/real E2E expectations to target the unified “返回” button.
- Non-goals preserved: no backend changes, no route/API contract changes, no permission changes, no fallback or silent error downgrade.

## Closeout Evidence

- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260803-unify-header-return-buttons/frontend-feature-evidence.md` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\design-system-delivery\scripts\validate_design_system.py --evidence doc/tasks/20260803-unify-header-return-buttons/design-system-evidence.md` -> PASS.
- GREEN: `rg -n "按钮统一|header-return-buttons-static|返回按钮统一" docs\experience-index.md docs\frontend-development.md` -> PASS, experience route points to `docs/frontend-development.md#前端截图按钮统一静态契约门禁`.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260803-unify-header-return-buttons --mode preview` -> PASS, delete set only contained temporary skill evidence files.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260803-unify-header-return-buttons --mode apply` -> PASS, deleted `frontend-feature-evidence.md` and `design-system-evidence.md`.
- COMMIT: `3649d56c5 fix: unify header return buttons` -> included only this task's UI, tests, task docs, and experience gate files; commit hook reported `Branch runtime port guard passed for int_main/int_main: frontend 8081, backend 48081`.
- BOUNDARY: Post-commit status still contained unrelated DCC/backend/package/task-document changes and untracked DCC artifacts; these were left unstaged and unmodified by this task.
