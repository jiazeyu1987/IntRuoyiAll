# Task: Showroom Frontstage Shell Wave C Reviewer Supervision

## Goal

Supervise the route-convergence wave for showroom frontstage after `screen`, `pad`, and `mobile` shells have all passed strict review.

## Scope

- Reviewer-owned task for supervising subagent development.
- Wave C only: route convergence and homepage/front entry convergence.
- Enforce alignment with:
  - `resource/展厅当前规划内容汇总.xlsx`
  - `doc/tasks/20260519-showroom-frontstage-structure-plan/frontstage-structure.md`
  - `doc/tasks/20260519-showroom-frontstage-delivery-slices/delivery-slices.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`

## Non-Scope

- Do not redesign accepted shared components or device shells in this wave.
- Do not add fallback routes or compatibility shims unless explicitly required by the reviewed docs.
- Do not loosen the explicit-route requirement for convenience.

## Previous Task Check

- Previous frontstage execution task: `doc/tasks/20260519-showroom-frontstage-shell-wave-b/task.md`
- Status before this task: completed.
- Impact: accepted `screen/pad/mobile` shells are now ready to be wired into canonical frontstage routes.

## Milestones

- [x] M1: Create reviewer task record and lock the Wave C scope.
- [x] M2: Launch Wave C worker with sole ownership of route convergence.
- [x] M3: Review the convergence output strictly against the docs.
- [x] M4: Either reject with concrete findings or accept Wave C for the next integration wave.

## Expected Verification

- Canonical device-mode routes are wired.
- Homepage or frontstage entry reaches a deterministic frontstage shell.
- No accepted shell directory is rewritten outside the minimum necessary route wiring surface.
- Verification evidence is present and credible.

## Current Status

Completed. Wave C route convergence was reviewed strictly, rejected once for runtime category-icon fallback drift, corrected, and then accepted.

## Reviewer Rule

Wave C is **not** accepted unless:

1. explicit device-mode route intent is honored,
2. entry behavior is deterministic,
3. no fallback/compatibility drift is introduced,
4. the accepted shell wave remains intact,
5. verification evidence is credible.

## Worker

- Worker id: `019e3ea4-dc41-77e1-824e-af66ba0eba65`
- Nickname: `Kuhn`
- Owned write set:
  - `src/router/modules/showroom.ts`
  - `src/views/Home/Index.vue`
  - `scripts/showroom-frontstage-route-convergence.test.mjs`
  - `doc/tasks/20260519-showroom-frontstage-shell-wave-c/worker-route-convergence.md`

## Final Review Result

PASS.

- First-pass acceptance was withheld because runtime category binding for `screen/pad` still used placeholder initials or a single generic icon.
- A follow-up mobile runtime review also found first-character icon placeholders inside the mobile controller layer.
- After corrective passes:
  - `screen/pad` runtime category binding now uses explicit category-icon semantics based on the live hall naming set;
  - unknown category names now fail fast with explicit configuration errors;
  - the homepage frontstage entry is deterministic and routes to the `screen` shell;
  - old frontstage entry paths remain redirect-only aliases rather than primary entry points.

## Verification Result

- PASS: `node --test scripts/showroom-frontstage-route-convergence.test.mjs scripts/showroom-frontstage.test.mjs`
- PASS: `pnpm exec eslint src/router/modules/showroom.ts src/views/Home/Index.vue scripts/showroom-frontstage-route-convergence.test.mjs`
