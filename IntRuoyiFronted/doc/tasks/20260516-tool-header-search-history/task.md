# Task: Header Menu Search Recent History

## Goal

Enhance the top-header menu search so it can drop down the most recent 10
search records, keep the newest record first, and allow users to select a
record for quick menu access.

## Scope

- Check the latest frontend task state before starting this task.
- Create the task document, execution log, and feature evidence before
  production-code edits.
- Reproduce the current missing-history behavior through the real frontend entry
  `http://localhost:8081`.
- Implement the minimal enhancement inside `RouterSearch/index.vue` and any
  directly required local cache usage.
- Preserve current routing, permissions, header layout, and modal search
  behavior unless required by this scope.
- Do not add fallback logic, mock data, or unrelated UI redesign.

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260516-electronic-batch-record-image-directory-verify/task.md`
- Status before this task: blocked by user priority switch and pending backend
  import result.
- Impact: the paused verification task does not conflict with this independent
  top-header search enhancement.

## Milestones

- [x] M1: Block the unfinished previous frontend task and create this task package.
- [x] M2: Record BDD scenarios and capture RED evidence for the missing recent-search behavior.
- [x] M3: Implement recent 10 search-history support with newest-first ordering and quick selection.
- [x] M4: Run GREEN verification, update evidence, and prepare a scoped commit.

## Expected Verification

- Real login through `http://127.0.0.1:8081/login?redirect=/index`.
- After two menu searches are completed, reopening the top-header search with
  an empty keyword shows recent search history.
- The dropdown keeps at most 10 records and shows the latest record first.
- Selecting a recent record performs quick navigation to the stored route.
- `pnpm.cmd exec eslint src/components/RouterSearch/index.vue`

## Current Status

Completed. The top-header menu search now persists recent search records,
shows the newest record first when the keyword is empty, and allows direct
selection for quick route navigation.

## Blocker And Impact

- Blocker: none for the scoped feature change.
- Impact: users can now reopen recent menu searches from the top-header
  dropdown instead of retyping the same route keywords.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli --session router-search-history-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-tool-header-search-history\scripts\verify-router-search-history.mjs`
  -> FAIL before the fix because the empty-keyword dropdown never rendered the
  `最近搜索` group after prior searches.
- `npx.cmd --yes --package @playwright/cli playwright-cli --session router-search-history-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-tool-header-search-history\scripts\verify-router-search-history.mjs`
  -> PASS after the fix with recent-search order
  `["router.home/index/index", "common.profile/user/profile/user/profile"]`
  and final quick navigation to `/user/profile`.
- `pnpm.cmd exec eslint src/components/RouterSearch/index.vue`
  -> PASS.
