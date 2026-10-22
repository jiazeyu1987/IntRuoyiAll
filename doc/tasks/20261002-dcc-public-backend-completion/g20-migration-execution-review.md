# G20 readonly migration execution preparation Review

2026-10-03. Review only; no MySQL connection, restore, SQL execution, Docker/service/Maven/Git action. Writes limited to this existing backend task's own reports. Root's actual isolated rehearsal script and database-write approval remain pending.

## Result

The frozen nineteen SQL files are internally ordered correctly and match every raw SHA256 in the candidate. Their seventeen new table names and sixteen original ALTER/INSERT target tables exactly match the approved scope. None of these SQL files performs permanent historical business-row UPDATE/DELETE. The actual backed-up schema contains all required old tables and the missing new columns remain additive/nullable. V4 is append-only, future obsolete policy adds two rows and the remediation template adds one; old policy/BPM/catalog replay remains excluded.

The existing general release executor cannot directly consume either the full closure or the candidate and fulfill this scope. There are three actionable execution-preparation findings below. This is not reopening G17's resolved historical source/hash investigation or G19's fixed startup sync switch.

## G20-R01 — full closure selects forbidden initialization and skip path rewrites old ledger

Production source anchors:

- `IntRuoyiBackend/script/release/release_preflight_plan.py:105,120-125`: normalized APPLIED with a differing checksum becomes APPLY (`target checksum differs from manifest; reapply current required SQL`).
- `IntRuoyiBackend/script/deploy/publish-int-ruoyi.ps1:4641`: real target-state query correctly normalizes SKIPPED_ALREADY_APPLIED to APPLIED. The finding therefore uses that normalization, not a mistaken claim that all SKIPPED rows are pending.
- `publish-int-ruoyi.ps1:4940-4941`: SKIP_ALREADY_APPLIED invokes the state writer. `:4715-4717` INSERT/ON DUPLICATE KEY UPDATE changes status, file name, SHA256, release tag, finished time, operation id, updater and update time on the old unique target_environment/migration_id row. The Python executor's `release_deploy_executor.py:53` likewise invokes record_state for skipped items.

Reproduced without execution: import build_preflight_plan from the actual integration source using Python -B; pass g18-migration-package.json.executionOrder (45) and actual g13-runtime-complete-ledger.log test rows with APPLIED/SKIPPED normalized as the real PowerShell query does. `target_environment=test,publish_scope=with-data` returns **status passed; APPLY40, SKIP_ALREADY_APPLIED5**. APPLY includes both old base/catalog with changed semantic files, V1 workflow seed, V2/V3 seeds, old publication policy, and activation registration excluded from this upgrade. Even the five actual matching skipped ledger rows would be rewritten by the normal apply entry point.

Specific dangerous entries:

| Identity | Planner reason | Required G18 behavior |
| --- | --- | --- |
| 20260513_dcc_base_schema | APPLIED old hash differs; APPLY current SQL | preserve historical record and exclude current initialization |
| 20260710_dcc_product_catalog_database | APPLIED old hash differs; APPLY current SQL | no catalog re-import and no old ledger hash replacement |
| 20260922_dcc_three_workflow_bpmn_seed | ledger absent; APPLY | target old workflow/configuration already exists; do not replay |
| 20260926_dcc_three_workflow_matrix_multi_instance_fix | ledger absent; APPLY | V3 already exists; do not rerun its final UPDATE of older procdefs |
| 20261003_dcc_controlled_file_activation_job_registration | pending; APPLY | explicitly excluded; official task-page registration later |

Remedy for this bounded local upgrade: use a task executor whose SQL input is exactly candidate.executionOrder, frozen ID/path/raw hash and no other item. Reject count !=19, duplicate IDs, path traversal, hash mismatch or any extra identity before a connection/write. Do not invoke generic full-release migration selection or skipped-state callbacks. Do not fake old current-file hashes/equivalence, rewrite historical statuses, or insert invented historical APPLIED rows. General release planner/writer behavior requires a separately authorized fix if later used for unrelated releases; this Review does not modify it.

