# Task: MES item master Kingdee sync frontend

## Goal

Expose a dedicated `同步金蝶` action on the MES item master page so users can sync MES item master data directly from Kingdee.

## Scope

- Add a dedicated toolbar action on the MES item master page.
- Add the matching frontend API binding.
- Default the page status filter to enabled items.
- Refresh the type tree and item list after a successful sync.
- Record BDD and strict TDD evidence for the frontend behavior.

## Milestones

- [x] M1: Previous frontend task reviewed and confirmed completed before starting.
- [x] M2: Frontend task directory and initial task document created before production code changes.
- [x] M3: Record BDD and RED evidence for the missing MES Kingdee sync entry.
- [x] M4: Implement button/API wiring and enabled-status default behavior.
- [x] M5: Run real-page verification, update evidence, and prepare scoped frontend commit.

## Expected Verification

- The MES item master page shows a dedicated `同步金蝶` button.
- Clicking the button calls the MES Kingdee item sync API.
- The page defaults to enabled items and refreshes list/tree after success.

## Current Status

Completed.
