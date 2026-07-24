# Task: ERP schema repair

## Goal

Fix `[ERP 系统 yudao-module-erp - 表结构未导入]` by making the ERP MySQL base schema available in this repository and verifying the runtime database has the required ERP tables.

## Scope

- Identify the missing ERP table root cause.
- Add or use a MySQL schema script for ERP base tables and the Kingdee sync table.
- Provide a repeatable verification script for the configured local database.
- Do not suppress the global missing-table exception.
- Do not import third-party or guessed SQL dumps.

## Milestones

- [x] M1: Previous backend task state checked before this repair.
- [x] M2: Task documentation created before schema changes.
- [x] M3: RED verification records missing ERP schema.
- [x] M4: ERP base schema script and verification tooling added.
- [x] M5: Local database schema verified or exact blocker recorded.
- [x] M6: Evidence updated and task finalized.
- [x] M7: Task changes committed separately.

## Expected Verification

- The verification script fails before ERP base tables exist.
- The repository contains a MySQL schema script covering ERP base tables required by `yudao-module-erp`.
- The configured local database contains core tables such as `erp_product`, `erp_supplier`, `erp_purchase_order`, `erp_purchase_order_item`, and `erp_kingdee_purchase_order_sync_record`.

## Current Status

Completed. Final verification passed and task changes are ready for the dedicated task commit.
