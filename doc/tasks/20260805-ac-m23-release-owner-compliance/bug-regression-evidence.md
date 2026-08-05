# Bug Regression Evidence

## Bug

AC-M23 release owner flow was incomplete: the batch detail release stage exposed quality rejection as the apparent release-stage rejection path, backend terminal release actions did not write operation audit entries, direct approval accepted any nonblank signoff hash, and precheck-passed release owner return was not supported.

## Expected

Release owners must be able to sign and release or return a production order through formal release APIs. Terminal release actions must be auditable, forged or missing signature evidence must be rejected, non-owners must be rejected, and quality rejection must stay separate from release return.

## Reproduction

- Reproduction: `node tests\e2e\edhr-release-owner-return-static.spec.js` failed before implementation because `BatchExecutionDetailPage.vue` did not import/use `rejectEdhrRelease`.
- Reproduction: code inspection and new JUnit tests showed `submit/approve/reject/withdraw` wrote release transaction events without terminal operation audit calls.

## Root Cause

The release stage UI reused the quality-reject action for release-stage rejection semantics. The backend release service recorded transaction events but lacked a shared terminal operation audit path, and approval signature evidence was only checked for nonblank input rather than a current-user approval-center signature record.

## RED

- RED: `node tests\e2e\edhr-release-owner-return-static.spec.js` -> FAIL, missing formal `rejectEdhrRelease` release-return entry.
- RED: backend regression tests were added, but Maven RED/GREEN execution was blocked by shared same-module Maven processes and no backend test PASS is claimed.

## GREEN

- GREEN: `node tests\e2e\edhr-release-owner-return-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\edhr-release-direct-submit-button-static.spec.js` -> PASS.
- GREEN: `git diff --check -- <task-owned AC-M23 files>` -> PASS with LF-to-CRLF warnings only.

## Risk

Backend Maven verification remains the main residual risk. The code path is implemented and test coverage is added, but the task cannot be marked complete until targeted Maven tests pass without concurrent target contention.

## Verification

- PASS: `node tests\e2e\edhr-release-owner-return-static.spec.js` verified the formal release-return frontend contract.
- PASS: `node tests\e2e\edhr-release-direct-submit-button-static.spec.js` verified the direct submit button remains gated after adding the return action.
- PASS: `git diff --check -- <task-owned AC-M23 files>` completed with LF-to-CRLF warnings only.
- PENDING: backend targeted Maven verification must still prove the added release-service regression tests pass.

## Blockers

- Backend targeted Maven verification is blocked until concurrent same-module Maven processes stop writing the shared `yudao-module-mes` target directory.
- The task must not be marked `ready_for_closeout` or `completed` until targeted Maven tests return an explicit PASS.

## Follow-up

After same-module Maven processes finish, run `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrReleaseServiceImplTest,MesProEdhrApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` and update this task to ready-for-closeout only if it passes.
