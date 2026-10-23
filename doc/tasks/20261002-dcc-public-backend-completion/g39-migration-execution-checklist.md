# G39 exact one-migration execution checklist

Prepared by readonly source review only. Root executes all real DB/backup/DDL steps. No tool edits, tests, Maven, DB/client, service, browser or Git actions by this reviewer. User's new actual authorization is recorded in main `g39-user-authorization.json`; actual quality remains unapproved. This procedure creates only three empty sidecars plus at most one ledger row per database; it does not activate legacy names, restore objects, configure audit rules or register quality.

## Fixed reviewed identities

- Existing clone `dcc_intqms_g18_rehearsal`, then source `ruoyi-vue-pro`; UUID `92ca05d0-aec8-11f1-a944-02b4e226a5ef`; MySQL8.0.40/page16384/defaultdynamic; strict session modes include STRICT_TRANS_TABLES and NO_ENGINE_SUBSTITUTION.
- Only `20261003_dcc_legacy_source_name_occupancy`; SQL SHA `621fe041064ac07c5bfe995abd556c3f9436b0253c883a3b7d2b33014e68de2a`. Dependency closure8 is validated as prerequisites, never replayed; old19 migration identities/payloads must remain unchanged.
- Three targets: `dcc_legacy_source_name_scope`, `dcc_legacy_source_name_evidence`, `dcc_source_name_reservation`. Exact70 columns/generated/index/CHECK/time shapes are checked by `g28-schema.validate_facts:94–139`; each target remains zero rows. Partial/existing-without-ledger state blocks, not automatically repaired.
- Sealed tool raw SHA: collector `b2db3483ecc747010a89d87ceea377bd0885f73bc8883cbdd49a71f7d2fde574`; driver `1560ee9ad223b4f65b6393428b84e949422fcc433ce92242285dd762d71c2f56`; schema `c4ef3ac1004df611fa4b81adc936c7f3202de00e65645649e8bc4deb762e3356`. Root support SHA `6416a70ce24ea4fdec6abcbeb1f1a7746bd471533fff5c994b3c6ee08faf1331` is enforced by both tools. Do not run `plan` now: driver plan regenerates task assets and is unnecessary for execution.

## 1. Root's specific flat authorization receipt

`g29-driver-r2.validate_request:137–147` cannot consume the grouped main G39 answer record directly. Root derives a separate protected flat receipt from the actual sidecarMigration answer, with these exact required values:

```json
{
  "status": "SPECIFIC_USER_DDL_AUTHORIZATION_RECEIVED",
  "userAnswerReceived": true,
  "migrationId": "20261003_dcc_legacy_source_name_occupancy",
  "migrationSha256": "621fe041064ac07c5bfe995abd556c3f9436b0253c883a3b7d2b33014e68de2a",
  "allowedDatabases": ["dcc_intqms_g18_rehearsal", "ruoyi-vue-pro"],
  "newLedgerMaximum": 1,
  "preserveHistoricalRows": true,
  "allowLegacyActivation": false,
  "allowObjectRecovery": false,
  "allowAuditPolicyConfiguration": false,
  "sourceQuestionId": "call_mc2WuQaway0pBN7yucRpSVS9",
  "actualAnswer": "授权按方案执行（建议）"
}
```

That narrower receipt's object/audit flags are false even though separate G39 sections authorize conditional work. Preserve the complete main reply separately; do not conflate scopes. Descriptor throughout R2 is **exact actual** `{path,sha256,bytes}` with absolute resolved file path/full raw SHA/integer byte length (`verify_descriptor:28–31`). Paths must remain within protected backup root, main task root or detail task root. Technical CLI flag alone is insufficient authorization.

## 2. Fresh writer attestation and clone collection

Root first establishes writer exclusion through actual ownership/service facts; the collector does not stop services. Fresh attestation JSON minimally contains:

```json
{
  "database": "dcc_intqms_g18_rehearsal",
  "serverUuid": "92ca05d0-aec8-11f1-a944-02b4e226a5ef",
  "allApplicationWritersExcluded": true,
  "capturedEpoch": 0
}
```

`capturedEpoch=0` above is an intentionally non-executable placeholder: Root substitutes the real current numeric Unix epoch after confirming writers. No guessed boolean/time. `_collect_fresh_inputs:89–96` requires age0..900seconds and a **new nonexisting direct child** of `C:/IntRuoyiBackups/20261003-dcc-integration`; clone and source use distinct children. The actual collector reads INNODB_TRX/PROCESSLIST/enabledEVENTS and requires exact integer zero, rechecks before/after dump and freshness at completion. An attestation cannot override a live writer.

Run the real readonly collector via `main:147–153` (absolute source path; Root chooses new output and actual attestation paths):

```text
python -B C:/IntRuoyi/20261001-dcc-integration/doc/tasks/20261002-dcc-detail-integration/g29-collector-r2.py --collect-readonly --database dcc_intqms_g18_rehearsal --output <new-direct-protected-clone-input-directory> --writer-attestation <actual-clone-attestation.json>
```

