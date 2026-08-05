# Backend API Evidence

## Scope

- Endpoint: `GET /mes/qa/inspection-regulation/project-statuses`
- Controller: `MesQaInspectionRegulationController#getProjectStatuses`
- Service: `MesQaInspectionRegulationService#getProjectStatuses`
- Mapper: `MesQaInspectionRegulationMapper#selectListByProductIds`
- Response VO: `MesQaInspectionRegulationProjectStatusRespVO`

## Contract

- Request: `productIds` query parameter accepts DCC project-code bound MDM product IDs.
- Response: one status row per requested product ID, preserving first-seen request order after de-duplication.
- Data source: existing `mes_qa_inspection_regulation.product_id`; no schema change and no DCC-name inference.
- Configured rule: a product is configured when at least one QA inspection regulation exists for that product.
- Representative regulation: prefer `PUBLISHED`, then `DRAFT`, then other lifecycle statuses; tie-break by current version ID and regulation ID.
- Auth: endpoint keeps current QA read permission `mes:qc-template:query`.

## Validation

- Empty or null product ID collection returns an empty list.
- Product IDs with no matching QA regulation return `configured=false` and `regulationCount=0`.
- Product IDs with matching QA regulations return regulation ID, current version ID, code, name, lifecycle status, and count.
- Missing backend data is not silently treated as success in the frontend; status API load errors are visible.

## BDD: Scenarios

- `BE-BDD-1`: Given DCC project codes are bound to MDM product IDs / When QA loads project statuses / Then the API returns configured and unconfigured status rows by product ID.
- `BE-BDD-2`: Given a product has both draft and published QA regulations / When status is requested / Then the representative status prioritizes the published regulation.
- `BE-BDD-3`: Given a product has no QA regulation / When status is requested / Then the API returns `configured=false` without inventing a regulation.

## RED: Contract Failure

- `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL.
- Expected reason: frontend contract required `getQaRegulationProjectStatuses` and `/mes/qa/inspection-regulation/project-statuses`, but the formal API wrapper/page integration was absent.
- `mvn -pl yudao-module-mes "-Dtest=MesQaInspectionRegulationServiceTest" test` -> FAIL.
- Expected reason: new backend test referenced `MesQaInspectionRegulationProjectStatusRespVO` and `getProjectStatuses`, which were not yet implemented.

## GREEN: Verification

- `mvn -pl yudao-module-mes "-Dtest=MesQaInspectionRegulationServiceTest" test` -> PASS.
- Result: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`.

## Verification

- Backend unit coverage validates configured and unconfigured product statuses in request order.
- Frontend static contract validates use of the formal status API and rejects hardcoded configured project sets.
- TypeScript verification validates the frontend API wrapper and page state integration.

## Blockers

- No backend API blocker remains for QA project configuration status.
- QA regulation save/publish persistence remains out of scope for this task.
