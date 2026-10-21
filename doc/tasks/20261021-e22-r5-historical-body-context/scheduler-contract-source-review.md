# Scheduler Contract Source Review

Date: 2026-10-21

## Scope

Read-only review of source files, preserved task evidence, and the already captured database observations. No database or runtime writes were performed; no job was triggered and no service was restarted. This report does not attribute job 5625 to any particular file.

## Findings

The first due candidate inspected, controlled file `4141` (`2054545668044084141`), has no prepared contract metadata. The bound resolver therefore selects `LEGACY_V4_ACTION`. Its bound process definition is `dcc-controlled-file-revision:4:dcc-three-workflow-tenant-1`, but the exact BPMN bytes currently stored for that deployed definition have canonical digest `9922d53710050318a4827b3b61449727ff87e3c62f3146619be4d19c7cffcd13`, registered as `PUBLIC_MATRIX_NATIVE_V1`. The contract mismatch is consequently deterministic for this candidate if resolution reaches the catalog lookup.

The current V4 deployment resource has raw SHA-256 `832eeb34ac311d615115b4c49889b5910843b1f6a17b47d06f7b778370b8d759`. It is byte-identical to the V5 BPMN resource and to the V5 source imported by the October 14 UI deployment script. In contrast, the exact V4 BPMN literal in the official September 30 V4 seed produces canonical digest `42aff38a44dec91c5cd64cae5a19507263260d4d5fed7ec0f28b3e4632805cc4`, registered as `LEGACY_V4_ACTION`. The migration creates the V4 resource only when absent and does not update existing V4 bytes.

The earliest identified operation consistent with the rewrite is the October 14 real UI import/save/publish of V5, recorded by `doc/tasks/20261013-dcc-project-version-e2e/execution-log.md` and implemented by `deploy-revision-model-ui.cjs`. The V5 import source is the same 832e resource. An October 16 deployment snapshot records a V4 model editor-source value aliasing the V4 deployment resource byte-array identity; the October 16 task record also documents a real UI save overwriting old deployment XML through that shared identity. This establishes a concrete aliasing mechanism and a strong operation bracket for the drift.

The exact first physical UPDATE is not independently proven. The October 13 pre-image artifact contains only the former raw hash `657530a311e79270a33b1067ccbeae7285d2a4f3be11ce72fbf74754f5b88956`, not the old XML bytes or a database write event. No binlog/audit event or immediate post-save/pre-publish resource snapshot was found. The 832e `workflow-definition-before` snapshot is therefore not authoritative evidence of original V4 content; it is V5-content bytes present under the V4 definition identity.

The later V6 drift is separate: the V4 row changed from 832e to the V6-content raw hash `bbcee56f4beeb8440c497ddada581e770e34216c2e9200279624c6cc04ad219a` during V6 deployment and was subsequently restored to 832e. It does not explain the earlier V4/V5 content collision.

The activation job iterates due files and calls `activateDue(file.getId())` without per-file exception isolation. A thrown catalog error exits that execution loop, preventing later due entries in the same run from being processed. The inspected due list places 4141 first, but there is no evidence that job 5625 was running this exact list or that 4141 was its failing object. The historical job 5625 file ID and actual contract/key/digest tuple remain unknown.

## Safe Recovery Conditions

Keep affected legacy files on HOLD. Do not restore bytes to an existing deployed definition, add legacy catalog mappings, or backfill prepared contract metadata: historical process instances may have been evaluated against different BPMN versions, and the old authoritative bytes have not been recovered.

Before any repair, require authoritative original V4 bytes or a trusted migration artifact, a database write audit/binlog or equivalent first-write evidence, and an exact inventory linking affected files to process-definition IDs, deployments, prepared metadata, and execution history. A repair proposal must identify the quality-approved business action and preserve existing process history and audit records. If a new formal workflow is required, publish it as a new immutable definition with isolated editor-source storage and verified digest/contract/key metadata; do not mutate the historical definition in place.

Job-level isolation is a separate change. Any future per-file continuation design must report the failed file ID and cause, preserve an observable failed batch outcome, and document that separately transactional files may already have committed. It requires explicit idempotency and partial-progress tests before rollout.

## Evidence Pointers

- `IntRuoyiBackend/sql/mysql/20260930_dcc_a_workflow_bpmn_v4.sql` — official V4 seed and create-if-absent behavior.
- `IntRuoyiBackend/config/dcc-workflow-v5/revision.bpmn20.xml` — V5 import source.
- `doc/tasks/20261013-dcc-project-version-e2e/deploy-revision-model-ui.cjs` and `execution-log.md` — October 14 UI import/publish evidence.
- `doc/tasks/20261013-dcc-project-version-e2e/definition-header-repair-original.json` — October 13 V4 raw-hash-only pre-image.
- `doc/tasks/20261016-dcc-24-e2e-coordination/workflow-deployment-backup.json`, `workflow-deployment-drift-check.json`, and `task.md` — editor-source alias and later V6 drift/restoration evidence.
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/listener/DccControlledFileActivationJob.java` — fail-fast due-file loop.
- `doc/tasks/20261021-e22-r5-historical-body-context/execution-log.md` — captured read-only database observations and hashes.