This verifies target ABSENT/ledger0 (`FIRST_REQUIRED`), actual post19 schema and exact19 ledger, captures full original rows for seven protected tables, creates `schema.sql.gz` and `protected-original-rows.sql.gz` using actual fixed readonly single-row dump commands, then proves original rows unchanged. Seven tables: controlled_file, controlled_file_master, controlled_file_name_claim, controlled_file_source_ownership, controlled_file_signature, **system_electronic_signature**, infra_release_migration, all with `dcc_` prefix where applicable. Never substitute dcc signature for the unified table.

Require actual CLI exit0 and `g29-collection-receipt.json` status `G29_FRESH_READONLY_INPUT_COLLECTION_COMPLETE_NOT_DDL_EXECUTION`. It creates `g28-execution-request.json` with `specificNewMigrationAuthorized=false`, `authorization=null`, `newMigrationClonePass=null`, `privateDirectory=null`. Both fresh backups have endmarkers/counts/fullcolumns/bytes/time/command receipts verified; previous October3/4 readonly collection files are not fresh enough for a new run.

## 3. Clone authorized request and single first/repeat invocation

Preserve the collector's original unapproved request. Write a new Root-reviewed execution request copy retaining all five fresh descriptors:
`freshPreflight`, `freshOriginalBaseline`, `writerExclusion`, `freshBackup`, `prerequisiteProof`.

Set only `specificNewMigrationAuthorized=true`, `authorization=<actual flat authorization descriptor>`, and `privateDirectory=<different new nonexisting direct protected execution child>`. Keep version `G28-EXEC-1`, database clone, actual operationId safe `[A-Za-z0-9_-]{1,100}` (collector provides `dcc-g28-local-new-sidecars`), and `newMigrationClonePass=null`. All fresh proof timestamps must remain0..900seconds at driver validation; stale collection is a stop/recollect condition, not permission expiration.

```text
python -B C:/IntRuoyi/20261001-dcc-integration/doc/tasks/20261002-dcc-detail-integration/g29-driver-r2.py execute --request <authorized-clone-request.json> --authorize-specific-new-migration-writes
```

`execute:240–285` performs **both first and repeat in one invocation**; there is no separate `first`/`repeat` CLI or need to re-run execute. Before each phase it checks current schema/writers/old19; first material adds max1exact ledger, repeat material adds no ledger. It compares original seven-table full snapshots allowing exactly one new infra ledger row, exact new-ledger payload, schema/postflight and first/repeat equality. Require exit0, stdout final `G28_OWNED_EXISTING_CLONE_FIRST_REPEAT_PASS`, protected `driver-receipt.json` two steps ordered first/repeat each `POSTFLIGHT_AND_ORIGINAL_ROWS_PASS`. Re-run pure `validate_completed_journal(read(actualReceipt),CLONE)` before source using the sealed driver, which rereads every material/stream/postflight/snapshot/newledger and pins exact execution SQL material. This is verification, not a second DDL invocation.

## 4. Fresh source collection, clone-proof binding and source execution

Only after actual clone complete proof passes, create a **new source** writer attestation with database `ruoyi-vue-pro` and current real epoch, then execute the same collector command replacing database/output/attestation with fresh source-specific values. Source full snapshot must be source kind `ACTUAL_SOURCE_READ_NOT_CLONE`, not copied clone facts; original unified signatures may differ and must be preserved as actually observed. Required sources remain empty target3/targetledger0.

Create a new authorized source execution request copy using its own five fresh descriptors, the same narrowly scoped authorization descriptor and a new direct execution child. Set `newMigrationClonePass` to the **actual full clone `driver-receipt.json` descriptor**, not a PASS summary, plan, clone schema receipt or forged status. `validate_request:176–177` invokes `validate_completed_journal(clone,CLONE)` before the source adapter initializes.

Run the identical driver execute CLI with the source request. Require actual exit0/final `G28_SOURCE_ONE_MIGRATION_FIRST_REPEAT_PASS_NOT_APPLICATION_READINESS`, exactly first/repeat complete phases, three precise empty tables, exactly max1new ledger, unchanged old7table rows/signatures/old19ledger and repeat equality. Source execution uses the current actual UUID and database in the same-session SQL envelope; no database creation/restore/replay of old19 occurs.

## Failure and completion boundary

Any invalid proof/authorization/identity/writer state stops before transport or DDL as appropriate. Driver marks `databaseWriteAttempted=true` immediately before actual run; a later failure is `STOPPED_DDL_PARTIAL_COMMIT_POSSIBLE_REVIEW_REQUIRED`. Do not retry, delete output, drop partially created tables, rerun phase2, restore automatically or use `--force`. A pre-run guard failure journal is `GUARD_FAILED_NO_DDL_REACHED`. Preserve protected raw streams/partial receipts for Root review, without printing possible data/secret payloads.

Successful migration is **schema prerequisite PASS only**, not application readiness, actual QA approval, 26 audit-rule publication, complete39 source restoration, authenticated legacy registration or frontend acceptance. Those remain independent gates. No new questions or approval were requested by this reviewer.
