# EDHR-STATIC-022 Execution Log

## Rule Read Evidence

- Read `AGENTS.md`.
- Read `docs/task-closeout-rules.md`.
- Read relevant `docs/backend-development.md` sections:
  - association one-to-many gate;
  - eDHR cell-link / release dossier lifecycle boundary;
  - production pick-list source boundary;
  - repair verification extension for inventory detail dedup.
- Read `docs/product/production-team-leader-daily-operations.md` search hits covering transfer association and material traceability.
- Read `docs/bugs/20260913-edhr-additional-logic-audit.md` lines 111-122 for EDHR-STATIC-022 evidence and boundaries.
- Read `docs/test-release-preflight.md` for non-E2E/build/test gate awareness; release/deploy steps are out of scope and forbidden by the user.
- Read bug-regression-fix-loop, behavior-driven-development, task-closeout-cleanup, and project-experience-consolidation skills plus the bug evidence and closeout rule references.

## BDD

BDD: Legal distinct transfer details remain valid -> Given one active order has two persisted inventory trace details from different formal transfer details, and the rest of final release prerequisites are complete, When final release inventory consistency is evaluated, Then both details are included and the inventory check does not fail as duplicate.

BDD: True duplicate transfer detail identity is blocked -> Given one active order has two persisted inventory trace rows that point to the same formal transfer detail identity, When final release inventory consistency is evaluated, Then the inventory check fails with a duplicate inventory trace source issue before final release proceeds.

## TDD Evidence

- RED: `mvn -pl yudao-module-mes -Dtest=MesOrderReleaseCompletenessServiceTest#evaluateInventoryConsistencyAllowsDistinctTransferDetails test` -> FAIL, expected reason: old `sourceType`-only duplicate grouping returns `BLOCKER` for a legal second `TRANSFER` detail.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesOrderReleaseCompletenessServiceTest#evaluateInventoryConsistencyAllowsDistinctTransferDetails,MesOrderReleaseCompletenessServiceTest#evaluateInventoryConsistencyBlocksWhenTraceSourceIdentityDuplicate" test` -> PASS, 2 tests, 0 failures, 0 errors.
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesOrderReleaseCompletenessServiceTest test` -> PASS, 16 tests, 0 failures, 0 errors.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesActiveOrderTransferTraceServiceTest,MesActiveOrderTransferTraceSchemaTest" test` -> PASS, 5 tests, 0 failures, 0 errors.

## Implementation Notes

- Replaced inventory duplicate detection in `MesOrderReleaseCompletenessServiceImpl.evaluateInventoryConsistency` from `sourceType` counts to `duplicateInventoryTraceIdentities`.
- The duplicate identity is built from source type, direction, source object type/id, transfer document/line/detail IDs, material stock ID, material item ID, and batch ID.
- Transfer traces with missing formal transfer document/line/detail identity now fail as invalid inventory trace evidence before stock health checks.
- `MesOrderReleaseCompletenessServiceTest` now covers legal distinct `TRANSFER` details and true duplicate transfer-detail identity.

## Verification Evidence

- Static contract: `rg -n "duplicateSourceTypes|groupingBy\\(type -> type|map\\(MesProcessPoolActiveOrderTransferTraceDO::getSourceType\\).*counting" IntRuoyiBackend\\yudao-module-mes\\src\\main\\java\\cn\\iocoder\\yudao\\module\\mes\\service\\pro\\batchrecord\\MesOrderReleaseCompletenessServiceImpl.java` -> PASS, no matches.
- Static contract: `rg -n "duplicateInventoryTraceIdentities|inventoryTraceIdentity|transferDetailId=|evaluateInventoryConsistencyAllowsDistinctTransferDetails|evaluateInventoryConsistencyBlocksWhenTraceSourceIdentityDuplicate" ...` -> PASS, found new identity helper and tests.
- Transfer trace regression: `MesActiveOrderTransferTraceServiceTest` and `MesActiveOrderTransferTraceSchemaTest` PASS, confirming generated trace records still carry formal transfer line/detail identity and schema fields required by the dedup contract.
- Evidence validator: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260914-edhr-static-022-inventory-detail-dedup\verification-report.md` -> PASS.
- Diff check: `git diff --check -- <task-owned paths>` -> PASS.
- Project experience consolidation check: existing `docs/backend-development.md` already contains the reusable rule "去重按来源身份，不能把同类多条合法明细当重复"; no new long-term experience document was created.
- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260914-edhr-static-022-inventory-detail-dedup --mode preview` -> BLOCKED. Keep list contains task.md, execution-log.md, and verification-report.md; delete list is empty; blocked reason is `Current worktree branch could not be resolved`.
- Task record tracking check: `git check-ignore -v doc/tasks/20260914-edhr-static-022-inventory-detail-dedup/{task.md,execution-log.md,verification-report.md}` -> ignored by `.git/info/exclude` rule `/doc/tasks/*/`; files remain present on disk and were read by cleanup preview.

## Blockers

- Final `completed` status is blocked by explicit user instruction forbidding git commit/push while `docs/task-closeout-rules.md` requires commit and push before completion.
- Cleanup closeout is also blocked because the linked worktree is on detached HEAD and the cleanup script cannot resolve a current branch. Cleanup apply was not run.
- Task documents are ignored by local `.git/info/exclude`; they exist on disk but do not appear in normal `git status`.
