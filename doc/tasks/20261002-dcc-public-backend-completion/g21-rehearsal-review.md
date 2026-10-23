# G21 isolated rehearsal driver preparation

ready_for_closeout for Root Review. **31 offline tests PASS after independent R01/R02 repair**; no database connection, restore, SQL migration or actual driver invocation was performed. No MySQL rehearsal PASS is claimed. Production Java/SQL, Root support modules, other Owner files, service/Maven/Docker/Git were untouched.

## Frozen assets

- g21-rehearsal-driver.py: fixed clone-only orchestration with disk-only prepare/validate and an explicit technical write gate for rehearse. There is no original-database upgrade/delete/recovery mode.
- g21-rehearsal-contract.json: sealed Root scope/proof/backup/material/support identities, exact922schema/210data/16old/17new/19SQL and7 config delta counts totaling33.
- g21-rehearsal-postflight-package.json: exact final detail Owner receipt and all query/environment/validator/contract/import dependency hashes. Unfinished or changed postflight rejects before transport creation.
- g21-rehearsal-prepared-inputs.json: produced by offline unit setup from the actual frozen disk materials and complete25 prerequisite proofs. status prepared_verified_rehearsal_not_database_execution, databaseWritesExecuted=false, writeAuthorizationGranted=false. No Docker/MySQL transport was created by prepare.
- g21-rehearsal-tests.py: offline positive/negative tests; full first/repeat pipeline uses explicit fake transport/snapshots, not a real MySQL result.
- g21-rehearsal-delivery-fingerprints.json: five raw+normalized source/material fingerprints and frozen Root support hashes. All executable prepared scripts remain protected outside the repository at C:/IntRuoyiBackups/20261003-dcc-integration/exec-g21-0610.

## Entry points

Offline filesystem-only preparation:

`python -B g21-rehearsal-driver.py prepare --postflight-package g21-rehearsal-postflight-package.json --result g21-rehearsal-prepared-inputs.json`

Disk-only revalidation:

`python -B g21-rehearsal-driver.py validate --prepared g21-rehearsal-prepared-inputs.json`

Future Root invocation only after actual explicit user write authorization is recorded:

`python -B g21-rehearsal-driver.py rehearse --prepared g21-rehearsal-prepared-inputs.json --private-directory C:/IntRuoyiBackups/20261003-dcc-integration/rehearsal-g21-NEW --authorize-rehearsal-writes`

The flag is a technical gate and receipt field; it never proves user permission. Current real authorization is still pending. Without it the function rejects before any transport/client/connection. Source/foreign DB modes reject; only fixed dcc_intqms_g18_rehearsal may receive restores/migrations. Parent protection-directory must be the exact task backup directory and private run directory must be new, so stdout/stderr/step proofs cannot silently overwrite earlier results.

## Input and operation gates

Prepare/validate import the three read-only Root support modules after exact frozen fingerprints, validate19 whitelist/25 external proofs/44 closure and raw metadata, reverify protected gzip size/SHA/integrity, scan dump routing and exact922schema/210data names, verify first/repeat byte-for-byte against Root compose_script, and require final postflight package. Any scope/proof/SQL/backup/support/postflight drift stops. Preparation writes only the requested local artifact, never a write connection.

Authorized future pipeline:

1. Fresh readonly source identity (database/server UUID/MySQL8.0.40), clone absence and source database charset/collation. CREATE DATABASE targets the one fixed clone with no IF EXISTS. This is the sole administrative source-selected connection write; it contains no source table DDL/DML. Collation is actual source metadata, never guessed unicode_ci. The clone is not reused/dropped if it exists.
2. Restore the unchanged protected922-table schema and210-table data via Root routing-guarded transport. Verify exact922 table identity set. Freeze the complete clone environment before migration. Snapshot original columns and null/value-separated per-primary-row hashes for all16 original tables plus infra_release_migration; all19 task-ledger IDs must be absent.
3. First and repeat run unchanged exact19 material bytes with only the Owner's pure environment SELECT prefix. The same executing connection emits one environment row; it must equal the pre-migration frozen schema/server/defaults exactly. Record source material SHA, prefix query SHA and whole actual SQL input SHA separately. Exact ordered38 begin/complete markers must be present; plain source SELECT status output is retained but is not mistaken for a marker.
4. First/repeat compare every old primary row and original column against the baseline, permit exactly33 config additions (five BPM storage/info tables ×6 each, two future obsolete policies andone remediation template) and19 task-only APPLIED ledger rows. Each new primary-key delta must match the precise selected V4 model/definition/body/info/deployment, tenant/policy/action/state/mode/executor/form slots/template content identity; counts alone cannot pass. Old ledger hashes/times/status/release metadata remain unchanged.
5. Require exact original922+new17 table identities (939 total), all17 new structural tables empty, and invoke the frozen detail Owner postflight schema CLI. First/repeat validate complete26-table/51column/320-byte generation/constraints schema and previous-result fingerprint. Each successful read also has a unique phase/object raw `.facts.txt` and readonly receipt with query/facts SHA; evidence cannot overwrite first-phase files. Repeat additionally requires the entire first original/new-row snapshots and seed payload proofs identical, including all ledger rows.
6. Stop at the first failure, preserve private stdout/stderr/driver/step receipts, and retain the clone. No automatic deletion, ROLLBACK claim, source restore or continuation after error. A failure receipt is FAILED_STOPPED_NO_AUTOMATIC_RECOVERY; original database upgrade is a different future task.

