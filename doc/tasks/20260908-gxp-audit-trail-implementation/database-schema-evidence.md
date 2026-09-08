# Database Schema Evidence - 20260908 GxP Audit Trail Implementation

## Data Change Goal and Affected Entities

Implement additive schema for unified GxP audit trail ledger, policy operation registry, coverage report registry, daily manifest and seal watermark. Affected entities: `gxp_audit_event`, `gxp_audit_policy_version`, `gxp_audit_policy_operation`, `gxp_audit_coverage_report`, `gxp_audit_daily_manifest`, `gxp_audit_seal_watermark`, and H2 unit-test mirrors.

## Database Engine and Migration Tool

MySQL migration SQL under `IntRuoyiBackend/sql/mysql/20260908_gxp_audit_trail_core.sql`; H2 unit-test schema mirrors under `IntRuoyiBackend/yudao-module-system/src/test/resources/sql/create_tables.sql`.

## Schema Changes

- Added policy operation registry table with operation/source/domain/action/reason/signature/state/retention/test/owner/applicability fields.
- Added idempotency payload hash on `gxp_audit_event` so same idempotency key can replay only identical canonical payloads.
- Kept tenant ledger sequence uniqueness and idempotency uniqueness.
- Kept append-only triggers for event, daily manifest and seal watermark update/delete attempts.

## Data Safety Analysis

Additive schema only. No destructive migration, no historical backfill, no silent coercion, no default-success data repair. The SQL contract test explicitly rejects `DROP TABLE`, `TRUNCATE TABLE`, and `DELETE FROM` in the core migration.

## Rollback or Recovery Plan

Before production execution, rollback must be a controlled migration rollback in an approved maintenance window. No destructive rollback script is generated in this task without explicit approval because audit ledgers are append-only compliance records.

## BDD Scenarios

BDD: GXP audit schema is append-only -> Given the unified audit migration is reviewed, When the SQL contract checks required tables, unique ledger/idempotency keys and no-update/no-delete triggers, Then the migration provides an additive append-only audit ledger foundation.

BDD: GXP audit coverage registry is authoritative -> Given policy operation records and annotated write entries exist, When the coverage gate scans config and source, Then unregistered annotated writes or dangling source locators fail CI.

## RED Command and Expected Failure

RED: `python -X utf8 script/gxp_audit_coverage_gate.py --root . --policy config/gxp-audit-policy.yaml` -> FAIL, expected scanner reason: service method source locator resolver looked at repository root instead of `**/src/main/java` trees.

## GREEN Command and Passing Result

GREEN: `python -X utf8 script/gxp_audit_coverage_gate.py --root . --policy config/gxp-audit-policy.yaml` -> PASS, operations=8, annotations=7, sha256=61a0206128d8d0d0fbf41b736a22639eddd3569af8638c628fac554dd41f13e6.

GREEN: `python -X utf8 -m pytest script/tests/test_gxp_audit_core_contract.py -q` -> PASS, 2 tests passed.

GREEN: `mvn -pl yudao-module-system "-Dtest=GxpAuditPersistenceModelTest,GxpAuditServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 10 tests passed.

## Migration Verification

Static SQL contract verifies required core tables, idempotency hash, unique ledger/idempotency constraints, and append-only triggers. Runtime migration execution against a real MySQL instance was not performed in this turn because no database-write authorization was requested for this implementation pass.

## Blockers

- Runtime operational evidence (NTP, WORM/Object Lock, backup restore rehearsal, SOP/signature/training) is not a code-only deliverable and remains BLOCKED until real evidence is supplied.
- Production DB role grants for INSERT-only audit writer, SELECT-only audit reader, and privileged DBA external audit require environment-specific execution and approval.
