# Task: Header Menu Search History Real E2E Verification

## Goal

Verify with real frontend data and a real browser path that the top-header menu
search history works end to end: recent records are shown, the latest record is
first, and selecting a history record navigates to the stored route.

## Scope

- Check the latest frontend task state before starting this verification task.
- Create the verification task document and execution log before running the
  browser path.
- Reuse the existing real Playwright verification script for the implemented
  search-history feature.
- Use the real local frontend entry at `http://127.0.0.1:8081`.
- Do not change production code in this verification task unless the real E2E
  path exposes a verified defect.

## Previous Task Check

- Latest repo task before this verification:
  `doc/tasks/20260516-electronic-batch-record-image-directory-verify/task.md`
- Status before this task: blocked by user priority switch and pending backend
  import result.
- Immediate feature predecessor:
  `doc/tasks/20260516-tool-header-search-history/task.md`
- Feature task status before this verification: completed.
- Impact: the blocked image-directory verification does not conflict with this
  independent header-search E2E verification, and the implemented search
  history feature is ready for real-browser validation.

## Milestones

- [x] M1: Confirm task state and create this verification package.
- [x] M2: Run the real Playwright E2E path against the live 8081 frontend.
- [x] M3: Record final evidence, blockers, and artifact paths.

## Expected Verification

- Real login through `http://127.0.0.1:8081/login?redirect=/index`.
- Perform real top-header menu searches on the live frontend.
- Reopen the empty-keyword dropdown and confirm recent-search records are shown.
- Confirm the latest history record is listed first.
- Confirm selecting a stored history record navigates to the stored route.

## Current Status

Completed. The live 8081 frontend passed the real browser verification for the
header menu-search history behavior.

## Blocker And Impact

- Blocker: none.
- Impact: this thread now has a standalone real-data E2E record for the header
  menu-search history feature.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli --session router-search-history-real-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-tool-header-search-history\scripts\verify-router-search-history.mjs`
  -> PASS.
- Real login path: `http://127.0.0.1:8081/login?redirect=/index`
- Real route selections during the run:
  - first selection: `common.profile/user/profile`
  - second selection: `router.home/index`
- Recent history order in the live dropdown:
  - `router.home/index/index`
  - `common.profile/user/profile/user/profile`
- Final quick navigation target: `http://127.0.0.1:8081/user/profile`
- Screenshot artifact:
  `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\router-search-history-20260516.png`
