# Task: ERP Kingdee config management backend

## Goal

Add a dedicated ERP configuration management backend so operators can view and edit the Kingdee connection and sync settings from the ERP system, with defaults populated from the current local runtime configuration.

## Scope

- Add ERP config get/save endpoints.
- Persist the editable config via existing `infra_config` storage.
- Return current `.env`/runtime Kingdee settings as defaults when no saved config exists.
- Make ERP and MES Kingdee sync services read the effective saved config instead of only the boot-time bean defaults.
- Prepare and apply a menu seed SQL script for the ERP config page entry.

## Milestones

- [x] M1: Previous backend task reviewed and confirmed completed before starting.
- [x] M2: Backend task directory and initial task document created before production code changes.
- [x] M3: Record BDD and RED evidence for missing ERP config management.
- [x] M4: Implement backend config get/save, effective-config provider, and sync-service integration.
- [x] M5: Add menu seed SQL, run targeted verification, update evidence, and prepare scoped backend commits.

## Expected Verification

- `GET /admin-api/erp/kingdee-config/get` returns current effective Kingdee config.
- `PUT /admin-api/erp/kingdee-config/save` persists updated config.
- ERP/MES sync actions use saved config values after restart-free save.
- ERP menu can expose a dedicated config page entry.

## Current Status

Completed.
