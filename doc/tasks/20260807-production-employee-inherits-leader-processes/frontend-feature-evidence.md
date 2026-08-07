# Frontend Feature Evidence

## Goal And Non-Goals

- Remove the production employee-to-process binding card and client write API.
- Do not redesign unrelated team configuration or personnel management sections.

## Acceptance

- The team configuration page no longer exposes production employee process binding controls.
- No frontend API function can write an employee-process relation.
- Existing device, parameter, exception, personnel, and active-order functions remain available.

## Entry Points And Owned Files

- `src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- `src/api/mes/pro/processpool/teamLeader.ts`
- Related static contract tests.

## API And UI States

- Removed write contracts: employee binding add/disable and process employee binding save.
- No new loading, empty, error, or fallback UI is introduced.

## BDD Scenarios

- Given a production leader opens team configuration, when the page renders, then no employee-process binding card or action is present.
- Given the frontend API module is loaded, when its exports are inspected, then no employee-process write function exists.

## RED And GREEN

- RED: `node tests\\e2e\\production-employee-inherits-leader-processes-static.spec.cjs` fails because the existing page still renders “生产人员工序绑定”.
- GREEN: pending.

## Responsive, Accessibility, And Permission Checks

- Removing one grid item must leave the existing responsive grid valid; no new controls or permission paths are introduced.

## E2E Or Component Verification

- Target static contracts, TypeScript check, and relevant existing real-flow script syntax.

## Blockers And Follow-Up

- None.
