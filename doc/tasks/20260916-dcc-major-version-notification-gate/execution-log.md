# Execution Log

## 2026-09-16 kickoff
- Current branch: `int_qms`, clean and tracking `origin/int_qms` before this task.
- Existing source review found `DccControlledFileFinalizationServiceImpl.activateRevision` calls `recordPublishedRevision` for every governed activation.
- Existing `DccPublicationFollowupServiceImpl` and notification materialization have no revision-code comparison; an existing test intentionally accepts `B/2` as a follow-up event.
- Version model is `revisionCode/iterationNo`: same revision with higher iteration is a minor version; changed revision code at iteration 1 is a major version.

## BDD / TDD
- BDD scenarios are recorded in `task.md`.
- RED/GREEN entries will be appended as tests and implementation progress.

## 2026-09-16 implementation
- RED/GREEN target: replace the stale `B/2 still creates follow-up` expectation with `A/1 -> A/2 does not create follow-up` and add first-publication no-followup coverage.
- Implementation: `DccPublicationFollowupServiceImpl.recordPublishedRevision` now validates publication identity and tenant, then returns before batch creation unless the published revision code differs from the previous active revision code.
- Design choice: major/minor uses server-owned Windchill identity. `revisionCode` (`A`, `B`, `AA`) is the major version; `iterationNo` (`/1`, `/2`) is the minor version. `A/1 -> A/2` is minor; `A/2 -> B/1` is major.
- Fail-closed behavior: if an old row cannot provide a valid Windchill `versionNo` or normalized `revisionCode`, follow-up processing throws instead of creating a notification on guessed data.
- Added static contract: `dcc-static-028-major-version-notification-gate-contract.spec.cjs` locks the major-version gate before `batchMapper.insertOrKeepExisting` and verifies first publication is not treated as a version-change notification.

## 2026-09-16 verification
- PASS: `node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-028-major-version-notification-gate-contract.spec.cjs`.
- PASS: `node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-007-related-file-inheritance-contract.spec.cjs`.
- PASS: `node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-013-impact-revision-chain-resolution-contract.spec.cjs`.
- PASS: `mvn.cmd -pl yudao-module-dcc -am -Dtest=DccPublicationFollowupServiceTest,DccPublicationFollowupTransactionIntegrationTest,DccPublicationNotificationServiceTest,DccRelatedFileImpactAssessmentServiceTest,DccControlledFileFinalizationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` from `IntRuoyiBackend`; 87 tests, 0 failures, 0 errors, 0 skipped.
- PASS: `mvn.cmd -pl yudao-module-dcc -am -DskipTests install` from `IntRuoyiBackend`; installed sibling module snapshots needed for isolated module commands.
- PASS: `git diff --check` after trimming the service file EOF.
- BLOCKED FULL REACTOR: `mvn.cmd -pl yudao-module-dcc -am test` was blocked before DCC by existing `yudao-module-system` fixture path `C:\ProjectPackage\erp-invoice-voucher-print-assistant\server.js` missing.
- BLOCKED DCC FULL MODULE: after local snapshot install, `mvn.cmd -pl yudao-module-dcc test` reached DCC but failed on pre-existing unrelated suites including destructive schema-contract assertions, registration-certificate contract drift, platform-adapter static assertion, missing tenant context in publication-flow fixture, and missing `assignmentScopeService` mock in OnlyOffice preview tests. The targeted major-version notification tests passed in the directed slice.
- Cleanup precheck: `task_closeout.py` / task-closeout-cleanup script was not present under the repository or `C:\Users\D01020\.codex`; cleanup preview/apply could not be executed from this machine.

## 2026-09-16 configurable version policy scope extension
- User scope update: future behavior is enough; no historical data cleanup required.
- User clarified the major/minor rule must be configurable. Current examples: `A/1 -> A/2` is minor and `A/1 -> B/1` is major; another allowed scheme treats `A/1/1 -> A/1/2` as minor while `A/1` and `B/1` are major-version identities.
- Reopening implementation status to extend the previous fixed revision-code comparison into a configurable version policy used by publication follow-up gating.
- RED: added `DccControlledFileVersionPolicyTest`; compilation failed before `DccControlledFileVersionPolicy` and its properties existed.
- Implementation: added `DccControlledFileVersionPolicy` and `DccControlledFileVersionPolicyProperties` with `yudao.dcc.controlled-file.version-policy.major-identity-segment-count`, defaulting to `1`.
- Implementation: publication follow-up, impact-assessment candidate resolution, check-in version generation, workflow submission validation, and new-row `revisionCode` / `iterationNo` projection now use the shared policy.
- Implementation: `DccControlledFileVersion` now accepts slash versions with more than two segments so configured policies such as `A/1/1` sort consistently.
- GREEN: `mvn.cmd -pl yudao-module-dcc -Dtest=DccControlledFileVersionPolicyTest,DccPublicationFollowupServiceTest,DccNewFileInitialVersionTest -Dsurefire.failIfNoSpecifiedTests=false test`; 19 tests, 0 failures, 0 errors, 0 skipped.
- GREEN: `mvn.cmd -pl yudao-module-dcc -am -Dtest=DccControlledFileVersionPolicyTest,DccPublicationFollowupServiceTest,DccPublicationFollowupTransactionIntegrationTest,DccPublicationNotificationServiceTest,DccRelatedFileImpactAssessmentServiceTest,DccControlledFileFinalizationServiceImplTest,DccNewFileInitialVersionTest -Dsurefire.failIfNoSpecifiedTests=false test`; 96 tests, 0 failures, 0 errors, 0 skipped.
- GREEN: `node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-028-major-version-notification-gate-contract.spec.cjs`.
- GREEN: `node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-007-related-file-inheritance-contract.spec.cjs`.
- GREEN: `node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-013-impact-revision-chain-resolution-contract.spec.cjs`.
- GREEN: `git diff --check`.
- Experience consolidation: updated `docs/backend-development.md` and `docs/experience-index.md` with the configurable DCC version policy and publication notification gate.
- Cleanup precheck remains blocked: `task_closeout.py` / task-closeout-cleanup is still absent under the repository and `C:\Users\D01020\.codex`.
