# Verification Report

## Scope
- User rule: related-file publication follow-up notification is emitted only for major-version changes; authorized users decide whether to associate the new version; minor-version changes do not notify and do not auto-switch existing relations.
- Code scope: DCC publication follow-up creation, candidate notification materialization trigger path, related impact-assessment trigger path, and regression tests.

## Version Interpretation
- Full version format is slash-separated and policy-driven.
- Default policy: `majorIdentitySegmentCount=1`; `A/1 -> A/2` is minor, `A/2 -> B/1` is major.
- Configured two-segment policy: `majorIdentitySegmentCount=2`; `A/1/1 -> A/1/2` is minor, while `A/1` and `B/1` are different major-version identities.
- Minor changes create no follow-up batch, no impact task, and no notification.
- Major changes create the existing follow-up batch, impact assessment, and notification candidate chain so authorized recipients can decide whether to associate the new version.

## Evidence
- PASS: `node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-028-major-version-notification-gate-contract.spec.cjs`.
- PASS: `node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-007-related-file-inheritance-contract.spec.cjs`.
- PASS: `node IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-static-013-impact-revision-chain-resolution-contract.spec.cjs`.
- PASS: `git diff --check`.
- PASS: `mvn.cmd -pl yudao-module-dcc -Dtest=DccControlledFileVersionPolicyTest,DccPublicationFollowupServiceTest,DccNewFileInitialVersionTest -Dsurefire.failIfNoSpecifiedTests=false test`; 19 tests, 0 failures, 0 errors, 0 skipped.
- PASS: `mvn.cmd -pl yudao-module-dcc -am -Dtest=DccControlledFileVersionPolicyTest,DccPublicationFollowupServiceTest,DccPublicationFollowupTransactionIntegrationTest,DccPublicationNotificationServiceTest,DccRelatedFileImpactAssessmentServiceTest,DccControlledFileFinalizationServiceImplTest,DccNewFileInitialVersionTest -Dsurefire.failIfNoSpecifiedTests=false test`; 96 tests, 0 failures, 0 errors, 0 skipped.
- PASS: `mvn.cmd -pl yudao-module-dcc -am -Dtest=DccPublicationFollowupServiceTest,DccPublicationFollowupTransactionIntegrationTest,DccPublicationNotificationServiceTest,DccRelatedFileImpactAssessmentServiceTest,DccControlledFileFinalizationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`; 87 tests, 0 failures, 0 errors, 0 skipped.
- PASS: `mvn.cmd -pl yudao-module-dcc -am -DskipTests install`; compile and local snapshot install succeeded for DCC dependency closure.

## Full Regression Boundary
- BLOCKED: `mvn.cmd -pl yudao-module-dcc -am test` fails before running DCC because `yudao-module-system` expects `C:\ProjectPackage\erp-invoice-voucher-print-assistant\server.js`, which is absent on this machine.
- BLOCKED: `mvn.cmd -pl yudao-module-dcc test` reaches DCC after local snapshot install, but the module already contains unrelated failing suites: schema destructive-operation assertions, registration-certificate contract drift, platform adapter static assertion, missing tenant context in one publication-flow fixture, and missing assignment-scope mock in OnlyOffice preview tests.
- Current change is still covered by directed Java tests plus static contracts that exercise and lock the modified publication follow-up path.

## Result
- The previously stated issue is fixed in code for future behavior: non-major changes return before any follow-up batch, visibility snapshot, relation snapshot, impact task, notification candidate, or notification delivery materialization can be created.
- The major/minor boundary is configurable through `DccControlledFileVersionPolicyProperties.majorIdentitySegmentCount`, and all modified follow-up, impact-assessment, check-in, and submission-version paths use the same policy.
- Existing manual decision flow remains intact for major changes: the major-change branch still creates the follow-up batch, relation snapshots, impact tasks, and notification candidates for authorized handling.
- Per user scope, no historical-data cleanup or migration was attempted.

## Closeout Boundary
- `task_closeout.py` / task-closeout-cleanup was not found in the repository or `C:\Users\D01020\.codex`, so cleanup preview/apply could not be run from this machine.