## G20-R02 — nineteen-item preflight still needs real external dependency proof

Candidate order has no forward candidate dependencies. The minimal recursive closure is **44 =19 executable +25 externally satisfied prerequisites**. The published45 package includes the one paused activation registration, which is outside this actual execution set. Distinguish metadata package45 from executable closure44 and raw SQL19; changing this count is not required to execute the current fixed whitelist.

With the same actual normalized ledger and **candidate-only19** passed to the existing planner, result is **blocked; APPLY2, BLOCKED_DEPENDENCY_MISSING17**. Only reason-capacity/P1 clear their recorded base prerequisite. For example A lifecycle requires task-assignee-snapshot; V4 additionally requires V3; B requires project request/template; C requires name claim/P3; D requires related-file/P4/publication notification. These target objects exist, but most historical migration IDs were never recorded. It is incorrect either to replay their SQL or invent APPLIED history just to make a ledger-only planner green.

Target facts observed in protected schema and logs (schema existence is not itself complete semantic migration proof):

| External prerequisite group | Existing evidence | Preparation acceptance |
| --- | --- | --- |
| Base and catalog (2) | G17 exact historical sources/hashes retained; backed-up DCC base/catalog tables exist | preserve those original ledger rows byte-for-byte; target required table/column facts resolve base/catalog dependency |
| Category action + V1/V2/V3 workflow + task snapshot (5) | route action_type/non-null/action-version unique index; two tenants each three exact V3 process/model/info rows; V1/V2 paused, V3 active; all18 old BPM bytes hashes; task snapshot original unique identities | seal exact procdef/model/info linkage, tenant/key/version/state and required task column/index order; existing V4 IDs absent before first apply |
| Catalog relation/request/taxonomy/template/name claim (5, plus catalog above) | dcc_data_relation, request/identity-claim/project-relation, taxonomy, template item and name-claim CREATE definitions in the real schema | full columns/nullability/collation and unique keys match required existing contracts; no guessed backfill or catalog/menu imports |
| Lifecycle P2/P3 (2) | checked-out/source/predecessor/cancel columns and actual checkout table/active-master generated index; revision_base_active_controlled_file_id exists | pin exact definitions and current source identities; P1 separately in executable whitelist removes only the old master-chain index |
| Related file + form/menu/policy + UPLOAD/PUBLISH/P4 (7) | all required tables present; five matching historical ledger entries include menu/FormCenter/policy, taxonomy, notification identity; actual UPLOAD DRAFT PUBLISHED, PUBLISH BPM_REQUIRED DISABLED and DIRECT PUBLISHED in both tenants | verify formal object/action/state/mode/key/executor and counts with exact BINARY identity; never run old seeds |
| Publication followup/impact/notification and notify-message identity (4) | real followup/impact/notification table definitions; matching notify-message migration row and existing template state | pin required columns/indexes and actual existing config rows; remediation is the sole new template |

The groups total25 when counted by actual IDs, not table count. A machine-readable target-specific prerequisite receipt should enumerate all25 IDs and their required verified facts, source schema/log hashes, baseline database/server identity, and freshness/writer precondition. A predicate may mean `satisfied_by_existing_target_facts`; it must not mean `that migration was executed with today's SQL hash`. The task executor checks each dependency as either an earlier successful candidate receipt or a sealed external predicate; any missing/wrong fact stops before that candidate. Review of the provided files establishes object presence and selected formal facts, while the final executable proof/query list still needs Root's prepared rehearsal script.

## G20-R03 — recording nineteen migrations adds a ledger boundary outside the sixteen business tables

The sixteen-table affected backup exactly matches candidate DDL/DML targets. `infra_release_migration` is included in the larger210-data backup, but is not in existingTablesAffected nor the separate16-table recovery payload. Calling the standard state writer adds/modifies a seventeenth existing table, outside the raw SQL sixteen-table rollback statement. Preserving old ledger rows and adding new nineteen actual results can coexist, but that distinction needs an explicit receipt/rollback boundary.