New V4 XML is compared with the exact formal source literal using UTF8/LF canonical hash; protected original XML remains verified by raw per-row hashes. This canonical newline rule is explicit and only applies to the newly inserted body, not a historical rewrite or alternate-content fallback. Future real execution must pass the body/linkage checks; offline tests do not prove MySQL client rendering.

## BDD / TDD and offline evidence

- g21-rehearsal-red.log: two assertions fail because the fixed isolated driver is absent, proving missing authorization/scope behavior before implementation.
- g21-rehearsal-session-environment-red.log: dedicated same-connection environment check absent before its helper was implemented; earlier gate/pipeline cases remain. The strict function now rejects missing, duplicate or foreign session facts and cannot reach postflight/repeat.
- g21-rehearsal-offline-final.log:26/26 PASS. Actual protected metadata/material preparation is disk-only with subprocess.run/Popen mocked/asserted untouched. Pipeline execution/restore/schema callbacks are explicitly fake; source writes are asserted to the fixed CREATE DATABASE string only.
- Strong negatives cover no flag; source/foreign DB; manifest/script/proof/support/backup/postflight hash drift or incomplete contract; changed prepared receipt; missing SQL marker; correct-count wrong XML/tenant/model linkage/policy executor/template; ledger foreign operation; existing clone; restore error; firstSQL error no repeat; old row changed on repeat; mismatched same-session environment; zero-exit/nonJSON/failed/foreign schema validator; nonempty new structural table.

No additional GxP application-policy seed or application readiness gate is added to pure SQL rehearsal. Root's separately prepared audit-operation configuration does not block raw schema/seed replay, and a future ISOLATED_REHEARSAL_PASS would still not mean live application/UI/E2E readiness. Driver does not execute actual tests/business application actions in the database.

## Remaining actual work

Actual user DB write approval and Root Review are required before any clone create/restore/first-repeat execution. Root retains writer-quiescence and approved invocation. The exact prepared first/repeat package and Owner postflight are ready; real MySQL constraints/DDL implicit-commit behavior and protected first/repeat data proofs are still untested. No source upgrade mode is implemented or authorized by this handoff.

The clone rehearses the exact protected backup snapshot. Fresh source server/defaults identity does not prove all25 current source prerequisites or original-row payloads still match that snapshot. Before any later source upgrade, Root must recollect the actual25 facts and refresh backups/materials against the then-current source state. Current protected backups may legitimately age; this clone-only driver does not invent perpetual freshness or treat rehearsal PASS as source-upgrade approval.

Root Review correction: successful read output was previously only returned (not overwritten). The final helper now preserves every phase/object's successful raw output and hash receipt, with a negative nonoverwrite test; first and repeat source facts are distinct. No previous raw DB evidence was deleted.

## Independent R01/R02 Review repair (final31 tests)

The prior26-test receipt remains historical evidence and is not full current coverage. Detail's actual projection negative established that changing new form_id/print_template_setting while retaining33 IDs/old hashes and repeat-stable new-row hashes was previously undetected. The old snapshot support path also returned MysqlFailure.private_error without the driver storing those bytes. Own g21-rehearsal-review-red.log reproduces missing complete-copy contract/recording behavior before repair.

R01: after clone restore and before nineteen SQL, capture six exact native V3 rows from each required source table. All27 info business copy fields and10 procdef copy fields are extracted from the frozen official V4 SELECT, not guessed values. A NULL/value-separated HEX digest over the ordered exact original source columns is frozen in protected baseline-v3-complete-copied-business.json; each first/repeat new V4 row must match its actual source digest. The info digest includes form_id/category/icon/description/simple_model/sort/allow_cancel/withdraw/process_id_rule/auto_approval/print_template and all previously covered fields. Procdef digest includes graph/start-form/description/engine/root/resource facts. Identity/tenant/model/process/derived-from remain separately exact; constants REV1/modelREV1/DERIVED_VERSION0/deployment NULL metadata/info codex creator-updater are separately asserted. Dynamic timestamps are never guessed, and whole new-row snapshot equality still guards repeat. This proves exact backup-clone copy consistency; it does not claim all current source form settings are independently business-valid. No frozen8 fact tool or25 prerequisite receipt needed a change/recollection.

R02: a driver-only RecordingMysql adapter wraps the frozen snapshot_original_rows helper; baseline/first/repeat each table's column/row SELECT now uses protected_read with uniquephase/table/read/sequence stems. Every successful raw result gets a query/facts hash receipt; any MysqlFailure.private_error is saved byte-for-byte to exclusive private stderr plus a failure receipt with its SHA. No error data is printed. Actual Root helper is used in the offline negative to prove exact private-error retention; support modules stay byte-identical. Reusing names cannot overwrite a prior receipt, and first failure still stops without deleting clone/recovering source.

Final g21-rehearsal-review-green.log:31/31 PASS; adds full-copy wrong-form/print/procdef hash, missing/wrongtenant/duplicate V3 source, actual snapshot-helper failure recording and baseline/first separate successfulraw capture. Prepared inputs re-generated in disk-only unit setup; five driver/material fingerprints refreshed. ActualDB authorization and MySQL rehearsal remain unperformed.
