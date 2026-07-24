# Task: IntPP auto schedule first loop frontend

## Goal

Implement the first closed loop of IntPP auto scheduling in the IntRuoyi MES frontend: start auto scheduling from the production scheduling page, preview the result, show blocking issues, confirm publish, and render task dependency links in the Gantt view.

## Scope

- Add the frontend API layer for auto-schedule preview, apply, issues, and dependency loading.
- Add an auto-schedule entry on the MES production scheduling page following the IntPP production order list style baseline.
- Show preview tasks, capacity summary, and blocking issues before confirm publish.
- Render route dependency links in the existing Gantt component while keeping manual link editing disabled.
- Do not add frontend-only fallback logic, mock success paths, or version-management UI.

## Milestones

- [x] M1: Previous frontend task checked and completed before new work.
- [x] M2: Frontend task documentation created in the frontend repository before production code changes.
- [x] M3: RED frontend verification written for scheduling entry, preview flow, issue rendering, and publish interaction.
- [x] M4: Frontend API wrappers and auto-schedule UI implemented.
- [x] M5: Gantt dependency rendering and issue presentation integrated.
- [x] M6: Frontend verification and focused lint/tests pass.
- [x] M7: Evidence updated and frontend task marked completed.
- [ ] M8: Frontend changes committed on `feature/auto-schedule-first-loop`.

## Expected Verification

- The production scheduling page exposes an auto-schedule action guarded by MES scheduling permissions.
- The preview flow submits parameters, renders the returned schedule preview, and surfaces blocking issues without pretending success.
- Confirm publish calls the apply API, refreshes the task list, and reloads Gantt dependencies.
- The Gantt view shows backend dependency links while link dragging remains disabled.

## Current Status

Implemented and runtime-verified on `feature/auto-schedule-first-loop`. Focused ESLint, task-local contract verification, Playwright real-user-path verification, and script-replay verification pass in `D:\ProjectPackage\Int\IntRuoyi-worktrees\auto-schedule-first-loop\yudao-ui-admin-vue3`. Repository-wide `ts:check` remains blocked by pre-existing unrelated TypeScript errors outside this task scope.
