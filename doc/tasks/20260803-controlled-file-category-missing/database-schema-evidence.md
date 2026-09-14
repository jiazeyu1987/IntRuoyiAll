# Database Schema Evidence

## Data Change Goal

Seed a formal DCC upload directory row with code `UNCLASSIFIED` and display name `未分类` so categories without submit-directory binding can be placed under a real auditable directory instead of prompting submitters to maintain category-directory bindings.

## Engine And Migration Tool

- Database engine: MySQL.
- Migration location: `IntRuoyiBackend/sql/mysql/20260803_dcc_unclassified_upload_directory_seed.sql`.
- Policy gate: `IntRuoyiBackend/script/release/run-release-migration-policy-gate.py`.

## Data Safety

- Non-destructive seed only; no `DELETE`, `TRUNCATE`, or `ON DUPLICATE KEY UPDATE`.
- Uses `NOT EXISTS` for idempotent insert.
- Uses ASCII-safe `CONVERT(UNHEX('E69CAAE58886E7B1BB') USING utf8mb4)` for `未分类`.
- Fails fast if `dcc_file_directory` is missing, if more than one active `UNCLASSIFIED` directory exists per tenant, or if insert verification is incomplete.

## Rollback

- Seed is additive and idempotent. If rollback is required, disable or remove only the task-owned `UNCLASSIFIED` rows after confirming no controlled files reference the directory; no automatic destructive rollback is included.

## BDD

- BDD: Seed formal unclassified directory -> Given DCC base schema exists When the seed migration runs Then each discovered DCC tenant has an active `UNCLASSIFIED / 未分类` directory if one did not already exist.
- BDD: Duplicate active unclassified directories -> Given duplicate active `UNCLASSIFIED` directories exist for a tenant When the seed migration runs Then it raises `DCC_UNCLASSIFIED_UPLOAD_DIRECTORY_SEED_DUPLICATE_ACTIVE`.

## RED

- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_unclassified_upload_directory_seed_sql.py -q` -> FAIL, expected reason: missing seed SQL file.

## GREEN

- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_unclassified_upload_directory_seed_sql.py -q` -> PASS.
- GREEN: DCC base + unclassified seed migration policy gate -> PASS, report written to `doc/tasks/20260803-controlled-file-category-missing/migration-policy-gate-unclassified.json`.

## Verification

- PASS: static SQL contract confirms release metadata, procedure name, fail-fast signals, idempotent insert, and non-destructive constraints.
- PASS: migration policy gate validates the DCC base schema dependency plus this seed migration.

## Blockers

- Full SQL-root migration policy gate fails before this migration on unrelated `20260730_mes_process_pool_team_leader.sql` missing release metadata.
