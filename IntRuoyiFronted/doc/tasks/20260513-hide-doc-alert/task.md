# Task: Hide global DocAlert banners

## Goal

Hide all page-level green documentation banners rendered through the shared `DocAlert` frontend component, including banners such as `【生产】生产排程、工序流转卡】文档地址: https://doc.iocoder.cn/mes/pro/schedule-card/`.

## Scope

- Frontend-only behavior change in the shared `src/components/DocAlert/index.vue` component.
- Preserve existing page routes, data loading, permissions, and non-DocAlert success messages.
- Verify the real logged-in user path on a page that currently renders `DocAlert`.

## Milestones

- [x] M1: Check the previous frontend task record and explicitly block the unfinished prior task before starting this one.
- [x] M2: Create the task document, execution log, and frontend evidence file before editing production code.
- [x] M3: Record BDD scenarios and RED evidence showing that a real page still displays a DocAlert green banner.
- [x] M4: Implement the minimal shared-component change to stop rendering DocAlert banners globally.
- [x] M5: Run GREEN verification on the real page, validate evidence, and prepare a scoped frontend commit.

## Expected Verification

- A real page that previously rendered `DocAlert` no longer displays the green documentation banner after login.
- Existing page content below the former banner still loads normally.
- Only the shared `DocAlert` behavior changes; unrelated success toasts, tags, and page content remain untouched.
- `execution-log.md` and `frontend-feature-evidence.md` contain BDD, RED, and GREEN evidence.

## Current Status

Completed. Real-page verification confirms the shared documentation banner is hidden, and the target MES production page still renders.

## Blocker And Impact

- Blocker: None for the behavior change itself.
- Impact: This task changes frontend banner rendering only. It does not alter backend APIs, permissions, routes, or non-DocAlert UI feedback. Type-check verification remains limited because `pnpm ts:check` exhausted Node heap in the current repository state.
