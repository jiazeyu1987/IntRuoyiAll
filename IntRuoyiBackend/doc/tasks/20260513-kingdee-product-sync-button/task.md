# Task: Kingdee product master sync action

## Goal

Add a dedicated Kingdee material-to-ERP-product sync action so operators can sync ERP product master data directly instead of relying only on downstream order or stock imports to create products indirectly.

## Scope

- Backend: add a dedicated Kingdee material query client and product sync service.
- Backend: add a product sync endpoint under `/admin-api/erp/product`.
- Backend: create or update ERP product, product category, and product unit records from approved Kingdee material master data.
- Frontend: add a `同步金蝶` action on the ERP product page and refresh product/category data on success.

## Milestones

- [x] M1: Previous backend task reviewed and confirmed completed before starting.
- [x] M2: Backend task directory and initial task document created before production code changes.
- [x] M3: Confirm Kingdee material-master query fields and sync scope.
- [x] M4: Record BDD and RED evidence for missing dedicated product sync.
- [x] M5: Implement backend sync client/service/endpoint and tests.
- [x] M6: Implement frontend button/API wiring and verification.
- [x] M7: Run targeted verification, update evidence, and commit scoped backend/frontend changes.

## Expected Verification

- `POST /admin-api/erp/product/sync-kingdee` returns product sync counts.
- Approved Kingdee materials create or update ERP product master records.
- ERP product page exposes a dedicated `同步金蝶` button.
- No silent fallback or mock-success path is introduced.

## Current Status

Completed.
