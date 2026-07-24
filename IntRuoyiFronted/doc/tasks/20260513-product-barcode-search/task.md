# Task: ERP product barcode search frontend

## Goal

Add a `产品编码/条码` search input on the ERP product page so users can find synced Kingdee products directly by code from the frontend.

## Scope

- Add a barcode search field on the ERP product page.
- Bind the field to the existing product page request.
- Preserve the current page layout, permissions, and refresh behavior.
- Verify the real browser path with Playwright and record BDD/TDD evidence.

## Milestones

- [x] M1: Previous frontend task reviewed and confirmed completed before starting.
- [x] M2: Frontend task directory and initial task document created before production code changes.
- [x] M3: Record BDD and RED evidence for the missing frontend barcode search entry.
- [x] M4: Implement the search field and request binding.
- [x] M5: Run real-page verification, update evidence, and prepare scoped frontend commit.

## Expected Verification

- The ERP product page shows a `产品编码/条码` search input.
- Searching by a known barcode returns the matching products in the table.
- The page still supports the existing name/category filters.

## Current Status

Completed.
