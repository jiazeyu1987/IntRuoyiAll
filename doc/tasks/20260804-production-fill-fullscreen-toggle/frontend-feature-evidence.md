# Feature

Production fill fullscreen toggle for the eDHR frontline fixed template panel.

## Acceptance

- Acceptance 1: production fill page top-right action displays `最大化` by default instead of hard-coded `主页`.
- Acceptance 2: clicking `最大化` requests native fullscreen on the production operator screen.
- Acceptance 3: while fullscreen is active, the same action displays `主页` and exits fullscreen to restore the normal page.
- Acceptance 4: save, submit, payload, permission, and backend API behavior remain unchanged.

## UI Entry Points

- Route/component: `src/views/mes/pro/edhr-batch/BatchProductionFillPage.vue`
- Owned component: `src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`
- Focused contract: `tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs`

## API Contracts And Data States

- No API contract changed.
- Fullscreen state is local UI state synchronized from `document.fullscreenElement`.
- Missing fullscreen API or failed fullscreen call is surfaced as a visible error message; no mock, fallback, or silent success is introduced.

## BDD

- BDD: Default max entry -> Given production leader enters production fill page, When the fixed production panel renders, Then the top-right button is `最大化`, not hard-coded `主页`.
- BDD: Enter fullscreen -> Given the production fill page is normal size, When the user clicks `最大化`, Then the production operator screen requests native fullscreen and the button becomes `主页`.
- BDD: Restore -> Given the production operator screen is fullscreen, When the user clicks `主页`, Then fullscreen exits and the button returns to `最大化`.

## RED

- RED: `node tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> FAIL, old implementation had no `productionScreenRef` and still used `@click="handleHome">主页`.

## GREEN

- GREEN: `node tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.

## Verification

- `node --check tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs` -> PASS.
- `node --check tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs` -> PASS.
- `git diff --check -- <task-owned paths>` -> PASS, with Git line-ending warnings only.

## Blockers

- Broader adjacent static contract `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs` currently fails before this task's assertions because `EdhrBatchRecordTabs.vue` does not render `历史批记录`; this is recorded as an unrelated existing blocker.
- Task closeout commit/push not performed because the workspace was already dirty and `int_main` was ahead of origin before this task.
