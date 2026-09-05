# Verification Report: Registration Certificate Change E2E

## Scope

Based on `e2e_test\registration\biangeng\registration-certificate-change-e2e-acceptance.md`, verified the registration certificate change path in the synced worktree `D:\IntRuoyiWorktree\20260904-registration-change-e2e-sync` using real Playwright browser operations. After user approval, product code was changed for BPM notification routing, registration-certificate business-event notification behavior, and registration-certificate candidate approver TODO visibility/claim behavior, so approval no longer depends on SMS/mobile validation, missing reminder-job configuration, or Flowable randomly assigning the task to only one selected candidate.

## Environment

- Worktree: `D:\IntRuoyiWorktree\20260904-registration-change-e2e-sync`
- Branch: `codex/20260904-registration-change-e2e-sync`
- Latest source: `origin/int_main` / `549b32bf62f46633a6817d583100f2fd92589bf4`
- Frontend: `http://127.0.0.1:8154`
- Backend: `http://127.0.0.1:48154`, health `UP`
- Change file: `e2e_test\registration\biangeng\biangeng.pdf`
- Tenant: `芋道源码`
- Runtime: task-owned frontend/backend were restarted for the latest E2E retry on ports `8154/48154`.

## Requirement-To-Test Matrix

| Case | Result | Evidence |
| --- | --- | --- |
| E2E-1 申请人提交注册证变更审批 | PASS | Applicant `wanglixuan` submitted certificate `沪械注准20212020492`; natural frontend request `/admin-api/dcc/registration-certificates/990819128/changes` returned HTTP 200, business code `0`, requestId `224`. |
| E2E-2 提交后状态正确 | PASS | Submit created a pending approval request; corrected approver saw the target “注册证变更审批” TODO before approval. After approval, the target task disappeared from TODO as expected. |
| E2E-3 注册经理待办审批 | PASS | Corrected approver `chudongchuan` approved through the real page. Natural `/admin-api/approval-center/tasks/review` returned HTTP 200, business code `0`, result `true`; no `1002013000` or `1080000275` recurred. |
| E2E-4 审批后当前证正式字段更新 | PASS | Applicant reopened certificate detail and saw product name `变更后产品名称-E2E-CHANGE-20260904-1439`. |
| E2E-5 变更履历正确留痕 | PASS | Certificate detail change history showed `已变更`, the after value, and change file `biangeng.pdf`. |
| E2E-6 全结构化字段变更正确回显 | PASS | Full-field submit succeeded for certificate `国械注准20223030034`, requestId `256`; `chudongchuan` found the target approval task by request id, approved through the real page, and detail/history showed every structured after-value plus `biangeng.pdf`. |
| E2E-7 注册部经理直接下载变更批件文件 | PASS | Approver opened the approved change history item and clicked the real download button. Natural file download request returned HTTP 200, and `Content-Disposition` contained the generated business filename with the E2E run key and `.pdf`. |
| E2E-8 普通用户申请下载并在 24 小时内下载 | PASS | Final rerun used a real approved download request `377`; ordinary user downloaded change-approval file `990819182` from the real detail page within the 24-hour grant. |
| E2E-9 普通用户下载授权超过 24 小时后重新申请 | PASS | With user-authorized timestamp simulation, request `377` was expired, the page restored the request-download entry, ordinary user submitted second request `380`, `chudongchuan` approved it through approval center, and ordinary user downloaded again within the new 24-hour grant. |

## Failure Analysis

1. The old `当前账号未配置可用授权公司` error is not present in the latest synced code path. `DccRegistrationCertificateQueryServiceImpl.scopedCompanyIds(...)` now returns an empty list directly, so registration certificate list/detail queries no longer require authorized-company configuration.

2. The original documented approver `chudongqian` could not log in, but the user corrected the approver to `chudongchuan` / redacted password. With that account, login and todo visibility pass.

3. The submit side works on the latest code. The change request was accepted and started the approval flow, returning requestId `224`. That means the original "must configure authorized company before listing/submitting" issue is no longer the failure seen in this run.

4. The SMS/mobile blocker was fixed in BPM notification routing. `BpmMessageServiceImpl` now treats `dcc-registration-certificate-access` as a DCC notify process, so task assigned, approve, reject, and timeout notifications use admin notify inbox and do not call `SmsSendApi.sendSingleSmsToAdmin`.

