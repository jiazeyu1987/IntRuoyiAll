# Task: Kingdee product master sync frontend

## Goal

Expose a dedicated `同步金蝶` action on the ERP product page so users can sync product master data directly from Kingdee.

## Scope

- Add a dedicated toolbar action on the ERP product page.
- Add the matching frontend API binding.
- Refresh the visible product and category data after a successful sync.

## Milestones

- [x] M1: Previous frontend task reviewed and confirmed completed before starting.
- [x] M2: Frontend task directory and initial task document created before production code changes.
- [x] M3: Record BDD and RED evidence for the missing dedicated product sync entry.
- [x] M4: Implement button/API wiring and success feedback.
- [x] M5: Verify the real page path and update evidence.
- [x] M6: Prepare a scoped frontend commit.

## Expected Verification

- The ERP product page shows a dedicated `同步金蝶` button.
- Clicking the button calls the dedicated product sync API.
- The product list refreshes after success.

## Current Status

Completed.
