# Verification Report

## Summary

The runtime FormCenter action panel no longer calls the template management version endpoint. It uses the `openTask` embedded template snapshot and fails visibly when the runtime snapshot is missing.

## Commands

- RED: `node tests/e2e/edhr-switch-filler-formcenter-slot-static.spec.js` -> FAIL before implementation because `getTemplateVersion(templateId, versionNo)` was still present in runtime loading.
- GREEN: `node tests/e2e/edhr-switch-filler-formcenter-slot-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/edhr-dynamic-form-action-panel-prefill-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/form-center-static.spec.js` -> PASS.
- REGRESSION: `node tests/e2e/edhr-work-task-formcenter-navigation-static.spec.js` -> PASS.
- REGRESSION: `node tests/e2e/edhr-loss-form-open-action-static.spec.js` -> PASS.
- REGRESSION: `node tests/e2e/form-center-action-projection-static.spec.js` -> PASS.
- SETUP: `pnpm install --frozen-lockfile` -> PASS after isolated worktree lacked `node_modules`.
- GREEN: `pnpm ts:check` -> PASS.
- VALIDATOR: `validate_bug_regression.py --evidence doc\tasks\20260803-form-center-route-missing\bug-regression-evidence.md` -> PASS.
- VALIDATOR: `validate_frontend_feature.py --evidence doc\tasks\20260803-form-center-route-missing\frontend-feature-evidence.md` -> PASS.
- EXPERIENCE: `rg -n "请求地址不存在" docs\experience-index.md docs\frontend-development.md` -> PASS.
- CHECK: `git diff --check` -> PASS, no whitespace errors.
- CLOSEOUT PREVIEW: `task_closeout.py --task-id 20260803-form-center-route-missing --mode preview --worktree-closeout off` -> READY.
- CLOSEOUT APPLY: `task_closeout.py --task-id 20260803-form-center-route-missing --mode apply --worktree-closeout off` -> APPLIED.
- WORKTREE SLOT: reserved `int_main` slot `13`, frontend `8094`, backend `48094`.
- GUARD: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS after slot reservation.

## Files Verified

- `IntRuoyiFronted/src/views/form-center/business-action/ActionFormPanel.vue`
- `IntRuoyiFronted/tests/e2e/edhr-switch-filler-formcenter-slot-static.spec.js`
- `IntRuoyiFronted/tests/e2e/edhr-dynamic-form-action-panel-prefill-static.spec.js`
- `IntRuoyiFronted/tests/e2e/form-center-static.spec.js`

## Remaining Risks

- No real browser E2E was run in this isolated worktree because no local frontend/backend runtime was started. Static contracts and TypeScript verification cover the reported regression path.
- Linked worktree merge/removal was intentionally not run because the main worktree `E:\IntRuoyi` is dirty with non-task changes.