5. The later `1080000275 未配置注册证提醒任务` blocker came from `DccRegistrationCertificateBusinessEventNotificationConfigService.resolveRecipientScope()`, called by `DccRegistrationCertificateBusinessEventNotifier` after approval recorded the change event. This notification is not the approval itself, so the notifier now treats missing reminder-job config as "skip this optional business-event notification with warn log" and does not fail approval. Real E2E confirmed the approval now completes.

6. Static check after this change: registration-certificate approval/change/notification code has no direct SMS/mobile dependency on the current approval path. Remaining failures are normal hard validations: request/process binding state, approval status, duplicate terminal approval key, revision conflict, malformed change payload, missing required change file, file ownership conflict, pending change conflict, and product-relation requirements for production-related field changes. Reminder-job config is still required by the reminder daily job and business-time simulation, but those are not the approval main path.

7. The E2E-6 assignee blocker was fixed with the user-selected "shared candidate TODO" option. `BpmTaskServiceImpl` now queries candidate-or-assigned tasks, and `BpmNativeApprovalTaskProvider.pageTodo(...)` supplements registration-certificate tasks visible to any selected candidate. When a selected candidate reviews the task, the provider claims the task to that login user before approval; non-candidates are not allowed to claim.

8. Existing request `256` was started before the certificate summary variable fix, so its BPM variables do not contain `certificateNo`. The E2E continuation found it by request id. Future registration-certificate change approvals now include certificate summary variables such as `certificateNo`, classification, product name, and owner company name.

9. Current no-modification E2E-8 failure is no longer the earlier local project-code validation. The real page submitted request `300`, but the approval step selected a visible unrelated approval-center task instead of the target download request; after approval, request `300` stayed `BPM_BOUND/RUNNING`, `/download-grants` returned `canDownload=false` and `pendingRequestId=300`, and no authorized download button appeared.

10. Code analysis for E2E-8 points to a registration-certificate download approval routing gap: `DccRegistrationCertificateApprovalService.startNativeApproval(...)` creates `DOWNLOAD_FILE` approvals with scoped candidates, but `BpmNativeApprovalTaskProvider.claimRegistrationUploadTaskIfPermitted(...)` only claims selected-candidate tasks when variables identify an `UPLOAD_CERTIFICATE` approval. If Flowable assigns a download task to another candidate, the shared-candidate visibility and claim path used for upload/change does not cover it.

11. `DccRegistrationCertificateAccessPolicyService.canDownloadFile(...)` only returns true after a valid active download grant exists. Since request `300` remained running and `DccRegistrationCertificateGrantService.createGrantsForApprovedRequest(...)` was not reached for that request, the frontend correctly kept the file in `申请中`.

12. E2E-9 remains blocked because the scenario needs a grant older than 24 hours. No formal page operation was found to move business time forward, and this run did not use DB/API manipulation to fake expiry.

## No-Modification Rerun

| Case | Current Result | Evidence |
| --- | --- | --- |
| E2E preflight / UI smoke | PASS | Serial rerun of `registration-certificate-change-ui-smoke-real.spec.js` passed on `http://127.0.0.1:8154`; list/detail loaded, required change controls were visible, and no write requests were emitted. |
| E2E-1 through E2E-5 | PASS | Serial rerun of `registration-certificate-change-continue-approval-real.spec.js` passed using existing request `224`; approval completed through the real page and detail/history showed the approved product-name change and `biangeng.pdf`. |
| E2E-6 | PASS | Serial rerun of `registration-certificate-change-remaining-real.spec.js` preserved PASS evidence for full structured-field change request `256`; all structured after-values were visible after approval. |
| E2E-7 | PASS | The same remaining-result evidence preserved PASS for registration-manager direct download; natural file download request returned HTTP 200 with a generated `.pdf` filename containing the E2E run key. |
| E2E-8 | FAIL | Request `300` was created by the ordinary user, but approval did not complete that target request. Read-only evidence showed request `300` still `BPM_BOUND/RUNNING`, no grants, and `canDownload=false`. |
| E2E-9 | BLOCKED | The accepted scenario requires a >24-hour expired grant. No formal page business-time advance path was found, so DB/API expiry simulation was not used. |

The first batch run with three parallel workers failed before business verification because all three Chromium headless launches timed out at 180000 ms. Serial reruns isolated the real business results above.

## 2026-09-05 Final E2E-8/E2E-9 Verification

