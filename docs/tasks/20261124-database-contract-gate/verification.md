# Application database contract gate

Interface agreed: IntRuoyiBackend/sql/mysql/database-contracts/catalog.json uses ApplicationDatabaseContract/v1 and nonempty unique {id,file} entries. One readonly SELECT/WITH per SQL, exact single-cell single-row DATABASE_CONTRACT_PASS:<id>; all other output/errors fail. Maintenance owns lexical validation, digest-bound packaging and unconditional post-migration/pre-open execution. No dependency on APPLY/SKIPPED and no migration-ledger changes.

BDD/TDD: old or missing CHECK, disabled CHECK, incorrect column, narrowed/widened allowed set must fail current contract. Upgrade old database with immutable forward migration then contract passes, repeat remains unchanged and illegal write rejected. Actual Java-generated FIRST/PATROL/FINAL tasks must be accepted by real MySQL and current contract. Frozen historical AM/PM remain unchanged. Container tests are migration tests, never business E2E.

Original business acceptance remains blocked on approved route/project/process/QA/start authorization baseline. No servers or business records modified.

## Ownership and scope

`sql/mysql/database-contracts/catalog.json` is the application-owned current-contract registry, separate from `release-sources.json` and the immutable migration ledger. Contract filenames do not match migration scans (`20*.sql`). `scripts/preflight/test_release_source_registration.py` verifies every contract SQL has a catalog entry. Maintenance collects this catalog and SQL as digest-bound package artifacts and executes its generic verifier. No generic parser/verifier is copied into the application.

The first assertion covers `mes_pqc_inspection_task.inspection_rule_key`: varchar(32), NOT NULL, valid historical values, the enforced five-value named CHECK, and absence of other directly referencing CHECK constraints. Unknown equivalent CHECK representations conservatively fail. Binary comparison preserves literal contents: spaces, parentheses and charset-like text inside values cannot be normalized into a false PASS. MySQL quote escaping is normalized without changing literal contents. Unrelated quantity CHECKs remain allowed. This does not assert all table fields, triggers, foreign keys or indirect generated-column dependencies.

## Actual verification (2026-11-24)

- Real disposable MySQL **8.0.39**, local image, network disabled, tmpfs, no host ports or application database: **14 tests PASS** (36.435 s). Historical cross-tenant/deleted/submitted fixture snapshots remain unchanged. Upgrade/repeat, disabled/missing/wrong/narrow/wide/malformed/extra CHECKs and illegal-write rejection are covered.
- Actual maintenance `ops.release.release_database_contract.collect/verify` imported using `--maintenance-root`: old CHECK -> ValueError; immutable forward migration -> PASS; additional CHECK rejecting PATROL -> ValueError. Same real-MySQL single-query callback, no replacement implementation.
- Maven six-class regression: **215 tests PASS**, BUILD SUCCESS. Service tests capture actual mapper-bound task keys from active-order creation (FIRST/PATROL), patrol rounds (PATROL), and final inspection (FINAL). Their JSON evidence is then inserted into the real MySQL fixture and accepted by the current contract. This is service/migration verification, not frontend business E2E.
- TDD: missing contract initially failed; additional direct CHECK first reproduced a false PASS while real PATROL insert failed with MySQL 3819, then passed after the contract fix. Literal-distortion negative cases also pass.
- Source-registration tests: **2 PASS**. Maintenance separately reported actual application input collection: 913 migrations, 8 source references, 1234 artifacts including both contract files; 134 maintenance regressions PASS.

Commands (from repository root, Python and Maven on PATH):

```powershell
Push-Location IntRuoyiBackend
mvn -pl yudao-module-mes -am '-Dtest=MesFrontlinePqcPatrolRoundTest,MesTeamLeaderActiveOrderServiceTest,MesOrderQaStageReleaseCoverageTest,MesFrontlinePqcTaskOverlayTest,MesQaInspectionStageRulesTest,MesActiveOrderHandoffLifecycleTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
Pop-Location
python IntRuoyiBackend/script/tests/test_current_pqc_database_contract_mysql.py --writer-evidence-dir IntRuoyiBackend/yudao-module-mes/target/database-contract-writers --maintenance-root <maintenance-checkout>
python -m unittest discover -s scripts/preflight -p test_release_source_registration.py -v
python scripts/preflight/check-menu-migrations.py
```

Run Maven successfully in the current checkout before the MySQL command; missing writer evidence or maintenance module is a hard failure. Local evidence outside Git: `C:/SpugHandoff/release/dbc-java-regression.log`, `dbc-full-mysql.log`, `dbc-extra-check-red.log`, `dbc-extra-check-green.log`.

The initial integration used the maintenance working tree. A final frozen application/maintenance commit-pair rerun remains a separate coordination check after maintenance provides its final commit. Deployment and blocked business E2E are not claimed complete.
