# Task: MES item master Kingdee sync backend

## Goal

Add a dedicated Kingdee material-master sync action for the MES item master page so MES `mes_md_item` can mirror the Kingdee material list directly.

## Scope

- Add a MES sync endpoint under `/admin-api/mes/md/item`.
- Sync Kingdee material master data directly into `mes_md_item`, `mes_md_item_type`, and `mes_md_unit_measure`.
- Disable missing MES items instead of deleting them.
- Preserve MES-specific fields on update.
- Record BDD and strict TDD evidence for backend behavior.

## Milestones

- [x] M1: Previous backend task reviewed and confirmed completed before starting.
- [x] M2: Backend task directory and initial task document created before production code changes.
- [x] M3: Record BDD and RED evidence for missing MES Kingdee item sync behavior.
- [x] M4: Implement backend sync service, endpoint, and targeted tests.
- [x] M5: Run targeted verification, update evidence, and prepare scoped backend commit.

## Expected Verification

- `POST /admin-api/mes/md/item/sync-kingdee` returns created/updated/disabled/skipped counts.
- MES item master can mirror Kingdee product materials directly.
- Missing MES items are disabled, not deleted.
- No fallback or silent downgrade is introduced.

## Current Status

Completed.
