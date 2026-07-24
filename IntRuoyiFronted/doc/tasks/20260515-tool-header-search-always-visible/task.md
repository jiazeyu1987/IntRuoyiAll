# Task: Keep Header Search Visible And Hide Tenant/Screenfull

## Goal

Update the top-right header tools so the tenant selector and screenfull control
do not render, and the menu search input stays visible without requiring an
extra click on the search icon.

## Scope

- Check the latest frontend task state before starting this task.
- Create and maintain the task document and execution log in the frontend repo.
- Reproduce the existing header behavior with a real Playwright browser path at
  `http://localhost:8081`.
- Apply the minimal frontend-only fix in `ToolHeader.vue` and
  `RouterSearch/index.vue`.
- Avoid fallback logic, compatibility shims, extra UI controls, or unrelated
  layout refactors.

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-electronic-batch-record-image-codex-cli-import/task.md`
- Status before this task: blocked by backend prerequisite.
- Impact: the paused image-import task did not block this independent header UI
  fix.

## Milestones

- [x] M1: Confirm the previous frontend task is blocked and create this task document.
- [x] M2: Record BDD scenarios and capture RED evidence for the current header behavior.
- [x] M3: Implement the minimal top-header visibility fix.
- [x] M4: Run GREEN verification, record evidence, and review scoped commit readiness.

## Expected Verification

- Real login through `http://localhost:8081/login?redirect=/index`.
- The top header no longer shows the tenant selector.
- The top header no longer shows the screenfull control.
- The menu search input is visible immediately after login on `/index`.

## Current Status

Completed. The task was briefly paused by a later user-priority switch in this
shared repo workflow, then resumed and finished in the current thread. The
top-right header now hides the tenant selector and screenfull control, and the
menu search input stays visible by default.

## Blocker And Impact

- Blocker: none for this scoped fix.
- Impact: users can now type into menu search immediately, and the removed
  controls no longer occupy top-header space.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli --session tool-header-search-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-tool-header-search-always-visible\scripts\verify-tool-header-visibility.mjs`
  -> FAIL before the fix with `tenantVisible=true`, `screenfullVisible=true`,
  and `searchVisible=false`.
- `npx.cmd --yes --package @playwright/cli playwright-cli --session tool-header-search-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-tool-header-search-always-visible\scripts\verify-tool-header-visibility.mjs`
  -> PASS after the fix with `tenantVisible=false`, `screenfullVisible=false`,
  and `searchVisible=true`.
- `pnpm.cmd exec eslint src/components/RouterSearch/index.vue src/layout/components/ToolHeader.vue`
  -> PASS.
- `pnpm.cmd ts:check`
  -> FAIL on pre-existing repository-wide TypeScript errors outside this task
  scope, including files under `src/components/bpmnProcessDesigner`,
  `src/components/Qrcode`, `src/config/axios/service.ts`, and unrelated view
  modules.
