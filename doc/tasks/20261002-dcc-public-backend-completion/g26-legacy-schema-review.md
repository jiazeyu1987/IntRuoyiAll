# G26 standalone legacy sidecar schema review

## Scope and result
Readonly static review of `IntRuoyiBackend/sql/mysql/20261003_dcc_legacy_source_name_occupancy.sql` for the actual planned MySQL 8.0.40 environment. No production edits, Maven, actual DB writes/reads, object actions or Git by this reviewer. Latest inventory snapshot after legitimate DATETIME(6) precision fix: SHA-256 `621fe041064ac07c5bfe995abd556c3f9436b0253c883a3b7d2b33014e68de2a`, superseding `2c7451a09f88c46c9243cda6ca89acfbd5f101da4fb108473a4da818da9f42eb`. Later source changes require inventory refresh. Source is concurrently owned by legacy developer.

Static schema parsing, historical-DML exclusion and the repository's **actual `run_migration_policy_gate`** passed for the target's full dependency closure. This is not first-run/replay MySQL evidence. No static fatal MySQL REGEXP/generated/index syntax incompatibility was found. Existing-table shape checks, actual row-format/page-size, real isolated first/repeat and final SHA/ledger validation remain execution gates.

Machine-readable exact columns/declarations/indexes/CHECKs and the actual gate report are in `g26-legacy-schema-inventory.json`. No new validator or unrelated repository tests were needed for this document-only review.

## Exact schema impact
| New table | Columns | Nullable ordinary columns | Generated columns | Secondary indexes | CHECK clauses |
|---|---:|---|---|---:|---:|
| dcc_legacy_source_name_scope | 19 | activated_at | none | 2 unique | 3 |
| dcc_legacy_source_name_evidence | 36 | storage_region, claim_project_id, claim_leaf_id, claim_number, master_project_id, master_leaf_id, master_number, obsolete_time, retain_until, released_time | source_name_key varbinary(1024), STORED | 2 unique + 1 ordinary | 2 |
| dcc_source_name_reservation | 15 | verification_scope_id, modern_claim_id, modern_master_id, actor_id, reason, released_time | source_name_key varbinary(1024), STORED | 1 unique | 2 |

Total **3 new tables, 70 columns, 2 STORED generated keys, 6 secondary indexes plus 3 primary keys, 7 CHECK clauses**. Tables explicitly use InnoDB/utf8mb4/utf8mb4_bin. Scope ID and all hashes use ascii/ascii_bin; version_no also ASCII binary. All primary IDs and tenant IDs are bigint, retained externally as exact Long strings. New columns for source locator are `storage_type`, `storage_endpoint`, `storage_bucket`, optional `storage_region`, `storage_path_style`; no access key/secret is stored. Source_path and endpoint varchar(1024) are not unique or indexed.

DDL contains `SET NAMES` and these three `CREATE TABLE IF NOT EXISTS` only; no old File/Master/claim updates, backfill, INSERT seed, ALTER, DROP, DELETE, migration-ledger write or quality policy action. Root's driver may add **one new exact migration ledger row** after all schema postflight proves this exact file. Registration (later scope/evidence/reservation data and audit) is separate from the empty DDL and needs its own concrete authorized scope.

## Metadata closure and ledger boundary
Target ID is `20261003_dcc_legacy_source_name_occupancy`, schema/medium, allowed environments test/backup/prod; direct dependencies are C identity and source ownership. Actual closure contains **8** files:

1. 20260513_dcc_base_schema
2. 20260811_dcc_source_ownership
3. 20260906_dcc_new_file_lifecycle_p1
4. 20260906_dcc_new_file_lifecycle_p2
5. 20260906_dcc_new_file_lifecycle_p3
6. 20260917_dcc_controlled_file_name_claim
7. 20260930_dcc_c_revision_identity
8. 20261003_dcc_legacy_source_name_occupancy

Use this closure for policy/structure verification; **do not execute all eight** against source. The earlier 19-migration upgrade already established dependencies under frozen old-ledger/source evidence. Standalone execution writes only new target DDL plus max one exact new ledger registration; do not replay old source-ownership/base/P1/P2/P3/C or old seeds, reinterpret recorded historical checksums, or overwrite the previous 19 receipts. Recheck live prerequisite schemas and ledger status against Root's actual upgraded source proof. If the new target's ledger already exists with different SHA/environment/state, stop. Exact same completed entry is repeat/skip, not an INSERT IGNORE or checksum rewrite.

