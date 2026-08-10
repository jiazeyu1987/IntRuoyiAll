# Frontend Feature Evidence

## Feature Goal And Non-goals

- Goal: provide one route-process-scoped loss maintenance dialog with list, inline edit, delete confirmation, and bottom create action.
- Non-goals: no backend contract, database, authorization, route scope, device mapping, parameter rule, or historical snapshot change.

## Requirements And Acceptance IDs

- AC-LMD-1: each process row exposes exactly one visible loss maintenance entry.
- AC-LMD-2: the dialog reads the selected row's formal loss list and survives formal list reloads.
- AC-LMD-3: one inline create or edit operation is allowed at a time.
- AC-LMD-4: create, update, and delete use the existing formal APIs and expose failures.
- AC-LMD-5: the top generic create dialog no longer exposes loss reason creation.

## UI Entry Points And Owned Files

- Entry: production leader workbench -> 工序配置 -> row-level 损耗 button.
- Component: `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`.
- Tests: focused loss dialog contract, adjacent process-config contracts, and a dedicated real Playwright CRUD flow.

## API Contracts And Data States

- GET `/mes/pro/process-pool/team-leader/process-config/list` refreshes formal route-process rows.
- POST `/mes/pro/process-pool/team-leader/loss-reasons` creates with `routeProcessId + reasonName`.
- PUT `/mes/pro/process-pool/team-leader/loss-reasons/{id}` updates name, enabled state, and optional remark.
- DELETE `/mes/pro/process-pool/team-leader/loss-reasons/{id}` keeps the existing soft-disable semantics.
- States: closed, list idle, create draft, edit draft, submitting, delete confirmation, refreshed, error.

## BDD: Scenarios

- Given multiple losses, when the process row renders, then only one 损耗 button is visible.
- Given the dialog is open, when create or edit succeeds, then formal rows reload and the same dialog stays open.
- Given delete is cancelled, when the confirmation closes, then no delete request is sent.
- Given a write fails, when the promise rejects, then the dialog and draft remain and an explicit error is shown.

## RED And GREEN Evidence

- RED: `node tests/e2e/team-leader-loss-maintenance-dialog-static.spec.cjs` -> FAIL at the missing single row-scoped loss maintenance button, as expected.
- GREEN: focused and four adjacent static contracts pass; `pnpm ts:check`, real E2E syntax check, evidence validation, independent review, and `git diff --check` pass.
- REAL-E2E: dedicated script is implemented and fail-fast preflight records BLOCKED because the permitted writable test tenant/account/runtime/route-process inputs are absent.

## Verification And Experience Checks

- Responsive: dialog width is constrained by viewport and inline controls cannot overflow.
- Accessibility: visible text commands, table empty text, disabled states during submission, and explicit confirmation remain available.
- Loading: mutation controls are disabled while one formal write or refresh is active.
- Empty: current process with no losses still exposes the bottom create action.
- Error: no swallowed exception, mock success, or local fallback row.
- Mutation/refresh: a successful write clears duplicate-submit state before reload; reload failures retain the dialog context and explicitly state that the write succeeded.
- Permission: existing production leader page and backend route-process authorization remain unchanged.

## E2E Path

- `tests/e2e/team-leader-loss-maintenance-dialog-real.e2e.js`: real login -> production leader workbench -> 工序配置 -> locate the unique row by visible business identity plus formal `routeProcessId` -> 损耗 -> create -> cancel edit -> edit -> cancel delete -> confirm delete -> verify dialog persistence, target `code=0`, disabled result, and clean page/console/network signals.

## Blockers And Follow-up Skills

- Real E2E is blocked until a permitted writable test tenant, production leader account, matched local runtime pair, unique visible process text, and formal route-process ID are supplied through the documented environment variables.
- Follow-up: task-closeout-cleanup and project-experience-consolidation after verification.