| Case | Final Result | Evidence |
| --- | --- | --- |
| E2E-8 | PASS | `registration-certificate-change-remaining-real.spec.js` passed. Result file shows `e2e8.status=PASS`, request `377`, real frontend download path `/admin-api/dcc/registration-certificates/files/990819182/download`, HTTP 200, and the target change-history item visible for run key `E2E-CHANGE-REMAINING-1788511901826`. |
| E2E-9 | PASS | Same Playwright run passed. The test used the user-authorized runtime-data timestamp simulation on request `377` only to make the existing grant older than 24 hours; the second request `380`, approval-center review, and final download were all performed through the real frontend. Read-only DB verification shows request `380` is `APPROVED` with active grant window `2026-09-05 16:48:33` to `2026-09-06 16:48:33`. |

Final command:

`pnpm exec playwright test tests/e2e/registration-certificate-change-remaining-real.spec.js --project=chromium --reporter=line --workers=1` -> PASS, 1 test.

Additional code-level failure analysis from the final reruns:

1. Pre-fix request `300` remained invisible to `chudongchuan` because it was created before the access-request candidate change; its binding detail stored only `candidateUserIds: [1]` and Flowable assigned the task to user `1`. The stale pending request blocked a clean resubmission for the same file, so it was cleared by a real frontend approval using the assigned approver account and process-instance search, not by API/DB approval.
2. Newly created download requests after the fix store candidates `[1, 1490]`, so `chudongchuan` can see and approve them. Request `377` and second request `380` both reached `APPROVED` and created grants.
3. The result writer previously left top-level status as `PARTIAL_PASS_WITH_E2E9_BLOCKED` even when `e2e9.status=PASS`; the E2E script now sets top-level `status=PASS` whenever E2E-9 passes.

## Artifacts

- `e2e-artifacts\registration-certificate-change-ui-smoke-result.json`
- `e2e-artifacts\registration-certificate-change-submit-approval-result.json`
- `e2e-artifacts\registration-certificate-change-continue-approval-result.json`
- `bug-regression-evidence.md`
- `backend-api-evidence.md`

## Recommendation

The requested SMS/mobile dependency, missing reminder-job configuration blocker, candidate approver visibility gap, and missing-project-code download request blocker have been removed from the registration-certificate approval/download main path. E2E-1 through E2E-9 now have recorded real-frontend verification evidence; E2E-9's expiry branch depends on the user-authorized timestamp simulation because no formal page time-advance entry exists.

## Fix Verification

