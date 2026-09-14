# DF10 Round-3 Independent Verification Report

## Objective

Independently verify DF10 against the supervisor DF10 plan, TC-DF10-BACKEND-PROJECTION, interface-contracts section 2, and the DF10 agent contract. The gate requires an activeOrderId-only dedicated frontline PQC projection that reads the active-order locked QA snapshot through the frozen DF07 boundary, returns every locked QA process and complete published items, overlays all rule tasks and production-submit candidates in stable order, uses batched reads, preserves the production-route response contract, and introduces no inference, fallback, compatibility shim, or swallowed failure.

## Evidence Reviewed

- Root and worktree AGENTS.md, backend-development.md, task-closeout-rules.md, and powershell-encoding.md.
- `independent-verification-gate` SKILL.md and its required report structure.
- Supervisor prd.md, dev-plan.md DF10, and test-plan.md TC-DF10-BACKEND-PROJECTION.
- Design package common-background.md, interface-contracts.md section 2, and agent-tasks/DF10-process-page-projection.md.
- DF10 task.md, execution-log.md, verification-report.md, backend-api-evidence.md, and prior independent report.
- Full production and test diff from `a386dc0da` in `task/20260812-frontline-pqc-dcc-qa-df10`.
- Frozen DF07 implementation at commit `8e156fbf8`, especially `MesQaInspectionRegulationService#getLockedVersionProcessesForOrder`.

## Requirement Coverage

| Requirement | Evidence | Result |
| --- | --- | --- |
| DF10 ownership | Git status contains only the three owned service/VO production files, the owned context-service test, and DF10 task records | PASS |
| Dedicated activeOrderId service projection | `MesFrontlinePqcContextService#listProcessesByActiveOrder(Long)` exists and reads the selected active order | PASS |
| Locked PUBLISHED/RETIRED version, disabled DCC tolerated | Unit fixture uses RETIRED and a mismatching `currentVersionId`; implementation does not read current QA or DCC enabled state | PARTIAL |
| Reuse frozen DF07 locked-version boundary | DF10 calls a new private `resolveLockedQaProcessSource`; it injects no `MesQaInspectionRegulationService` and never calls `getLockedVersionProcessesForOrder` | FAIL |
| All QA processes and complete published items | Response and test cover two processes, zero-task process, full section-2 item fields, applicability and source fields | PASS |
| Four canonical rules and AM/PM separation | Test asserts FIRST, PATROL_AM, PATROL_PM, FINAL and ruleSort 10/20/30/40 | PASS |
| Task summary/status/options | Response includes taskSummary; test covers PENDING/SUBMITTED/CONFIRMED/CANCELLED, MIXED and NOT_CREATED | PASS |
| Production candidate ownership and sorting | Active-order process snapshots filter events; test asserts exclusion and `serverSubmitTime DESC, eventId DESC` result | PASS |
| Batched reads / no per-process query | New projection loads tasks, rules, equipment, items, process snapshots and events before the QA-process loop | PASS |
| Production-route response unchanged and module compiles | `MesFrontlineRouteProcessRespVO` is unchanged, but the shared PQC VO removal of two existing setters breaks `MesFrontlineDeviceAccountController` compilation | FAIL |
| No current QA, product/material inference, formBindings, QA-to-MES process validation | Added-line scans pass; routeProcessId/processId are used only for order-level production-event ownership | PASS |
| No fallback/compatibility/default-success/swallowed error | No such added path was found | PASS |
| BDD/RED/GREEN evidence proves current code | Historical evidence claims five tests green, but the mandatory Maven command now fails during production compilation before tests run | FAIL |

## Verification Commands

- `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - FAIL at 2026-08-13 22:08:59 +08:00 during `yudao-module-mes` compilation.
  - `MesFrontlineDeviceAccountController.java:337` cannot find `setAcceptanceStandard(String)`.
  - `MesFrontlineDeviceAccountController.java:338` cannot find `setProcessInspectionMethod(String)`.
  - Tests did not run, so the current code has no valid GREEN evidence.
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260812-frontline-pqc-dcc-qa-df10\backend-api-evidence.md`
  - PASS: Backend API evidence is structurally valid.
- `git diff --check`
  - PASS with LF-to-CRLF advisory warnings only.
- Git ownership and full-diff review
  - PASS for path ownership; no controller, frontend, schema, mapper, or production-route VO is changed.
- Precise added-line forbidden scan
  - PASS for fallback, compatibility, formBindings, current-QA lookup, product/material inference, QA-to-MES process matching, and catch blocks.
- Frozen-boundary scan
  - FAIL: `MesFrontlinePqcContextServiceImpl` contains no `MesQaInspectionRegulationService` or `getLockedVersionProcessesForOrder` reference and instead defines `resolveLockedQaProcessSource` at line 750.

## Findings

1. [P0] The required Maven gate fails, so DF10 is not buildable and its tests do not run. `MesFrontlinePqcProcessRespVO.PqcInspectionItem` removes `acceptanceStandard` and `processInspectionMethod`, while unchanged `MesFrontlineDeviceAccountController` still invokes both setters at lines 337-338 (and the same mapping also appears at lines 412-413). This is a direct compile-time regression in the current worktree. The fix must keep the canonical published-item contract without adding a compatibility shim; coordinate the caller migration within an explicitly owned integration scope.
2. [P1] DF10 violates its explicit dependency contract by reimplementing the locked-QA reader instead of calling DF07. `DF10-process-page-projection.md` requires the QA read to call DF07 `getLockedVersionForOrder`; the frozen code exposes `MesQaInspectionRegulationService#getLockedVersionProcessesForOrder`. DF10 instead adds `resolveLockedQaProcessSource` and directly queries regulation, version, process, and item mappers. This duplicates lifecycle/identity validation, bypasses the DCC-existence check in DF07, uses different errors, and leaves two independently drifting locked-version boundaries.
3. [P1] The recorded GREEN evidence is stale relative to current source. The task records say the target command passes 5 tests, but a clean reactor compile from the current worktree fails before Surefire. Passing the evidence-file validator and static scans cannot establish executable correctness.

## Residual Risks

- Once the compile regression is repaired, DF10 still needs a fresh RED/GREEN cycle proving that the projection actually invokes the frozen DF07 boundary and that disabled-DCC historical PUBLISHED/RETIRED orders remain readable through that boundary.
- The full-field unit assertions are useful, but they currently mock DF10's duplicated mappers rather than the dependency contract; they would not detect future drift between DF07 and DF10.
- No controller or real HTTP verification was required for this task slice, but successful module compilation is a minimum prerequisite and currently fails.

## Decision

FAIL

DF10 cannot advance: the mandatory Maven command fails compilation, and the implementation bypasses the explicitly frozen DF07 locked-version reader by duplicating its responsibility.

## Follow-Up Actions

1. Return DF10 to its worker with the compile failure and exact controller/VO call sites; remove the canonical-field regression without introducing alias or compatibility fields.
2. Replace `resolveLockedQaProcessSource` with the frozen DF07 locked-version service boundary. If the frozen boundary lacks the complete aggregate needed by DF10, repair that dependency through an explicitly approved ownership revision rather than copying the reader.
3. Add a test that mocks/verifies the DF07 boundary call using the three active-order snapshot IDs and proves PUBLISHED/RETIRED behavior without current-version or enabled-DCC lookup.
4. Rerun the exact Maven command, evidence validator, diff check, ownership scan, forbidden scan, and a new independent verification round before merge.