## Actual readonly readiness and time precision refresh
Root's already collected protected `g27-legacy-schema-readiness.jsonl` and receipt were read and raw-SHA verified by this reviewer (no new DB connection). At 2026-10-03 12:28:03.704912 UTC, source UUID `92ca05d0-aec8-11f1-a944-02b4e226a5ef`, `ruoyi-vue-pro`, actual MySQL 8.0.40 has page size **16384** and default row format **dynamic**; target ledger rows 0 and no target table metadata rows, confirming the three new names were absent at that capture. Facts SHA `252b699b7ae740fbaac68ae4609bd26bca3b9ac8a7a9ab68a676361b78de8d6d`. Fresh pre-write recheck remains Root's execution gate; this historical readonly snapshot is not DDL authorization or first-run proof.

The actual source-byte receipt has fractional completed UTC time. The standalone SQL now uses **8 DATETIME(6) columns** to retain exact six decimal digits through persistence and replay:

- scope: verified_at NOT NULL; activated_at nullable.
- evidence: obsolete_time, retain_until, released_time nullable.
- reservation: create_time and update_time NOT NULL DEFAULT CURRENT_TIMESTAMP(6); released_time nullable.

Initial precision edit temporarily used DATETIME(6) with unqualified CURRENT_TIMESTAMP defaults. Reviewer flagged matching explicit precision; sole owner corrected both defaults to CURRENT_TIMESTAMP(6), and the refreshed offline inventory asserts every temporal column uses DATETIME(6) and all timestamp defaults use `(6)`. No real MySQL error is claimed from the intermediate text. Postflight must inspect DATETIME_PRECISION=6 and actual default expressions, not just DATA_TYPE=datetime. Activation uses actual server timestamp and verifiedAt comes from real completed reader receipt converted to Shanghai; no client epoch-to-zone guessing. Nanoseconds beyond six digits need the declared protocol boundary rather than a replay comparison silently losing precision.

## MySQL 8.0.40 CHECK / REGEXP assessment
MySQL 8.0.16+ enforces CHECK constraints and permits deterministic built-in operators/functions; NULL evaluation yields UNKNOWN and is accepted. The current schema's status/count/hash/bytes predicates reference NOT NULL columns, and registry nullable owner fields are explicitly tested `IS NULL`/`IS NOT NULL`. There is no auto_increment column reference inside CHECK, subquery, NOW(), user/system variable, stored function or UDF. [Official CHECK rules](https://dev.mysql.com/doc/refman/8.0/en/create-table-check-constraints.html).

