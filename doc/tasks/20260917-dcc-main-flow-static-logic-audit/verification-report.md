# DCC Main-Flow Static Review Verification

## Scope

User-authorized gpt-5.5 high read-only subthreads reviewed controlled browsing, revision, file lifecycle, and upload through approval/stamped-PDF/default-directory controlled save. Main thread independently confirmed findings before changing code. This report covers static reasoning and automated unit/contracts, not real-page E2E acceptance.

## Findings And Decisions

| Finding | Main-thread decision | Result |
| --- | --- | --- |
| A project Owner checking in another applicant's file inherits the old requester and cannot submit the new WORKING version | Confirmed through copyForCheckin, backend requester gate and frontend requester gate | New version requester and submitter are the check-in actor; original version retained |
| Initial source/PDF pre-upload can store files before validating an enabled project and valid template selection | Confirmed in uploadPreviewFile | Source upload requires explicit context; NEW_UPLOAD validates project access, template and matching category before reading bytes |
| Upload tickets can cross NEW_UPLOAD / CHECKIN / EXTERNAL_REVIEW contexts | Confirmed during second review | Server scopes ticket sessions to validated context and template/version identity; submit/check-in verify the expected identity before ticket resolution |
| External-review drawing PDF deletion/replacement drops the unbound ticket without cleanup | Confirmed during final lifecycle review | Delete/replace first cleans the exact returned session/ticket; cleanup failure prevents the action |
| Upload-stage directory differs from final controlled-save directory | Rejected after reviewing user step 25 | Final save already resolves the configured default directory; reviewer withdrew the finding |

## RED / GREEN

- RED: `DccControlledFileQueryServiceTest#majorCheckinByProjectOwnerUsesOwnerAsNewWorkingRequester` failed with expected requester 99, actual 88.
- RED: `DccControlledFileUploadApiTest#sourceUploadWithoutTemplateContextIsRejectedBeforeReadingFile` failed because the source stream was read without template context.
- RED: `DccControlledFileWorkflowServiceImplTest#newUploadRejectsTicketSessionFromAnotherUploadContext` failed because the external-review session was accepted without throwing.
- GREEN: all three regressions passed in the final Maven run below.
- RED: `dcc-external-review-upload-cleanup-static.spec.cjs` reproduced deletion and valid-replacement cleanup omissions. A later review also reproduced an invalid-replacement branch dropping the old ticket without cleanup. GREEN: all those cases plus cleanup request/error propagation pass, 4 Node behavioral tests total.

## Automated Verification

Existing local Maven 3.9.11 and Java 17.0.20.1 were used through absolute paths under `.runtime/tools`; no tools were installed and no runtime service was changed.

Command from `IntRuoyiBackend`:

```text
mvn.cmd -q -pl yudao-module-dcc -Dtest=DccControlledFileUploadApiTest,DccSourceUploadContextTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileVersionNumberAllocationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

| Suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| DccControlledFileQueryServiceTest | 149 | 0 | 0 | 0 |
| DccControlledFileUploadApiTest | 34 | 0 | 0 | 0 |
| DccControlledFileVersionNumberAllocationTest | 3 | 0 | 0 | 0 |
| DccControlledFileWorkflowServiceImplTest | 135 | 0 | 0 | 0 |
| DccSourceUploadContextTest | 6 | 0 | 0 | 0 |
| Total | 327 | 0 | 0 | 0 |

- PASS: `node --max-old-space-size=8192 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json` from `IntRuoyiFronted`, rerun after final frontend production changes.
- PASS: frontend Node static contracts `dcc-upload-controlled-save-closed-loop-static.spec.js`, `dcc-working-iteration-submit-static.spec.js`, `dcc-upload-project-taxonomy-revision-static.spec.js`, `dcc-upload-category-taxonomy-binding-static.spec.js`, `dcc-static-022-remark-only-checkin-static.spec.cjs`.
- PASS: backend Node static contracts `dcc-static-014-browser-major-revision-owner-action-contract.spec.cjs`, `dcc-static-023-checkin-replay-structured-payload-contract.spec.cjs`, `dcc-static-025-revision-baseline-history-contract.spec.cjs`.
- PASS: `git diff --check`.
- PASS: `dcc-external-review-upload-cleanup-static.spec.cjs`, 4 isolated frontend handler tests using Vue/TypeScript parsers, no browser or network operations.
- Workflow regression initially exposed stale mock update-count/process-instance fixtures and an obsolete unclassified-directory fallback expectation. Those test fixtures were corrected to the existing production contract; production guards were retained.

## Subthread Review

| Flow | Reviewer thread | Latest conclusion |
| --- | --- | --- |
| Controlled browsing | Pascal 01a0acb0-bd14-7773-940b-eecdf06335d8; Goodall 01a0acb9-6ad4-73c0-90c9-3418e59c56d4 | No confirmed static logic issues |
| Revision | Lorentz 01a0acb0-c045-71f3-aadd-9506c7c07eb4 | Final scoped-session and full-flow rereview: no confirmed static logic issues |
| Lifecycle | Pasteur 01a0acb0-c31b-7d81-aad5-2cd1c5b6243b | Requester fix passed; final scoped-session rereview pending |
| Upload to controlled save | Nash 01a0acb0-c5ec-7bd3-a90f-ea9cc106fa52 | Final scoped-session and full-flow rereview: no confirmed static logic issues |

Godel 01a0acb9-6dd9-7272-b5e9-01634937ca7e independently reported the same requester issue, treated as a duplicate finding.

## Closeout Boundary

- No E2E, database writes, service restarts, remote changes, Git commit or push were performed.
- The repository requires commit/push and task-closeout-cleanup before marking its task record completed. Git commit/push were not authorized; the required cleanup skill/script was not found in installed local skills or repository scripts. Formal closeout is blocked, separately from static-review acceptance.
- Task artifacts to retain: `task.md`, `execution-log.md`, `verification-report.md`. No task-local temporary scripts or screenshots were created.
- Nine same-objective preparatory task records dated 2026-09-16/17 were retained and marked as duplicate blocked records referencing this report. They do not represent independent passing verifications.
- Pre-existing worktree changes were preserved. This report does not claim authorship of every dirty file in the shared worktree.
