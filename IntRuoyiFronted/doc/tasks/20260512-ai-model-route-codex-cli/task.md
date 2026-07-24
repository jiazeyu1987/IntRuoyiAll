# Task: AI big-model route sweep and Codex CLI frontend alignment

## Goal

Route through every visible child page under the AI big-model top-level menu in the real frontend and fix frontend route wiring defects. If the user path needs LLM-backed behavior, the frontend must target the backend contract that uses the local Codex CLI instead of another LLM provider.

## Scope

- Discover visible child routes under the AI big-model top-level menu, including direct AI feature pages and console management pages.
- Use Playwright against the real frontend entry point for navigation.
- Capture frontend console errors, route component load failures, and failed network requests.
- Fix frontend route wiring defects without fallback, mock data, or hidden downgrade behavior.
- Coordinate with the backend task `ruoyi-vue-pro/doc/tasks/20260512-ai-model-route-codex-cli` for local Codex CLI LLM behavior.

## Milestones

- [x] M1: Previous unfinished frontend task checked and blocked before new work.
- [x] M2: Task documentation created before route audit and production code changes.
- [x] M3: AI big-model child route inventory collected.
- [x] M4: RED route/contract verification added and observed failing for current defects.
- [x] M5: Frontend route wiring fixed with minimal changes.
- [x] M6: Playwright route sweep rerun and frontend verification completed.
- [x] M7: Evidence updated and frontend task changes committed separately.

## Expected Verification

- Playwright logs in through the real frontend and opens each AI big-model child route.
- Every visible AI big-model child route renders without unhandled frontend errors.
- Initial network responses do not show missing route, disabled AI module, or incompatible LLM provider errors.
- Frontend targeted tests and route audit script pass after changes.

## Current Status

Completed. All 16 visible AI routes load cleanly in the real frontend, and the AI write user path returns generated content again after the backend AI module, schema, and local Codex CLI path were enabled.
