# Task: Kingdee purchase order synchronization

## Goal

Add a backend API that synchronizes purchase orders from Kingdee K3Cloud into IntRuoyi ERP purchase orders, and support the frontend manual trigger.

## Scope

- Add a fail-fast Kingdee K3Cloud purchase order sync service and endpoint.
- Persist Kingdee source mappings for idempotency.
- Add backend tests before production code.
- Record schema, API, and integration evidence.
- Do not copy demo hardcoded credentials, DingTalk approval flow, SQLite storage, or endpoint fallback behavior.

## Milestones

- [x] M1: Previous backend task checked and completed.
- [x] M2: Backend task documentation created in the backend repository before production code changes.
- [x] M3: RED tests written for service idempotency, missing config, client payload, and sync-record schema.
- [x] M4: Backend service, Kingdee client, endpoint, persistence, SQL, and permission implemented.
- [x] M5: Targeted backend verification passes.
- [x] M6: Evidence updated and backend task marked completed.
- [x] M7: Backend changes committed on `feature/kingdee-purchase-order-sync`.

## Expected Verification

- A Kingdee source purchase order creates one IntRuoyi purchase order and records the source `FormId + FID`.
- An already-recorded source `FormId + FID` is skipped without creating a duplicate.
- Missing Kingdee endpoint or credentials fails fast before contacting Kingdee or creating local orders.
- The sync record table has a uniqueness constraint on `source_form_id + source_fid`.
- The Kingdee client uses the configured endpoint directly and does not silently try alternate URL variants.

## Current Status

Completed on `feature/kingdee-purchase-order-sync`. Targeted backend tests and evidence validation passed. Live Kingdee verification is blocked until real Kingdee endpoint, credentials, and master-data mappings are provided.