Proposed exact behavior:

1. Snapshot full old ledger keys/payload hashes, and confirm all nineteen `(target_environment=test,migration_id)` keys are absent or are exact prior task-owned results. Preserve unrelated/deleted/old rows as well; no broad APPLIED normalization DML.
2. Append task-owned rows only for the nineteen allowed IDs, with frozen original SQL SHA256/path, actual start/end result and fixed task operation ID. RUNNING/FAILED/APPLIED updates may affect only a row newly inserted by this operation with unchanged identity/hash. Unique-key collision rereads exact prior facts; mismatch rejects. Never ON DUPLICATE UPDATE old release/hash fields or call state writer for skipped external prerequisites.
3. On second rehearsal raw-SQL idempotency run, retain the original successful ledger rows unchanged and capture separate task execution receipts; normal repeat apply may skip exact APPLIED task rows without any ledger mutation.
4. Recovery removes only explicitly recorded newly created ledger keys/IDs after verifying task operation/hash and absence of foreign modifications, or keeps them as failed recovery facts according to the approved recovery policy. It must not restore the entire ledger from the broad backup or edit its old rows. Include these exact ledger writes/deletion policy in Root's pending database approval. If any candidate was already recorded before this batch, it is outside the nineteen-new-row assertion and requires explicit Review.

A RUNNING/APPLIED ledger row alone cannot undo MySQL implicit DDL commits. Maintain per-statement/step receipts and stop at the first MySQL failure; actual recovery preview must identify committed changes before selective recovery. This does not expand business DML beyond the approved raw19.

## First/repeat rehearsal acceptance matrix

Backups were reviewed locally without database access: all three protected gzip raw hashes and byte counts match g18-backup-receipt.json; gzip schema/affected decode succeeds. Full schema has **922 actual tables**, while the selected data backup is **210 tables**. Separate affected schema/data contains exactly16 named original tables; all17 proposed new tables are absent. A restored922-table schema with210-table data is deliberate isolated scope, not a full live-database recovery. Restore into the fixed new-only rehearsal database without interpreting embedded database switches to the original database. The receipt means gzip/dump integrity; restoration and MySQL first/repeat execution are still unperformed.

| Target | First raw apply from current backup | Second raw apply |
| --- | --- | --- |
| Original9 purely schema business tables | original key/column payload count/hash unchanged; new historical facts NULL | same original payload and final schema |
| Five BPM storage/config tables | each gains exactly6 new V4 identities; every old key/row/body hash unchanged | no row/count/config mutation |
| bpm_business_approval_policy | exactly2 future OBSOLETE policies; original8 DCC policies unchanged | zero delta |
| system_notify_template | exactly1 remediation template; original template IDs/content/params/status unchanged | zero delta |
| Seventeen new structural tables | exact table/column/type/nullability/charset/collation/index definitions, all empty before UI | exact schema and empty contents |
| infra_release_migration if explicitly authorized | exact task-owned new19; every old row unchanged | original successful task rows unchanged; repeat receipts outside old rows |

Expected raw configuration delta is **33 =5×6 V4 rows +2 policy +1 template**. Whole-table equality of the seven additive configuration tables would falsely fail; whole-table count-only checks would miss old-row changes. Freeze all original columns/order and primary-key identities; compare those exact original rows after apply, then validate the permitted new identities separately. Use exact bytes for BLOB/BPM and strings; preserve dates/body/signature/account snapshots and record NULL distinctly. All new owner/reviewer/rework columns stay historical NULL; C's generated keys initially compute NULL because source_original_file_name is newly absent/null, not because historical files were renamed. Generated values/index creation must be checked independently from protected original columns.

MySQL-specific checks to include on the true restored data:

- P1 removes only uk_dcc_controlled_file_master_chain, preserves the pre-existing tenant/project/type/number unique index and lookup, and leaves all36685 Master identities unchanged.
- A lifecycle preserves task snapshot rows while replacing the obligation unique key with the exact tenant/file/BPM/stage/department/deleted order; nullable old BPM facts remain NULL. operator_id becomes nullable without changing existing operator values.
- C creates full binary source-name/number generated keys and exact unique indexes before dropping the old PAD SPACE/template-name constraint. Verify resulting generation expressions and widths after C -> INITIAL -> FAILED-ATTEMPT are final320 bytes; rerunning earlier C/INITIAL must not shrink/revert them. Probe legal failed-attempt key differentiation, unique predecessor and active candidate mutual-exclusion separately on task-owned rehearsal fixtures, then remove/roll back fixtures. Existing originals cannot be guessed/resolved by synthetic claim backfill.
- Access log reason is2000 and original reason payloads unchanged. Existing schema columns/indexes are checked by full definition, not solely the name-based IF NOT EXISTS helpers; a same-named malformed object is a failing negative fixture, not successful compatibility.
- V4 row IDs/linkage/body are exact for tenants1/122 and three process keys; validate old and new model/procdef/deployment/info metadata and six BPMN hashes, not only the source procedure's count or bytearray existence check. Repeat with exact existing V4 is zero-write; a conflicting V4 body rejects. Existing non-body V4 metadata mismatch also needs an external postflight/negative check because SQL's identity-existence guards do not themselves fully validate every field.
- ACTIVE obsolete published policies are exact before future-policy insertion; no old policy updates. New future policies are exactly two and published uniqueness intact. Duplicate/conflicting configuration stops. Existing template contract mismatch stops without overwrite.
- Writer quiescence is rechecked immediately before rehearsal/original operation: previous transaction0 is a snapshot, not a lock. Root owns actual event/connection/service exclusion and task-owned operation lock.

These are preparation checks, not a MySQL PASS. H2/Maven560 and source policy-gate PASS do not validate first/repeated real MySQL DDL/data. Negative fixtures belong solely to the isolated rehearsal database and are not E2E business actions.

## Pinned Review inputs

Main task paths below are readonly inputs; hashes are raw bytes:

- g18-approved-scope-candidate.json: bdd0ec6493fdb3d4ec3d508209439503a5769cf3678f8c2f95710394f0afe74b
- g18-backup-receipt.json: a8cde19e891ca7a335b141d7dabf2b791996c110944d20d789948303ce603511
- g18-runtime-upgrade-plan.md: 2fdfe3a50f4ab30f54f5a0d70b179fc65a86efb7f9c01f9cd8936e00d79ca874
- g18-runtime-configuration-preflight.log: fc52fd543a5e1d7b26e16f43bd215b3f72e457f88c1d2cc4d593e1ef01545501
- g18-migration-package.json: 8ce3d1b74889b7767d8c3a7a0f89870d963e3f7e4f941268af00f41ececcf912
- g13-runtime-complete-ledger.log: 36a0ee8e04b4aa0be3d1c5ce578630fb05c33c02ea4caf0849410981cf528d78

Integration executor raw fingerprints:

- script/release/release_preflight_plan.py: bea4af16614cc31d193328b0ab3022432c7ea68ddc1e7fbe3dc5a043302ed2a8
- script/release/release_deploy_executor.py: 81b03318ae0d24022bf9a2b1e0bc197eb54289a33dcc7fb93c16cd6caf1c32c6
- script/deploy/publish-int-ruoyi.ps1: 2b7a8dfb51a16e2592dfbd9a8b6aaa7647d53d59c9c3b1967b2432e9009bc632

All nineteen candidate source SQL raw hashes match the frozen candidate. No SQL/executor edits or Maven/runtime actions in this Review. Root should resolve R01-R03 in its still-unexecuted local rehearsal preparation and return that concrete script/proof for readonly Review before requesting/exercising actual database-write approval.
