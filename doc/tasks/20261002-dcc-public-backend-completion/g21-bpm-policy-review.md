# G21 eight BPM/policy/template prerequisite contracts

ready_for_closeout. Eight exact current-target prerequisites are machine verified from Root's fresh readonly capture, with no migration execution or ledger mutation inferred. Only this existing backend task's own SELECT/JSON/Python/test/report artifacts were written. No Java/formal migration/schema/ledger/config/other Owner changes; no database connection, SQL execution, Maven, services, Docker or Git by this Agent.

## Deliverables and operation

- `g21-bpm-policy-facts.sql`: 22 SELECT-only statements. Same-session JSONL begin/end includes database/MySQL version/server UUID/connection/capture time; ten evidence sections each have count seals. No USE/SET/temporary-table/locking/DML/DDL commands. Root supplies one target-bound readonly session and writer-quiescence/freshness receipt.
- `g21-bpm-policy-contract.json`: exact IDs, tenants, versions, suspension states, deployment/model/procdef/info/body linkage, XML/metadata hashes, policy fields/unique match identity, template content/params/name/status, 86 required schema columns with type/nullability/default/collation/generation and16 required indexes with exact uniqueness/order/no-prefix. All eight migration source raw hashes and protected metadata snapshot hashes retained.
- `g21_validate_bpm_policy_facts.py`: parses JSONL, rejects missing/duplicate/count-mismatched/unexpected identities, verifies query/raw evidence and Root readonly capture receipt fingerprints plus frozen database/server UUID8.0.40, and returns each migrationId's actual checks. Non-success saves `--output` rejected JSON and exits1. No network/process/SQL behavior.
- `g21_build_bpm_policy_contract.py`: reproducible offline metadata/contract generator from the protected affected/schema dump and formal source/checklist hashes. It never connects to a database. Do not rebuild/rewrite frozen tools while Root uses them without refreezing fingerprints.
- `test_g21_bpm_policy_facts.py` and explicitly offline protected-metadata fixture: 26 cases PASS. Offline fixtures return `offline_fixture_acceptance_not_fresh_target_proof`, never a fresh target receipt. Runtime proof is separate.

Root runner example after selecting the correct database in its existing readonly CLI session:

`mysql --batch --raw --skip-column-names --default-character-set=utf8mb4 DATABASE < g21-bpm-policy-facts.sql`

Then invoke:

`python -B g21_validate_bpm_policy_facts.py --evidence /absolute/capture.jsonl --capture-receipt /absolute/capture.jsonl.receipt.json --output /absolute/proof.json`

Root receipt contract: status read_only_facts_collected_not_execution, databaseWrites=false, sqlSha256/factsSha256/selectCount22, capture.database/serverUuid/version. Default receipt path is evidence plus `.receipt.json`. Proof also carries contract/evidence/receipt hashes, query filename/SHA, protected snapshot hashes and capture begin/end identity/time. Root merges writer-exclusion/freshness and the other17 prerequisite proofs.

## Eight explicit current-fact decisions

| Migration prerequisite | Required current facts | Historic interpretation |
| --- | --- | --- |
| 20260922_dcc_three_workflow_bpmn_seed | exact18 native V1–V3 definitions and preserved historical V1 bodies; current native V3 complete chain; exact two ACTIVE obsolete published policy rows | V1 remains suspended under current V3; never replays V1 XML/policy UPDATE or claims seed chronology |
| 20260923_dcc_three_workflow_candidate_strategy_fix | exact V2 history and current V3 approval/doc-control strategy34, review35, editor/definition XML link | V2 remains suspended; later V3 satisfies required current routing, not proof V2 seed executed |
| 20260926_dcc_three_workflow_matrix_multi_instance_fix | six exact V3 native process/model/info/deployment/body chains for tenants1/122; parallel department obligations and exact completion expression | actual V3 basis for new V4; no V3 seed/re-suspension replay |
| 20260719_business_approval_policy | policy/request declared schema, exact generated published/pending identity expressions and unique indexes; exact current DCC policy uniqueness | matching ledger retained but not used as substitute for real rowvalues |
| 20260719_dcc_upload_form_policy_seed | current two-tenant published UPLOAD/DRAFT/BPM_REQUIRED/DCC_UPLOAD/NONE/[]; exact active latest legacy definition and same-tenant info/resource body | legacy shared model pointer frozen as history only; no model permission or fake native same-tenant chain |
| 20260720_dcc_publish_form_policy_seed | exact disabled historical BPM_REQUIRED publication rows plus exact current DIRECT PUBLISHED and upload prerequisite | DIRECT is explicit approved P4 successor; never reinterpret it as the July seed's original outcome |
| 20260906_dcc_new_file_lifecycle_p4 | two current READY_TO_PUBLISH DIRECT PUBLISHED/DCC_PUBLISH/NONE/[], historical BPM_REQUIRED disabled | no disabling/update/reinsert of existing policies |
| 20260907_dcc_publication_notification | delivery/audit table columns/indexes and exactly one id6829 publication template content/name/nickname/type/status/params contract | menu/role/package replay excluded; contract proves notification prerequisites required by19 upgrade, not all historic seed DML |

