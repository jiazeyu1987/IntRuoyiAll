# Task: ERP Kingdee config management frontend

## Goal

Add a dedicated ERP configuration management page so users can view and edit the Kingdee connection and sync settings inside the ERP system.

## Scope

- Add an ERP config page under the ERP system menu.
- Display current effective Kingdee settings with runtime defaults.
- Save config changes through a dedicated ERP backend API.
- Keep the page consistent with the existing single-page config management patterns used in other modules.

## Milestones

- [x] M1: Previous frontend task reviewed and confirmed completed before starting.
- [x] M2: Frontend task directory and initial task document created before production code changes.
- [x] M3: Record BDD and RED evidence for the missing ERP config page.
- [x] M4: Implement the ERP config page and frontend API bindings.
- [x] M5: Verify the real page path, update evidence, and prepare scoped frontend commits.

## Expected Verification

- The ERP system shows a dedicated config management page/tab.
- The page loads current effective Kingdee settings.
- Saving changes shows success feedback and refreshes the visible config.

## Current Status

Completed.
