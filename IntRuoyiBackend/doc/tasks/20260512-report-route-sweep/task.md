# Task: Report Management route sweep

## Goal

Support and verify every Report Management child route used by the real frontend, including required backend report module wiring when runtime validation exposes missing backend endpoints.

## Scope

- Discover Report Management menu entries from backend menu data and frontend component mapping.
- Verify backend module wiring required by `report/jmreport/index`, `report/jmreport/bi`, and `report/goview/index`.
- If a backend route fails because the report module is disabled or missing from the server dependency graph, fix the exact module wiring without fallback behavior.
- Leave unrelated MES, BPM, CRM, and Infrastructure task files untouched except for the required blocked-status notes already recorded.

## Previous Task Check

- Previous unfinished backend task: `doc/tasks/20260512-bpm-route-sweep`.
- Status before this task: blocked because the user redirected the active sweep from workflow/BPM to Report Management.
- Impact: Workflow/BPM route validation remains incomplete and is outside this Report Management route-sweep commit.

## Milestones

- [x] M1: Previous backend task checked and explicitly blocked before new work.
- [x] M2: Report Management backend task documentation created before route audit work.
- [x] M3: Report Management route and module inventory collected.
- [x] M4: Backend wiring failures fixed or blocked with exact evidence.
- [x] M5: Backend/frontend route sweep verified and task finalized.
- [x] M6: Task changes committed separately after verification passes.

## Expected Verification

- Report module is present in the Maven reactor and server dependency graph if the Report Management menu is enabled.
- Targeted Maven verification passes after backend wiring changes.
- Real frontend navigation opens every Report Management child route without missing-route or disabled-module responses.

## Current Status

Completed. Report module wiring, runtime package, and local schema prerequisites were fixed so the real frontend route sweep can load every Report Management child route.

## Final Verification

- `ReportModuleEnablementTest` passes in the full reactor.
- Packaged backend jar includes the report module and starts at `http://localhost:48081`.
- Local MySQL now contains the required JimuReport, JimuBI, and GoView tables used by the verified route sweep.
