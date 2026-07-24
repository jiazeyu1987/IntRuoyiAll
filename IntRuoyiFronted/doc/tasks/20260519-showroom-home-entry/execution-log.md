# Execution Log: Add Showroom Entry On Home Page

## BDD Scenarios

BDD: Home page enters Showroom frontstage -> Given a logged-in user is on the home page, When they click `进入展厅前台`, Then the frontend routes to `/showroom/display/home`.

BDD: Home page enters Showroom back-office -> Given a logged-in user is on the home page, When they click `进入展厅后台`, Then the frontend routes to `/showroom-admin/company`.

BDD: Existing home guidance remains visible -> Given the Showroom entries are added, When the home page renders, Then the existing welcome, current user, system status, and usage guidance remain available.

## TDD Evidence

- RED: `node --test scripts/home-showroom-entry.test.mjs` -> FAIL, home page does not contain `数字展厅入口`, `进入展厅前台`, `进入展厅后台`, or the required Showroom route handlers.
- GREEN: `node --test scripts/home-showroom-entry.test.mjs` -> PASS, the home page exposes both Showroom entries and keeps existing welcome guidance.
- GREEN: `pnpm exec eslint src/views/Home/Index.vue` -> PASS.
- REGRESSION: `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-frontstage.test.mjs` -> PASS, 12 Showroom route/API/view contract tests passed.
- EVIDENCE VALIDATION: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260519-showroom-home-entry/frontend-feature-evidence.md` -> PASS.
- CLOSEOUT PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260519-showroom-home-entry --mode preview` -> PASS, no delete candidates and no blockers.

## Verification Evidence

- `src/views/Home/Index.vue` now exposes a `数字展厅入口` card.
- `进入展厅前台` calls `router.push('/showroom/display/home')`.
- `进入展厅后台` calls `router.push('/showroom-admin/company')`.
- Existing welcome, user, system status, and usage guidance remain visible.

## Blockers

- None.
