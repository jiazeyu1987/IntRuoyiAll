# Task: Homepage welcome page

## Goal

Replace the current `/index` demo dashboard with a simple welcome page that fits the existing Int operations-console style.

## Scope

- Keep the homepage route and auth flow unchanged.
- Replace the current demo dashboard content in `src/views/Home/Index.vue`.
- Preserve the existing admin shell layout, menus, and backend contracts.
- Verify the real login-to-home path with Playwright.
- Do not add fallback content, mock data, or backend changes.

## Milestones

- [x] M1: Previous frontend task checked and explicitly marked blocked before starting this task.
- [x] M2: This task document was created before production code changes.
- [x] M3: Record BDD scenarios and run a RED Playwright check for the new welcome-page behavior.
- [x] M4: Implement the simple welcome page in the homepage view.
- [x] M5: Rerun the real login-to-home verification and record GREEN evidence.
- [x] M6: Run targeted frontend verification, update evidence, and finalize the task.
- [x] M7: Commit only this task's frontend changes after required verification passes.

## Expected Verification

- Playwright can log in to `http://127.0.0.1:8081` with the configured admin account.
- Visiting `/index` shows a simple welcome heading and supporting guidance.
- The old demo dashboard content is no longer shown on the homepage.
- No fallback or mock behavior is introduced.
- BDD, RED, and GREEN evidence are recorded in `execution-log.md` and `frontend-feature-evidence.md`.

## Current Status

Completed. The homepage now renders a simple welcome page, and real Playwright verification passed on both desktop and mobile viewports.

## Blocker And Impact

- Blocker: None at task start.
- Impact: None.

## Final Verification

- `pnpm exec eslint src/views/Home/Index.vue doc/tasks/20260513-homepage-welcome-page/scripts/verify-homepage-welcome.mjs` -> PASS
- `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session homepage-welcome-green-3 run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260513-homepage-welcome-page\scripts\verify-homepage-welcome.mjs` -> PASS