- RED: `mvn -pl yudao-module-bpm -Dtest=BpmMessageServiceImplTest test` -> FAIL before the fix.
- GREEN: `mvn -pl yudao-module-bpm -Dtest=BpmMessageServiceImplTest test` -> PASS, 17 tests.
- GREEN: `mvn -pl yudao-module-dcc -Dtest=DccRegistrationCertificateBusinessEventNotifierTest test` -> PASS, 3 tests.
- Combined: `mvn -pl yudao-module-bpm "-Dtest=BpmMessageServiceImplTest" test` -> PASS, 17 tests.
- Combined: `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateBusinessEventNotifierTest,DccRegistrationCertificateBusinessEventNotificationTest" test` -> PASS, 10 tests.
- Package: `mvn -pl yudao-server -am -DskipTests package` -> PASS.
- Candidate TODO GREEN: `mvn -pl yudao-module-bpm "-Dtest=BpmNativeApprovalTaskProviderTest,BpmTaskServiceImplApprovalFilterTest,BpmMessageServiceImplTest" test` -> PASS, 51 tests.
- Candidate summary GREEN: `mvn -pl yudao-module-bpm,yudao-module-dcc "-Dtest=BpmNativeApprovalTaskProviderTest,BpmTaskServiceImplApprovalFilterTest,DccRegistrationCertificateBpmIntegrationTest" test` -> PASS. BPM module ran 34 tests; DCC module ran 14 tests.
- Evidence validators: bug-regression and backend-api evidence both PASS.
- Static formatting: `git diff --check` -> PASS with only Git line-ending warnings.
- E2E retry after fresh jar before the latest fix: old `1002013000 手机号不存在` did not recur; it reached `1080000275 未配置注册证提醒任务`.
- Continue-verification after the latest fix: first run approved successfully but failed on a test-locator strict-mode issue because the expected product name appeared in two visible places.
- GREEN/E2E: `pnpm exec playwright test tests/e2e/registration-certificate-change-continue-approval-real.spec.js --project=chromium --reporter=line` -> PASS, 1 test. Evidence: target approval task no longer appears in TODO, detail page shows updated product name, history shows applied change and `biangeng.pdf`.
- No-modification serial rerun: `pnpm exec playwright test tests/e2e/registration-certificate-change-ui-smoke-real.spec.js --project=chromium --reporter=line --workers=1` -> PASS, 1 test.
- No-modification serial rerun: `pnpm exec playwright test tests/e2e/registration-certificate-change-continue-approval-real.spec.js --project=chromium --reporter=line --workers=1` -> PASS, 1 test; covers E2E-1 through E2E-5 using request `224`.
- No-modification serial rerun: `pnpm exec playwright test tests/e2e/registration-certificate-change-remaining-real.spec.js --project=chromium --reporter=line --workers=1` -> FAIL; E2E-6 and E2E-7 remained PASS, E2E-8 failed because request `300` remained `BPM_BOUND/RUNNING` with no grants after an unrelated approval-center task was approved, and E2E-9 remained BLOCKED by missing formal 24-hour time-advance path.
- E2E-8/E2E-9 final GREEN: `node IntRuoyiFronted\tests\registration-certificate-download-search-static.spec.mjs` -> PASS.
- E2E-8/E2E-9 final GREEN: `node IntRuoyiFronted\tests\registration-certificate-download-request-inline-ux-static.spec.mjs` -> PASS.
- E2E-8/E2E-9 final GREEN: `mvn -pl yudao-module-bpm "-Dtest=BpmNativeApprovalTaskProviderTest" test` -> PASS, 32 tests.
- E2E-8/E2E-9 final Package: `mvn -pl yudao-server -am -DskipTests package` -> PASS.
- E2E-8/E2E-9 final GREEN/E2E: `pnpm exec playwright test tests/e2e/registration-certificate-change-remaining-real.spec.js --project=chromium --reporter=line --workers=1` -> PASS, 1 test; result file top-level `status=PASS`, with `e2e8.status=PASS` and `e2e9.status=PASS`.

## Integration Closeout Preview

- Final branch: `codex/20260904-registration-change-e2e-sync`.
- Final task commit after rebase: one commit ahead of current `int_main`.
- Post-rebase static verification:
  - `node IntRuoyiFronted\tests\registration-certificate-download-search-static.spec.mjs` -> PASS.
  - `node IntRuoyiFronted\tests\registration-certificate-download-request-inline-ux-static.spec.mjs` -> PASS.
  - `git diff --check` -> PASS.
- Post-rebase backend verification:
  - `mvn -pl yudao-module-bpm "-Dtest=BpmNativeApprovalTaskProviderTest,BpmTaskServiceImplApprovalFilterTest,BpmMessageServiceImplTest" test` -> PASS, 53 tests.
  - `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateAccessRequestServiceTest,DccRegistrationCertificateBpmIntegrationTest,DccRegistrationCertificateBusinessEventNotifierTest,DccRegistrationCertificateBusinessEventNotificationTest" test` -> PASS, 38 tests.
- Closeout preview result: BLOCKED. `E:\IntRuoyi` currently has unrelated dirty/untracked files, so the task-closeout guard refused ff-only merge into `int_main`.
- Main-worktree blocker was committed after user authorization:
  - Commit `ca4a1fc33 feat: prepare DCC source governance manifests`.
  - Main blocker verification before commit: `git diff --check` -> PASS; DCC source-governance target tests -> PASS, 11 tests.
- Final pre-merge verification after rebasing onto `ca4a1fc33`:
  - `node IntRuoyiFronted\tests\registration-certificate-download-search-static.spec.mjs` -> PASS.
  - `node IntRuoyiFronted\tests\registration-certificate-download-request-inline-ux-static.spec.mjs` -> PASS.
  - `mvn -pl yudao-module-bpm "-Dtest=BpmNativeApprovalTaskProviderTest,BpmTaskServiceImplApprovalFilterTest,BpmMessageServiceImplTest" test` -> PASS, 53 tests.
  - `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateAccessRequestServiceTest,DccRegistrationCertificateBpmIntegrationTest,DccRegistrationCertificateBusinessEventNotifierTest,DccRegistrationCertificateBusinessEventNotificationTest" test` -> PASS, 38 tests.
  - `git diff --check` -> PASS.
