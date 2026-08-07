# Frontend Feature Evidence

## Feature Goal And Non-goals

- Goal: remove the visible team configuration module tab and its selectable frontend branch from the frontline production workbench.
- Non-goals: no backend API, permission, route, data model, or other module-tab behavior changes.

## Requirements And Acceptance IDs

- AC-1: the top module navigation does not render the text `班组配置`.
- AC-2: the module-tab state and page rendering no longer expose the team configuration branch.
- AC-3: neighboring module tabs remain covered by regression tests.

## UI Entry Points And Owned Files

- Entry point: `src/views/mes/pro/processpool/ProductionLeaderWorkbenchPage.vue`.
- Route: `/mes/pro/process-pool/production-leader`.
- Component: `src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`.
- Tests: production module-tab static contracts and the production-leader role-matrix flow definition.

## API Contracts And Data States

- No API contract change is expected.
- Loading, empty, error, and permission states outside the removed branch are non-goals.

## BDD Scenarios

- Given the user opens the frontline production workbench, when the module navigation renders, then the team configuration tab is absent and neighboring tabs remain available.
- Given the module tab state is initialized, when the user switches among visible tabs, then neither the state contract nor the page branch contains team configuration.

## RED

- `node tests/e2e/production-leader-function-tabs-static.spec.js` -> FAIL because the repeated module tabs still expose the team configuration pane and `config` remains selectable.

## GREEN

- Pending focused static contract command and passing result.

## Responsive And Accessibility Checks

- Removing one tab must not introduce layout overflow or focusable hidden controls.
- No new controls or visual states are added.

## E2E Or Component Verification Path

- Focused static contract plus neighboring frontend contracts and TypeScript check.
- Real-path browser verification will be used only if the configured local runtime and login prerequisites are available and required by the discovered page contract.

## Blockers And Follow-up Skills

- None at task creation.
