# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: replace the process-config title area with the standard condition-tab filter and submit formal query parameters.
- Non-goals: no full `UnifiedListTemplate`, pagination, column settings, route, permission, or maintenance-flow redesign.

## Requirements And Acceptance IDs

- AC-FE-01: empty condition tabs render in the marked header area and the create button remains.
- AC-FE-02: five filter definitions submit mapped backend parameters and support intersection.
- AC-FE-03: display rows are separate from unfiltered maintenance candidate rows.
- AC-FE-04: reset removes all formal parameters; query failure clears displayed rows and surfaces an error.

## UI Entry Point And Owned Files

- Route: `/mes/pro/process-pool/team-leader`, tab `processConfig`.
- Component: `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`.
- API wrapper: `IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts`.
- Tests: focused static contract and read-only Playwright flow.

## API Contract And Data States

- GET `/mes/pro/process-pool/team-leader/process-config/list` with five optional keyword parameters.
- States: initial unfiltered baseline, filtered display rows, loading, empty result, explicit request error.

## BDD And TDD Evidence

- BDD scenarios are recorded in `execution-log.md`.
- RED: focused static contract failed before `TableMultiFilter` and formal parameters existed.
- GREEN: `node tests/e2e/team-leader-process-config-filter-query-static.spec.cjs` passed.
- GREEN: `node tests/e2e/team-leader-process-config-unified-static.spec.cjs` passed.
- `pnpm ts:check` passed after this feature implementation, but the latest rerun is blocked by the unrelated `FrontlineFixedTemplatePanel.vue:2772` `actualEmployeeId` diagnostic.

## Responsive, Accessibility, Loading, Empty, Error, Permission

- Filter container must retain `min-width: 0`, wrap condition controls, and keep the create action visible.
- Standard plus/minus aria labels remain provided by `TableMultiFilter`.
- Existing loading and query permission remain unchanged; failed queries clear display rows and show a visible error.

## Verification Path And Blockers

- Static contract, `pnpm ts:check`, and real local Playwright path.
- Real Playwright verified the filter placement, all five options, formal single/combined/reset query strings, zero writes, zero page errors, and zero new console errors.
- The running backend returned 106 rows for the single, combined, and reset requests, proving `48081` is stale and preventing valid intersection-result evidence.
- No runtime restart or environment switch was performed from the shared dirty workspace.
