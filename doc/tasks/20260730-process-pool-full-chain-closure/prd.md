# PRD

## Goal
Close the formal production frontline report-work and process-pool chain while reusing the existing report-work, recordbook, process-pool, batch-record, and electronic-signature architecture.

## Scope
- Production binding sources for device-account routes and employee fixed templates.
- Frontline production and PQC fixed-template pages submit to real backend APIs.
- PQC results create formal process-pool inspection events.
- FIFO consumes available pool fragments for production work orders sorted by `plannedStartTime`.
- Review copies are generated from formal limit rules by clamping out-of-range values while retaining original values.
- Team leaders can see submitted/pending/exception process-pool work in one workbench.
- Full E2E verifies the chain through real pages when runtime/test data prerequisites exist.

## Non-Goals
- No scheduling system integration.
- No arbitrary dynamic form builder for frontline templates.
- No route-order gate on actual frontline submission.
- No mock data, placeholder success, API-only E2E, or silent fallback.

## User or System Scenarios
- Operator selects bound process and employee, fills previous process input quantity, device parameters, output quantity, and loss quantity, then submits once.
- PQC user selects process and employee, records pass/fail and quantities, then submits once.
- Production work orders consume pool quantities FIFO by planned start time.
- Reviewer sees both original data and clamped audit copy for out-of-range fields.
- Team leader monitors submissions, PQC state, allocation state, and exceptions.

## Functional Requirements
- FR-01: Device accounts load only formally bound active route/process/device options.
- FR-02: Employee switching is limited to employees bound to the selected process/workstation and loads that employee's fixed template.
- FR-03: Production submit writes feedback, recordbook entry/event, process-pool event, and raw payload in one transaction.
- FR-04: PQC submit writes recordbook/process-pool PQC event and maps UI pass/fail into pool `SUCCESS/FAILURE`.
- FR-05: FIFO orchestration selects production work orders by `plannedStartTime`, then consumes available fragments in FIFO order.
- FR-06: Review copy generation clamps configured numeric values to min/max and stores original and adjusted values.
- FR-07: Team-leader workbench displays submitted events, PQC status, FIFO status, review-copy status, and exceptions.

## Non-Functional Requirements
- NFR-01: All missing formal bindings, schema, signatures, or runtime data fail fast with visible errors.
- NFR-02: Original event data is immutable except through formal revision APIs with logs/signatures.
- NFR-03: Frontend submit must expose backend errors and must not show success before the real submit returns.
- NFR-04: E2E evidence must distinguish static PASS, backend PASS, frontend PASS, real E2E PASS, and blocked prerequisites.

## Dependencies and Constraints
- Existing MES production work order planned start time field.
- Existing electronic signature infrastructure.
- Existing process-pool migrations and services.
- Worktree runtime profile `int_main slot=1`.

## Acceptance Criteria
- AC-01: Binding source production beans are present and covered by tests; missing bindings fail fast.
- AC-02: Production frontline UI calls `/mes/pro/feedback/frontline/submit` and never treats template validation alone as submission.
- AC-03: PQC UI submits through a formal backend path and creates pool PQC records with `SUCCESS/FAILURE`.
- AC-04: FIFO allocation API/job orders production work orders by planned start time and records allocation lines.
- AC-05: Review-copy auto rule clamps low/high numeric values and preserves original plus adjusted values.
- AC-06: Team-leader workbench has a real route/API and displays pool events without browser-storage or mock data.
- AC-07: Full E2E either passes through real pages on `8082/48082` or records exact missing runtime/test-data prerequisites.
