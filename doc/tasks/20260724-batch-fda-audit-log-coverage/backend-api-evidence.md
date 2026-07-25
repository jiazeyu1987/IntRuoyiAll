# Backend API Evidence - Local State Sample Audit Trace Scope

## Contract

- Scope: `POST /admin-api/mes/pro/edhr-batch-execution/local-state-sample` and batch trace `GET /admin-api/mes/pro/edhr-operation-audit/page?batchExecutionId=...`.
- API contract: Local-only authorized sample creation returns `batchExecutionId`, `batchExecutionCode`, `sampleState`, and detail route query. Batch trace operation audit remains authorized by real object-level permission gate.
- Data contract: Created local sample batch task stores `permissionScopeId` referencing a `BATCH_EXECUTION_TASK` scope with `AUDIT_VIEW=ALLOW` for the creating user.
- Auth and permissions: The sample endpoint still requires local profile, `芋道源码/admin` tenant context, and create menu permission; no API-only bypass or fallback permission repair was added.
- Validation and error behavior: Missing permission scope still fails fast through `PRO_EDHR_PERMISSION_SCOPE_REQUIRED`; sample creation now fails if scope creation does not return a scope ID.
- Observability: `LOCAL_STATE_SAMPLE_CREATE` audit metadata retains request source, idempotency key, permission decision, result status, and created-record payload includes batch task permission scope ID.

## BDD

- BDD: Local state sample audit trace scope -> Given an authorized local admin creates a PRECHECK sample from the frontend When the batch detail trace drawer queries operation audit by batchExecutionId Then the created batch task has an AUDIT_VIEW permission scope so the permission gate can authorize the trace.

## Verification

- RED: Real UI E2E failed at operation audit page with missing `BATCH_EXECUTION` permission scope.
- RED: Static contract failed before implementation.
- GREEN: Static contract PASS after implementation.
- BLOCKED: Full Java compile and runtime E2E rerun are blocked by unrelated route projection compile errors.

## Blockers

- Full Java compile and runtime E2E rerun are blocked by unrelated route projection compile errors.

## Final E2E Pass - 2026-07-25 15:25 Asia/Shanghai

- BDD: Batch trace displays FDA operation audit -> Given the clean runtime jar includes the local sample AUDIT_VIEW scope fix When the authorized admin creates a PRECHECK sample through the real frontend Then the trace drawer queries operation audit by batchExecutionId and displays LOCAL_STATE_SAMPLE_CREATE.
- GREEN: Runtime jar SHA256 `1DC505A97E6BD91F94F0D975A6F404E7469DAE92F1960833DD9DCE05B241DC35`, PID `50968`, health `UP`.
- GREEN: E2E PASS for sample `900000000799` / `EDHR-UI-SAMPLE-PRECHECK-20260725152451737`.
- Verification: UI request `/mes/pro/edhr-operation-audit/page?pageNo=1&pageSize=10&batchExecutionId=900000000799`; objectType/objectId omitted; audit result `ALLOW` / `SUCCESS`.
- Blockers: none for requested E2E path; cleanup/commit/push remains a closeout activity.
