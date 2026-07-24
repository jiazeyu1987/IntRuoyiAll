# Task: ERP product barcode search backend

## Goal

Add backend support for querying ERP products by product code/barcode so the frontend can find products by exact codes such as `YXN.037.011.1004`.

## Scope

- Extend ERP product page request parameters with a barcode filter.
- Apply the barcode filter in the ERP product page query.
- Keep the existing page contract and permissions unchanged.
- Add targeted regression tests and record BDD/TDD evidence.

## Milestones

- [x] M1: Previous backend task reviewed and confirmed completed before starting.
- [x] M2: Backend task directory and initial task document created before production code changes.
- [x] M3: Record BDD and RED evidence for the missing barcode filter behavior.
- [x] M4: Implement backend barcode query support and targeted tests.
- [x] M5: Run targeted verification, update evidence, and prepare scoped backend commit.

## Expected Verification

- `GET /admin-api/erp/product/page` accepts a barcode query parameter.
- Product page queries can return products matched by exact or partial barcode text.
- No fallback or silent downgrade is introduced.

## Current Status

Completed.