The `REGEXP` hash subjects are **CHAR(64) ASCII with ascii_bin collation**, not BINARY/VARBINARY strings. MySQL 8.0.22+ rejects binary-string regex subjects; that restriction does not prohibit the current text-typed hash columns merely because their collation ends `_bin`. Do not “fix” these CHECKs by casting the subject AS BINARY. [Official regex rules](https://dev.mysql.com/doc/refman/8.0/en/regexp.html), [Oracle MySQL 8.0.40 regex implementation](https://github.com/mysql/mysql-server/blob/mysql-8.0.40/sql/item_regexp_func.h).

`^[0-9a-f]{64}$` is a constant pattern; column collation controls case sensitivity. Valid lowercase hashes pass; uppercase, nonhex, shorter or longer values must fail actual INSERT/UPDATE in the isolated first-run verification. Current SQL-mode and CHECK enforcement should be captured, not inferred from an H2 fixture. Verify all seven entries in CHECK_CONSTRAINTS/TABLE_CONSTRAINTS with ENFORCED=YES. MySQL assigns anonymous CHECK names as `<table>_chk_N`; schema comparison should normalize only the documented naming/order representation, not drop any expression. [Official CHECK metadata rules](https://dev.mysql.com/doc/refman/8.0/en/create-table-check-constraints.html).

Other required negative rows: invalid scope status/zero counts, mismatched expected-vs-actual bytes/hash, generation=0, illegal active/kind/owner combination. The current CHECKs do not authenticate evidence, enforce cross-table ownership or prove twenty-year retained/released lifecycle; those are strict formal service/registration/SQL-read guards and cannot be called proven merely because CREATE succeeded.

## Generated binary keys and index budget
`CONVERT(source_original_file_name USING BINARY)` deterministically copies the full UTF-8 bytes of the declared utf8mb4 varchar(256). It does not lowercase/trim/drop extension or truncate to a prefix. The maximum source bytes are 256*4=1024, fitting declared varbinary(1024). Both keys are STORED generated; application INSERT/UPDATE must omit generated keys. H2's `CAST(... AS VARBINARY)` fixture is deliberately engine-specific and is not a proof of MySQL syntax. [Official generated-column rules](https://dev.mysql.com/doc/refman/8.0/en/create-table-generated-columns.html).

Exact longest index budgets are:

- evidence `idx_dcc_legacy_name_owner`: tenant8 + full key1024 + Master8 = **1040 bytes**.
- reservation `uk_dcc_source_name_registry`: tenant8 + full key1024 = **1032 bytes**.
- scope tenant+ASCII scope/hash: **72 bytes** each.
- evidence unique claim-version/scope-version: **32/24 bytes**.

These fit 3072-byte DYNAMIC/COMPRESSED InnoDB indexes with 16 KiB pages, and 1536-byte 8 KiB limits, but **do not fit** the 767-byte COMPACT/REDUNDANT or 768-byte 4 KiB page limit. The SQL omits ROW_FORMAT and inherits the server default. Root must capture actual `@@innodb_page_size`, `@@innodb_default_row_format` and first-created table ROW_FORMAT; if defaults cannot support 1040 bytes, stop rather than shrinking the full key or dropping uniqueness. [Official InnoDB limits](https://dev.mysql.com/doc/refman/8.0/en/innodb-limits.html).

Boundary proof in isolated actual MySQL must include case variants, extension variants, trailing spaces, supplementary Unicode and maximum 1024-byte basenames. Assert `HEX(source_name_key)=HEX(source_original_file_name)` and no prefix `SUB_PART` on the name indexes. `utf8mb4_bin` comparisons of ordinary VARCHAR alone can have PAD SPACE semantics; the generated VARBINARY exact unique keys are what protects complete source-name byte identity.

## First-run and replay stoppers / concrete postflight checklist
`IF NOT EXISTS` can skip an already present table whose columns/indexes/CHECKs are wrong. Therefore an exit-zero SQL run alone is insufficient. Before execution, Root must see either all three target tables absent (fresh first run) or all three exactly matching the frozen expected contract (repeat); any partial or wrong shape stops **before** applying additional DDL.

After first run, and again after repeat, compare the exact inventory:

1. Table set exactly the three expected new names; InnoDB charset/collation/ROW_FORMAT supported. No unexpected table was touched.
2. All 70 columns by ordinal name/type/signedness/length/nullable/charset/collation/default/extra; 2 generated expressions AST-equivalent to full CONVERT(... USING BINARY), STORED. Do not accept fixture-only CAST, wrong utf8mb4 charset, reduced key length or newly supplied default for nullable old identities.
3. All 3 primary and 6 named secondary index column sequences, uniqueness, full key/no SUB_PART, no additional incompatible identity index.
4. Seven CHECK expressions and ENFORCED=YES, no drift hidden by anonymous names. First/repeat SHOW CREATE/table schema hashes equivalent, with only permitted automatic ID metadata normalization.
5. All three tables empty after schema-only apply; no evidence, VERIFIED scope, reserved name, actor or approval was seeded. Registration begins only under later scope approval.
6. Existing DCC File/Master/claim/source ownership/version/signature/BPM/old-ledger snapshots remain exact, including every existing row/column. New table creation is the only schema delta, and max one exact new migration ledger row is the only driver config delta.
7. First-run target ledger SHA matches current raw SQL; repeat creates no additional row/change. Existing dependency ledger hashes stay immutable, and the already upgraded 19 rows are not reinserted.

MySQL DDL is not one transaction across three CREATE statements: a failure creating table two can leave table one. Root must preserve exact failed receipt and resulting schema, do not auto-drop, auto-recreate, retry against source or label repeat idempotence as first-run proof. Use a task-owned isolated schema first, then fresh affected/ledger backup and writer exclusion for source, matching the already reviewed Root driver pattern. This child performs no actual DB operation.

## Backup / authorization impact
Before the new standalone source DDL, record live source UUID/database/version, absent-or-exact three table state, dependency baseline, old DCC and ledger hashes, capacity and owned stable writer window. Keep the approved prior upgrade backup/receipts. New schema-only backup impact adds three table definitions if present and the exact new ledger row; later configuration backup must separately include actual scope/evidence/reservation rows plus audit records. No attachments or historical records are rewritten by this DDL.

The user's original 19-script approval does not silently authorize this new migration. Root should present the final frozen one-script/three-empty-table/max-one-ledger impact after isolated proof and review; subsequent exact legacy registration is another clearly described data action. This is an exact implementation scope boundary, not a new general compliance checklist.