Observed baseline metadata pinned:22 procdefs (18 native +4 legacy),19 models (18 native +one old shared legacy),22 deployments,22 info rows,23 body rows,8 relevant policies andone registered template. Every Native V3 link is same-tenant; the old tenant122 legacy info points to tenant1 shared model by an existing exact modelId. Root explicitly confirmed retaining that history without granting model rights or widening this task into migration/cleanup. The validator requires exact legacy pointer preservation and same-tenant legacy procdef/info while requiring full native tenant/version/model/body chains.

## Effective reject and exact rendering correction

Root's first fresh capture was rejected with Generated identity expression mismatch. MySQL8.0.40 I_S rendered `_utf8mb4\'PUBLISHED\'` and `_utf8mb4\'|\'` rather than the protected DDL's plain literal delimiters. Only that literal rendering grammar is normalized; bytea/columns/order/state/operators/separator content remain exact. A dedicated offline positive equivalent-rendering test first failed (g21-rendering-red.log), then passed after normalization. Negative altered status, separator, AND/OR, missing object_state, missing generated/index and wrong collation still reject. Root's original rejected log is preserved in the main task, never overwritten or recast as success.

The initial source query did not output column collation; the final frozen query does. It also now stores the actual query-file raw SHA (Windows CRLF bytes), not an LF in-memory string hash. Old query capture with a different SHA is rejected and must be recaptured. CLI rejection now writes output JSON while retaining exit1; offline negative validates this behavior.

## Actual evidence

- Root fresh capture `C:/IntRuoyiAll-int_main/doc/tasks/20261001-dcc-integration-unblock/g21-bpm-runtime-facts-v2.jsonl` plus `.receipt.json`: 22 SELECT/264 JSONL rows, database ruoyi-vue-pro, MySQL8.0.40, server UUID92ca05d0-aec8-11f1-a944-02b4e226a5ef, capture29, 2026-10-03T06:02:37.052761Z–06:02:37.198764Z. Root performed readonly collection, not this Agent.
- Both Root `g21-bpm-runtime-proof-v2.json` and own `g21-runtime-eight-proof.json` validate exit0 with eight `satisfied_by_existing_target_facts`, `historicalSqlExecutionProven=false`, `migrationMustNotExecute=true`, `ledgerWrites=false`. No extra supplementary query remains for these eight current preconditions.
- Query raw SHA: 51da829aaf0891e36a1490a44bee28bd7a431d9d7c7fccea0f52e4a373895648.
- Fresh JSONL SHA: 12fc4f398f8b4edf0cc6b447ba3f961ae2a55756015edb884b76c37168ea19fa.
- Offline positive/negative validator tests:26/26 PASS in g21-offline-final.log. Includes missing/duplicate/wrong tenant/old-version/body tamper/deployment link/policy mode/status/executor/form slots/template/unique index/generated-expression/collation/transport/query checksum/offline-to-fresh and failure output checks.

Tools are frozen at g21-delivery-fingerprints.json. These facts are snapshot proof for the upgrade preconditions, not perpetual writer isolation or permission to execute migrations. Root keeps merge of25 proofs, actual MySQL rehearsal/first-repeat constraint checks, approval, original-ledger preservation and runtime actions.
