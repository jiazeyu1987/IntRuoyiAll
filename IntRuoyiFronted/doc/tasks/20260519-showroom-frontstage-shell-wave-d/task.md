# Task: Showroom Frontstage Shell Wave D Reviewer Supervision

## Goal

Supervise the integration and E2E convergence wave for showroom frontstage after shared components, device shells, and route convergence have all passed strict review.

## Scope

- Reviewer-owned task for supervising subagent development.
- Wave D only: frontstage integration verification and E2E convergence.
- Use real browser paths from `http://localhost:8081`.
- Enforce alignment with:
  - `resource/展厅当前规划内容汇总.xlsx`
  - `doc/tasks/20260519-showroom-frontstage-structure-plan/frontstage-structure.md`
  - `doc/tasks/20260519-showroom-frontstage-delivery-slices/delivery-slices.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`

## Non-Scope

- Do not redesign accepted shells in this wave unless a verified blocker forces a narrow follow-up.
- Do not add E2E-only UI controls.
- Do not fake success when login, data, or runtime prerequisites are missing.

## Previous Task Check

- Previous frontstage execution task: `doc/tasks/20260519-showroom-frontstage-shell-wave-c/task.md`
- Status before this task: completed.
- Impact: canonical device routes and homepage/frontstage entry are now ready for real-path integration verification.

## Milestones

- [x] M1: Create reviewer task record and lock the Wave D scope.
- [x] M2: Launch Wave D worker with sole ownership of integration verification assets.
- [x] M3: Review the integration/E2E output strictly against the docs.
- [x] M4: Either reject with concrete findings or accept Wave D as the current frontstage convergence gate.

## Expected Verification

- Route-level convergence tests pass.
- Real browser paths prove homepage entry and frontstage device routes are reachable.
- Evidence clearly distinguishes route/test success from any remaining data/runtime blocker.

## Current Status

Completed. Wave D was initially rejected because the live display data lacked published preview images. After temporary local preview assets were explicitly approved, uploaded, published, and made publicly readable through the refreshed backend/runtime path, the real E2E convergence gate passed.

## Reviewer Rule

Wave D is **not** accepted unless:

1. real frontstage user entry is exercised,
2. device-route behavior is proven on actual routes,
3. missing prerequisites are reported explicitly rather than hidden,
4. evidence is credible and reproducible.

## Final Review Result

PASS.

- `screen`, `pad`, and `mobile` canonical routes were all reachable from real browser paths.
- Homepage entry correctly landed on the canonical `screen` default route.
- The original published-preview blocker was resolved through temporary local preview-asset upload and live preview-asset records.
- The local backend runtime was refreshed to the current source version, `showroom_narration_version.voice` was added to the local runtime schema, and live preview file URLs now resolve through public readable `/admin-api/infra/file/{configId}/get/{path}` routes.
- The local Vite dev server now proxies `/admin-api` asset requests so frontstage `<img>` resources no longer fall back to `index.html`.
- Real browser E2E rerun returned `blockers=[]`.

## Remaining Blockers

- None for the current local-verification gate.

## Worker

- Worker id: `019e3ecc-49aa-7f22-99d4-234b44cdc932`
- Nickname: `Cicero`
- Owned write set:
  - `scripts/showroom-frontstage-integration.e2e.mjs`
  - `scripts/showroom-frontstage-integration.test.mjs`
  - `doc/tasks/20260519-showroom-frontstage-shell-wave-d/worker-integration.md`
  - `doc/tasks/20260519-showroom-frontstage-shell-wave-d/e2e-screenshots/**`
