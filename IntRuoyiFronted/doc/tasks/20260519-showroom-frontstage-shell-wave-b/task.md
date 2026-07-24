# Task: Showroom Frontstage Shell Wave B Reviewer Supervision

## Goal

Supervise the second implementation wave for showroom frontstage device-shell delivery. Wave B focuses on three parallel device-shell implementations: `screen`, `pad`, and `mobile`.

## Scope

- Reviewer-owned task for supervising subagent development.
- Wave B only: device-specific frontstage shell components and views.
- Enforce alignment with:
  - `resource/展厅当前规划内容汇总.xlsx`
  - `doc/tasks/20260519-showroom-frontstage-structure-plan/frontstage-structure.md`
  - `doc/tasks/20260519-showroom-frontstage-delivery-slices/delivery-slices.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- Require strict review before any shell is accepted.

## Non-Scope

- Do not implement route convergence in this wave.
- Do not modify shared domain files or shared presentation components in this wave.
- Do not loosen layout or interaction requirements for convenience.

## Previous Task Check

- Previous frontstage execution task: `doc/tasks/20260519-showroom-frontstage-shell-wave-a/task.md`
- Status before this task: completed.
- Impact: shared presentation components are accepted, so `screen/pad/mobile` shells may now proceed in parallel.

## Milestones

- [x] M1: Create reviewer task record and lock the Wave B scope.
- [x] M2: Launch `screen`, `pad`, and `mobile` workers with disjoint ownership.
- [x] M3: Review each worker output strictly against the docs.
- [x] M4: Either reject with concrete findings or accept each shell for the next route-convergence wave.

## Expected Verification

- `screen` files exist only under `src/views/showroom-frontstage/screen/**`
- `pad` files exist only under `src/views/showroom-frontstage/pad/**`
- `mobile` files exist only under `src/views/showroom-frontstage/mobile/**`
- Each worker owns its own test script and evidence file.
- No worker crosses into shared components, router modules, or another device-shell directory.

## Current Status

Completed. All three device shells were reviewed strictly against the workbook and structure docs. Each shell required at least one corrective pass before acceptance.

## Reviewer Rule

No Wave B shell is accepted unless:

1. it respects its write boundary,
2. it composes the accepted shared components instead of reimplementing them,
3. it matches the workbook and preview-driven layout intent for its device,
4. it preserves the operations-console visual baseline,
5. its verification evidence is credible.

## Workers

- `screen`
  - Worker id: `019e3e70-a750-7633-9cb0-9fc73dfe6d91`
  - Nickname: `Einstein`
  - Owned write set:
    - `src/views/showroom-frontstage/screen/**`
    - `scripts/showroom-frontstage-screen-shell.test.mjs`
    - `doc/tasks/20260519-showroom-frontstage-shell-wave-b/worker-screen.md`
- `pad`
  - Worker id: `019e3e70-a805-7c60-9b77-680c58eb95ca`
  - Nickname: `Schrodinger`
  - Owned write set:
    - `src/views/showroom-frontstage/pad/**`
    - `scripts/showroom-frontstage-pad-shell.test.mjs`
    - `doc/tasks/20260519-showroom-frontstage-shell-wave-b/worker-pad.md`
- `mobile`
  - Worker id: `019e3e70-a8e0-7d40-a497-65127fb39887`
  - Nickname: `Parfit`
  - Owned write set:
    - `src/views/showroom-frontstage/mobile/**`
    - `scripts/showroom-frontstage-mobile-shell.test.mjs`
    - `doc/tasks/20260519-showroom-frontstage-shell-wave-b/worker-mobile.md`

## Final Review Result

PASS.

- `screen` accepted after:
  - replacing text-placeholder category glyphs with a real icon-capable category interface;
  - supporting arbitrary category icon input instead of a narrow fixed semantic set.
- `pad` accepted after:
  - replacing fixed-page-tab assumptions with a true icon-bearing category-input model;
  - removing the default fallback to `showroomDisplayTabs` as the primary navigation contract.
- `mobile` accepted after:
  - changing the top navigation from hardcoded `company/context/settings` entries to a true category-set input;
  - moving route parsing, API loading, and orchestration out of the shell and into a mobile-local controller/composable.

## Verification Result

- PASS: `node --test scripts/showroom-frontstage-screen-shell.test.mjs scripts/showroom-frontstage-pad-shell.test.mjs scripts/showroom-frontstage-mobile-shell.test.mjs`
- PASS: `pnpm exec eslint src/views/showroom-frontstage/screen/**/*.vue scripts/showroom-frontstage-screen-shell.test.mjs src/views/showroom-frontstage/pad/**/*.vue src/views/showroom-frontstage/pad/types.ts scripts/showroom-frontstage-pad-shell.test.mjs src/views/showroom-frontstage/mobile/**/*.vue src/views/showroom-frontstage/mobile/composables/useShowroomMobileView.ts src/views/showroom-frontstage/mobile/types.ts scripts/showroom-frontstage-mobile-shell.test.mjs`
