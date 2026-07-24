# Execution Log

BDD: implementation gate -> Given DCC screenshot 8 gaps have approved development documents / When code agents implement each slice / Then every feature point must have RED, GREEN, REGRESSION, and real-path E2E evidence before reviewer release.

INFO: worktree created -> backend and frontend created on branch `task/20260526-dcc-gap8-implementation`.

INFO: previous documentation task -> `doc/tasks/20260526-dcc-gap8-doc-code` marked completed in implementation branch.

INFO: wave 1 dispatch -> T1 Maxwell implements R01/R02 upload/password; T3 Newton implements R11 paper distribution records. Both must provide E2E evidence before reviewer release.

REVIEW FAIL: T1 -> executor stopped after RED tests and partial implementation. R01/R02 backend/frontend implementation, GREEN, REGRESSION, and E2E evidence are missing.

REVIEW FAIL: T3 -> R11 backend unit and electronic receipt regression evidence exists, but E2E did not pass because frontend was not running; frontend typecheck failed with OOM; dedicated paper record read contract from the approved document is not yet implemented.

INFO: wave 1 revision dispatch -> T1 Darwin completes R01/R02 implementation and E2E; T3 Franklin completes R11 paper records contract and E2E.

BDD: T1-R01 source whitelist -> Given an applicant uploads a DCC source file / When the file is not doc, docx, xls, xlsx, dwg, sldprt, sldasm, or slddrw / Then frontend and backend reject it before creating upload preview or workflow records.

BDD: T1-R01 drawing PDF validation -> Given an applicant uploads a drawing source file / When the paired drawing PDF is missing or is not a real PDF record / Then the DCC submit path rejects it and creates no controlled file or process instance.

BDD: T1-R02 password policy unification -> Given a user enters a new password through register, forgot password, profile password change, user create, or admin reset password / When the password is shorter than 8 or lacks either an English letter or a digit / Then the frontend blocks the submit and the backend VO no longer rejects valid long passwords before `AdminUserPasswordPolicy` evaluates them.

RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileUploadApiTest,DccControlledFileWorkflowServiceImplTest#submitControlledFile_rejectsUnsupportedSourceExtension+DccControlledFileWorkflowServiceImplTest#submitControlledFile_rejectsInvalidDrawingPdfFile" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected because `DccControlledFileUploadPreviewReqVO#setPurpose`, `CONTROLLED_FILE_SOURCE_FILE_TYPE_INVALID`, and `CONTROLLED_FILE_DRAWING_PDF_FILE_INVALID` do not exist yet; unrelated T3 paper-distribution test compile failures are also visible in the shared module while another slice is in progress.

RED: `mvn -pl yudao-module-system -am "-Dtest=AdminPasswordPolicyVoValidationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected because old VO annotations still emit `密码长度为 4-16 位` for login, register, reset, user create, user reset, and profile password update.

RED: `node scripts/dcc-screenshot-t2-frontend.test.mjs` -> FAIL, expected because DCC upload frontend has no editable source whitelist, no upload purpose contract, and still states arbitrary file types are allowed.

RED: `node scripts/system-password-policy-frontend.test.mjs` -> FAIL, expected because `src/utils/systemPasswordPolicy.ts` does not exist and password entry pages still carry old local rules/copy.

BDD: T3 R11 paper issue registration -> Given a DCC controlled file has a PAPER distribution row / When doc-control registers paper issue with real recipient users / Then the system records issuer, issue time, paper recipients, no electronic message task, and later recovery/export/print all expose 文件编号、版本、名称、发放人、接收人、发放日期、回收人、回收日期.

RED: mvn -pl yudao-module-dcc -Dtest=DccPaperDistributionAckServiceTest test -> FAIL, R11 expected failure observed because DccPaperDistributionAckServiceImpl.acknowledgePaperDistribution still accepts only user/file/distribution and cannot persist paper recipient users; command output also contains unrelated T1 R01/R02 test compile errors in DccControlledFileUploadApiTest and DccControlledFileWorkflowServiceImplTest, so later Maven GREEN for T3 requires those parallel changes to be fixed or merged consistently.

RED: python -X utf8 -m pytest script\tests\test_dcc_paper_distribution_r11_e2e.py -q -> FAIL, E2E prerequisite blocker before product assertion because frontend is not reachable at http://127.0.0.1:8089/; R11 E2E test file and flow are present, but real-path execution requires frontend/backend services to be started.

BDD: T1-R01 validation ordering regression -> Given a controlled-file submit request has invalid category, missing directory binding, invalid version, or an already-leaf bound directory / When R01 source-file entity validation is added / Then product code may still fail early, but source-file whitelist and drawing-PDF entity validation must run only after parseVersion, category, binding/directory, master load/create, and version-chain validation, and before controlled-file insert.

RED: reviewer command `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileUploadApiTest,DccControlledFileWorkflowServiceImplTest,DccPaperDistributionAckServiceTest,DccDistributionReceiptServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, reported by reviewer because `DccControlledFileWorkflowServiceImplTest` old cases `categoryMissing`, `bindingMissing`, `invalidVersion`, and `bindingDirectoryAlreadyLeaf` were preempted by early `CONTROLLED_FILE_NOT_EXISTS` from `loadSourceFile`.

INFO: T1-R01 blocker root cause -> `prepareSubmitContext` called R01 source-file entity validation before parseVersion/category/binding/directory/master/version-chain checks; fix split product-code early validation from source-file/PDF late validation and added real `FileDO` mocks for success/R01 paths.

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileUploadApiTest,DccControlledFileWorkflowServiceImplTest,DccPaperDistributionAckServiceTest,DccDistributionReceiptServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 78 tests, 0 failures, 0 errors, 0 skipped, finished 2026-05-27T00:10:36+08:00.

GREEN: `mvn -pl yudao-module-system -am "-Dtest=AdminPasswordPolicyVoValidationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 6 tests, 0 failures, 0 errors, 0 skipped, finished 2026-05-27T00:11:09+08:00.

GREEN: `node scripts/dcc-screenshot-t2-frontend.test.mjs` -> PASS, 5 tests, 0 failures.

GREEN: `node scripts/system-password-policy-frontend.test.mjs` -> PASS, 4 tests, 0 failures.

GREEN: `node scripts/dcc-screenshot-r11-paper-records.test.mjs` -> PASS, 2 tests, 0 failures.

GREEN: `node scripts/dcc-screenshot-t5-frontend.test.mjs` -> PASS, 3 tests, 0 failures.

REVIEW HOLD: T1/T3 -> unit/static contract verification passed, but release is still blocked until real Playwright E2E passes for R01/R02 and R11 in the implementation worktree.

GREEN: `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_screenshot_upload_download_e2e.py script\tests\test_dcc_screenshot_admin_policy_e2e.py::test_e2e_14_weak_password_create_reset_and_profile_change_are_rejected script\tests\test_dcc_paper_distribution_r11_e2e.py -q` -> PARTIAL PASS, R01/R02 passed 2 tests against real frontend/backend.

RED: same E2E command -> FAIL for `test_r11_paper_distribution_registration_recovery_export_and_print`, expected blocker exposed because the paper issue dialog did not open a visible `人员选择` dialog before recipient selection; R11 remains unreleased and was sent back to worker Hegel.

BDD: R11 real paper recipient selection -> Given doc-control opens `纸质发放登记` from a PAPER distribution row / When the user clicks the `纸质接收人` selector / Then the real `人员选择` dialog is visible, a real user can be selected, and the same paper record flow continues through issue, recovery, export, and print without mock or skip.

RED: `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_paper_distribution_r11_e2e.py -q` -> FAIL, reproduced worker R11 blocker because `_select_paper_distribution_recipient` waited for `get_by_role("dialog", name=/人员选择/)`, while the real custom `Dialog` rendered a visible `.el-dialog` titled `人员选择` without that accessible dialog name.

INFO: R11 root cause -> product code already exposed a real `UserSelectV2` paper recipient selector inside `纸质发放登记`; the failing path was the E2E selector being narrower than the project's actual custom dialog DOM. After fixing the dialog selector, the same real E2E exposed a second test-data assertion bug: the fixture inserted `version_no='V1.0'` but the paper-record assertion expected `A`.

GREEN: `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_paper_distribution_r11_e2e.py -q` -> PASS, 1 test, real paper issue selection, acknowledge, recovery, records endpoint, export, and print assertions completed against frontend `8104` and backend `48104`.

GREEN: `node scripts/dcc-screenshot-r11-paper-records.test.mjs` -> PASS, 2 tests, frontend paper record contract and detail export/print reuse still intact.

GREEN: reviewer rerun `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_paper_distribution_r11_e2e.py -q` -> PASS, 1 test, independent reviewer confirmation for R11.

GREEN: reviewer combined E2E `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_screenshot_upload_download_e2e.py script\tests\test_dcc_screenshot_admin_policy_e2e.py::test_e2e_14_weak_password_create_reset_and_profile_change_are_rejected script\tests\test_dcc_paper_distribution_r11_e2e.py -q` -> PASS, 3 tests, R01/R02/R11 real-path acceptance remains green in the same worktree runtime.

BDD: T2-R05 withdrawn delete flow -> Given an applicant has submitted a controlled file and actively withdrawn it / When the applicant chooses `删除流程` from the withdrawn detail or mine entry / Then the DCC business record is soft-deleted from business lists, the withdrawn BPM history remains queryable, and any current ACTIVE version for the same master is unchanged.

BDD: T2-R05 withdrawn resubmit flow -> Given an applicant has submitted a controlled file and actively withdrawn it / When the applicant chooses `重新提交` from the withdrawn detail or mine entry / Then the system creates a new DCC business record and a new BPM process instance from the withdrawn record, while the old withdrawn record and old BPM history remain intact.

RED: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#deleteWithdrawnControlledFile_withdrawnOwner_softDeletesOnlyBusinessRevision+DccControlledFileWorkflowServiceImplTest#deleteWithdrawnControlledFile_activeVersion_throwsAndDoesNotDeleteCurrentVersion+DccControlledFileWorkflowServiceImplTest#resubmitWithdrawnControlledFile_createsNewBpmInstanceAndKeepsOldWithdrawnRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected because `CONTROLLED_FILE_WITHDRAWN_ACTION_NOT_ALLOWED`, `deleteWithdrawnControlledFile`, and `resubmitWithdrawnControlledFile` did not exist.

RED: `node scripts/dcc-screenshot-t2-frontend.test.mjs` -> FAIL, expected because the frontend workflow API and DCC withdrawn detail/mine pages did not expose `删除流程` or `重新提交`.

RED: `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_withdrawn_actions_r05_e2e.py -q` -> FAIL, product runtime reached withdrawn detail with the new frontend entry, then failed on `/admin-api/dcc/controlled-files/{id}/withdrawn-flow` because the running 48104 backend jar did not yet include the new R05 endpoint.

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 56 tests, 0 failures, 0 errors, 0 skipped.

GREEN: `node scripts/dcc-screenshot-t2-frontend.test.mjs` -> PASS, 6 tests, withdrawn delete/resubmit frontend contract included.

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS, rebuilt `yudao-server.jar`; local 48104 backend restarted from `D:\ProjectPackage\Int\IntRuoyi\output\runtime\20260526-dcc-gap8-implementation\backend-runtime-control-r05-20260527-014315.jar` and `/actuator/health` returned HTTP 200.

GREEN: `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_withdrawn_actions_r05_e2e.py -q` -> PASS, 1 test, real applicant submit -> withdraw -> delete flow and submit -> withdraw -> resubmit flow completed against frontend 8104 and backend 48104.

REGRESSION: `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_screenshot_workflow_actions_e2e.py -q` -> PASS, 1 test, ordinary submit/approve/return/transfer/sign/fourth-node training artifact paths remained green.

REGRESSION: `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_screenshot_upload_download_e2e.py script\tests\test_dcc_screenshot_admin_policy_e2e.py::test_e2e_11_electronic_distribution_recipient_acknowledges_and_records_signature script\tests\test_dcc_paper_distribution_r11_e2e.py -q` -> PASS, 3 tests, upload/download, electronic distribution receipt, paper distribution recovery/export/print stayed green.

REVIEW FAIL: T2-R05 -> `resubmitWithdrawnControlledFile` created a new DCC/BPM flow but left the old withdrawn record still actionable; a user could revisit the withdrawn detail or mine row and generate duplicate resubmissions from the same withdrawn process. Returned to worker Pascal for fail-fast duplicate-action prevention.

BDD: T2-R05 processed withdrawn guard -> Given a withdrawn DCC record has already been resubmitted / When the applicant revisits the old withdrawn detail or calls the resubmit/delete API again / Then the old record must keep BPM history but no longer expose or accept withdrawn post-actions.

GREEN: reviewer rerun `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 58 tests, including already-resubmitted delete/resubmit rejection.

GREEN: reviewer rerun `node scripts/dcc-screenshot-t2-frontend.test.mjs` -> PASS, 6 tests, including withdrawn action visibility tied to `supersededByFileId`.

GREEN: reviewer restart backend `powershell -NoProfile -ExecutionPolicy Bypass -File .\script\deploy\restart-int-ruoyi-local.ps1 -Component backend -WorktreeName 20260526-dcc-gap8-implementation` -> PASS, backend 48104 healthy from `backend-runtime-control-20260527-020942.jar`.

GREEN: reviewer rerun `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_withdrawn_actions_r05_e2e.py -q` -> PASS, 1 test, delete flow, resubmit flow, processed old-record action hiding, and repeat resubmit API rejection verified against real frontend/backend.

INFO: wave 2 dispatch -> T4 Aquinas implements R07 external file review with independent BPM key `dcc-external-file-review`, independent external-review fields, real E2E, and no fallback to ordinary `dcc-controlled-file-approval`.

INFO: T4 BPM fixture exploration -> existing DCC E2E baseline prepares categories/directories/routes/permissions/signatures only; R07 E2E must prepare or require a test-tenant Flowable definition for `dcc-external-file-review` and verify `ACT_RE_PROCDEF.KEY_='dcc-external-file-review' AND TENANT_ID_='122'`.

BDD: T4-R07 independent external review -> Given a test-tenant applicant opens the external review entry / When they submit external source, owner, reason, participants and source file / Then the system must create a DCC record with `processType=EXTERNAL_REVIEW`, start Flowable with `dcc-external-file-review`, persist external metadata, and reject ordinary controlled-file endpoint bypass.

BDD: T4-R07 final review output -> Given the external review flow reaches the final document-control approval node / When the reviewer approves / Then the API must require a nonblank review conclusion and a real output file, persist both, close the external review, and avoid ordinary controlled-file stamped-PDF finalization.

GREEN: reviewer R07 unit command `mvn -pl yudao-module-dcc "-Dtest=DccExternalFileReviewServiceImplTest,DccControlledFileWorkflowServiceImplTest#submitControlledFile_externalReviewProcessType_requiresExternalReviewEndpoint" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 6 tests, covering external metadata, independent BPM key, fail-fast missing BPM definition, ordinary endpoint bypass rejection, final output requirement, and intermediate approval behavior.

GREEN: reviewer R07 frontend contract `node scripts/dcc-screenshot-r07-external-review.test.mjs` -> PASS, 3 tests, covering independent page fields, API endpoints, detail display, and approval routing.

GREEN: reviewer R07 schema contract `python -X utf8 -m pytest script\tests\test_dcc_external_file_review_r07_schema.py -q` -> PASS, 1 test, covering `dcc_external_file_review` schema and independent process key declaration.

RED: reviewer R07 E2E first run `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_external_file_review_r07_e2e.py -q` -> FAIL before business execution because backend `48104` was still starting after restart and `/actuator/health` refused connection.

GREEN: reviewer backend health -> PASS after startup completed; `http://127.0.0.1:48104/actuator/health` returned HTTP 200.

GREEN: reviewer R07 real E2E rerun `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_external_file_review_r07_e2e.py -q` -> PASS, 1 test, real frontend submit, independent Flowable definition `dcc-external-file-review`, four approval nodes, final conclusion/output persistence, closed time, detail display, and ordinary process-key non-use verified.

RED: worker side-effect guard `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#deleteWithdrawnControlledFile_alreadyResubmitted_throwsAndKeepsOldRecord+DccControlledFileWorkflowServiceImplTest#resubmitWithdrawnControlledFile_alreadyResubmitted_throwsAndDoesNotCreateSecondBpm+DccControlledFileWorkflowServiceImplTest#resubmitWithdrawnControlledFile_createsNewBpmInstanceAndKeepsOldWithdrawnRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected because already-resubmitted withdrawn records were still accepted instead of raising `CONTROLLED_FILE_WITHDRAWN_ACTION_NOT_ALLOWED`.

RED: worker frontend contract `node scripts/dcc-screenshot-t2-frontend.test.mjs` -> FAIL, expected because withdrawn action visibility did not yet depend on `supersededByFileId`.

GREEN: worker side-effect guard `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 58 tests, repeated resubmit/delete on processed withdrawn records rejected and successful resubmit records `supersededByFileId` on the old withdrawn business row.

GREEN: worker frontend contract `node scripts/dcc-screenshot-t2-frontend.test.mjs` -> PASS, 6 tests, detail and mine withdrawn actions are hidden when `supersededByFileId` is present.

GREEN: worker backend package/restart `mvn -pl yudao-server -am -DskipTests package` -> PASS, rebuilt `yudao-server.jar`; local 48104 restarted from `backend-runtime-control-r05-reviewfix-20260527-020451.jar` and `/actuator/health` returned HTTP 200.

GREEN: worker R05 E2E `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_withdrawn_actions_r05_e2e.py -q` -> PASS, 1 test, real submit -> withdraw -> delete and submit -> withdraw -> resubmit paths completed; after resubmit the old withdrawn row had `superseded_by_file_id`, old detail hid both actions, and repeat resubmit API returned non-success.

REGRESSION: worker workflow `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_screenshot_workflow_actions_e2e.py -q` -> PASS, 1 test, ordinary submit/approve/return/transfer/sign/fourth-node training artifact workflow remained green.

REGRESSION: worker combined side-path command `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_screenshot_upload_download_e2e.py script\tests\test_dcc_screenshot_admin_policy_e2e.py::test_e2e_11_electronic_distribution_recipient_acknowledges_and_records_signature script\tests\test_dcc_paper_distribution_r11_e2e.py -q` -> FAIL, environment interference observed after the first upload/download test passed: backend 48104 was unavailable during the third test startup and the electronic distribution test timed out during login.

REGRESSION: worker side-path reruns -> PASS individually: `python -X utf8 -m pytest script\tests\test_dcc_screenshot_admin_policy_e2e.py::test_e2e_11_electronic_distribution_recipient_acknowledges_and_records_signature -q` passed 1 test, `python -X utf8 -m pytest script\tests\test_dcc_paper_distribution_r11_e2e.py -q` passed 1 test, and `python -X utf8 -m pytest script\tests\test_dcc_screenshot_upload_download_e2e.py -q` passed 1 test against frontend 8104/backend 48104.

BDD: T5-R09 applicant training-record gate -> Given a submitted controlled file has `needTraining=true` / When matrix approval completes and BPM creates the fourth document-control approval task / Then the DCC business flow must expose an applicant training-record upload step, block fourth-node approval until the requester uploads a real file, and move the status to the fourth document-control node after upload.

BDD: T5-R10 single-file electronic distribution plan -> Given document-control is approving the fourth node for one controlled file / When document-control selects electronic distribution recipients and uploads the stamped PDF / Then final release must reuse that single-file recipient plan, create real distribution recipient receipt rows, and the list/detail distribution status must expose those recipients for signing and acknowledgement.

RED: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#approveTask_matrixApprovalNeedTraining_entersApplicantTrainingRecordUploadBeforeDocControlApproval+DccControlledFileWorkflowServiceImplTest#uploadTrainingRecord_requesterMovesTrainingGateToDocControlApproval+DccControlledFileWorkflowServiceImplTest#approveTask_docControlApprovalPersistsStampedPdfAndSingleFileElectronicRecipients+DccControlledFileFinalizationServiceImplTest#handleProcessInstanceStatusChanged_existingElectronicDistributionPlanDispatchesSelectedRecipientsAndActivates" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected because `DccControlledFileTrainingRecordReqVO` and the applicant training-record upload workflow contract do not exist yet.

RED: `node scripts/dcc-screenshot-r09-r10-training-distribution.test.mjs` -> FAIL, expected because the frontend API/detail page does not yet expose applicant training-record upload or fourth-node electronic recipient selection.

RED: `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_training_distribution_r09_r10_e2e.py -q` -> FAIL, expected product gap reproduced against real frontend/backend: after the third approval, the file moved directly to `PENDING_DOC_CONTROL_APPROVAL` with no applicant training-record upload gate.

BDD: T5-R09 reviewer applicant-training status split -> Given a controlled file needs training and matrix approval completes / When BPM creates the fourth document-control task / Then DCC must set a dedicated pre-fourth applicant status `PENDING_APPLICANT_TRAINING_RECORD`, show upload only to the requester, and keep post-finalization `TRAINING_IN_PROGRESS` semantics unchanged.

BDD: T5-R09 reviewer fourth-node training payload rejection -> Given the applicant training record has already been persisted before the fourth node / When document-control approves the fourth node / Then the approval dialog and payload must not collect or submit `trainingRecordFileId`, and the backend must reject any fourth-node training-record payload while still requiring the persisted file.

BDD: T5-R10 reviewer recipient payload contract -> Given document-control is approving the fourth node / When they choose electronic distribution recipients / Then `electronicDistributionRecipientUserIds` must be included in the real frontend approval action payload and reused by finalization to dispatch existing distribution recipient receipts.

RED: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#approveTask_matrixApprovalNeedTraining_entersApplicantTrainingRecordUploadBeforeDocControlApproval+DccControlledFileWorkflowServiceImplTest#uploadTrainingRecord_requesterMovesTrainingGateToDocControlApproval+DccControlledFileWorkflowServiceImplTest#uploadTrainingRecord_postFinalizationTrainingStatus_throwsAndDoesNotReuseTrainingInProgress+DccControlledFileWorkflowServiceImplTest#approveTask_docControlApprovalRejectsTrainingRecordPayloadEvenWhenPersisted+DccControlledFileWorkflowServiceImplTest#approveTask_docControlApprovalPersistsStampedPdfAndSingleFileElectronicRecipients+DccControlledFileFinalizationServiceImplTest#handleProcessInstanceStatusChanged_existingElectronicDistributionPlanDispatchesSelectedRecipientsAndActivates" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, reviewer RED currently blocked at compile because the partial T5 production code is not releasable (`DccControlledFileWorkflowServiceImpl` missing `LinkedHashMap` import); after that fix the new tests require `PENDING_APPLICANT_TRAINING_RECORD` and fourth-node training payload rejection.

RED: `node scripts/dcc-screenshot-r09-r10-training-distribution.test.mjs` -> FAIL, expected because the frontend still lacks `ControlledFileTrainingRecordReqVO`, applicant-only upload action, dedicated pending applicant-training status, and fourth-node electronic recipient payload wiring.

RED: `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_training_distribution_r09_r10_e2e.py -q` -> FAIL, real runtime still moves the file from matrix approval directly to `PENDING_DOC_CONTROL_APPROVAL`; expected `PENDING_APPLICANT_TRAINING_RECORD` before requester upload.

RED: reviewer reproduction `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_training_distribution_r09_r10_e2e.py -q` -> FAIL at first approval after frontend restart because MySQL rejected `PENDING_APPLICANT_TRAINING_RECORD` with `Data too long for column 'status'`.

INFO: T5-R09 runtime schema root cause -> `docker exec int-ruoyi-mysql mysql -uroot -p123456 --default-character-set=utf8mb4 -N -e "SELECT COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='ruoyi-vue-pro' AND TABLE_NAME='dcc_controlled_file' AND COLUMN_NAME='status';"` returned `varchar(32) NO NULL`; `yudao-module-dcc/src/test/resources/sql/create_tables.sql` already defines `dcc_controlled_file.status` as `VARCHAR(64) NOT NULL`.

GREEN: migration added and applied -> `sql/mysql/20260527_dcc_controlled_file_status_widen.sql` widens `dcc_controlled_file.status` to `varchar(64) NOT NULL`; `docker exec int-ruoyi-mysql sh -c "mysql -uroot -p123456 --default-character-set=utf8mb4 -D 'ruoyi-vue-pro' < /tmp/20260527_dcc_controlled_file_status_widen.sql"` -> PASS; post-check returned `varchar(64) NO NULL 受控文件状态`, with `SELECT MAX(CHAR_LENGTH(status)), COUNT(*) FROM dcc_controlled_file;` returning `28, 3720`.

RED: post-migration R09/R10 E2E `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_training_distribution_r09_r10_e2e.py -q` -> FAIL, fourth-node approval reached the single-file electronic recipient payload but rejected selected `showroomviewer` because the real test-tenant user had no `dept_id`; fixed E2E baseline to bind the real R09/R10 users to a test-tenant DCC E2E department instead of adding a backend fallback.

RED: R09/R10 E2E after department baseline fix `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_training_distribution_r09_r10_e2e.py -q` -> FAIL, recipient re-login was intercepted by the previous browser session; fixed the shared E2E login helper to clear cookies and local/session storage before opening `/login`.

RED: R10 sign recipient dispatch `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_training_distribution_r09_r10_e2e.py -q` -> FAIL, `接收人加签` inserted the new recipient row but left `message_job_id=NULL`, so the signed recipient was not dispatched.

RED: `mvn -pl yudao-module-dcc "-Dtest=DccDistributionReceiptServiceImplTest#createDistributionRecipientSign_success_addsUniqueRecipientsAndReopensAcknowledgedDistribution" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected because `createDistributionRecipientSign` did not create distribution message jobs; Mockito reported zero interactions with `messageJobMapper`.

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccDistributionReceiptServiceImplTest#createDistributionRecipientSign_success_addsUniqueRecipientsAndReopensAcknowledgedDistribution" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test, sign recipients now receive `messageJobId` and dispatch through `DccControlledFileMessageDeliveryService`.

GREEN: backend restart `powershell -NoProfile -ExecutionPolicy Bypass -File .\script\deploy\restart-int-ruoyi-local.ps1 -Component backend -WorktreeName 20260526-dcc-gap8-implementation` -> PASS, backend restarted from `backend-runtime-control-20260527-042157.jar`; health check `Invoke-RestMethod http://127.0.0.1:48104/actuator/health | ConvertTo-Json -Compress` returned `{"status":"UP"}`.

GREEN: R09/R10 real E2E `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_training_distribution_r09_r10_e2e.py -q` -> PASS, 1 test, real submit, three approvals, applicant training-record upload gate, fourth-node stamped PDF and electronic recipient selection, final distribution dispatch, recipient sign, and acknowledgement completed against frontend 8104/backend 48104.

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileFinalizationServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 77 tests, 0 failures, 0 errors, 0 skipped.

GREEN: `node scripts/dcc-screenshot-r09-r10-training-distribution.test.mjs` -> PASS, 3 tests, 0 failures.

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccDistributionReceiptServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests, 0 failures, 0 errors, 0 skipped.

BDD: T5-R09 workflow regression applicant-training gate -> Given a `needTraining=true` controlled file has completed three approvals / When the existing workflow-actions E2E reaches the fourth Flowable task / Then DCC must first show `PENDING_APPLICANT_TRAINING_RECORD`, the applicant must upload the training record from detail, DCC must move to `PENDING_DOC_CONTROL_APPROVAL`, and fourth-node approval must use stamped PDF only without collecting applicant `培训记录`.

RED: `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_screenshot_workflow_actions_e2e.py -q` -> FAIL, 1 failed in 118.69s; stale test expected `PENDING_DOC_CONTROL_APPROVAL` immediately after three approvals, while runtime correctly returned `PENDING_APPLICANT_TRAINING_RECORD` with a running Flowable `DOC_CONTROL_APPROVAL` task.

GREEN: `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_screenshot_workflow_actions_e2e.py -q` -> PASS, 1 passed in 139.26s; workflow E2E now verifies the applicant training-record gate, uploads the real training record from detail, confirms the fourth-node dialog omits `培训记录`, rejects any fourth-node `trainingRecordFileId` payload, and approves with stamped PDF only.

BDD: T6-R12 DCC approval print template configuration -> Given DCC has an active tenant-scoped `.docx` approval print template with required placeholders / When a document-control user saves the template / Then the backend validates the file from infra storage, persists the active template for the current tenant, and exposes the active template metadata to DCC detail.

BDD: T6-R12 rendered approval Word export and print -> Given a controlled file has real DCC detail data and BPM approval print data / When the user clicks `流程导出 Word` or `流程打印` on detail with an active custom template / Then export downloads a real `.docx` rendered from the configured template, print uses backend print payload from the same DCC/BPM data, and the legacy built-in detail print/export remains available when no custom template is configured.

BDD: T6-R12 fail-fast guardrails -> Given an invalid `.docx`, a template missing required placeholders, a user without category access, or a cross-tenant file id / When save or export is requested / Then DCC rejects the request with a clear service error and does not silently fall back to fake Word or another tenant's data.

RED: `mvn -pl yudao-module-dcc "-Dtest=DccApprovalPrintTemplateServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected compile failure because R12 approval print template service/controller/VO/mapper/error contracts did not exist.

RED: `node scripts/dcc-screenshot-r12-approval-print-template.test.mjs` -> FAIL, expected because the frontend lacked the approval print template API, settings page, route, and detail integration.

RED: `python -X utf8 -m pytest script\tests\test_dcc_approval_print_template_r12_schema.py -q` -> FAIL, expected because the tenant-scoped `dcc_approval_print_template` migration and test schema did not exist.

RED: `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_approval_print_template_r12_e2e.py -q` -> FAIL, expected product gap reproduced against real frontend/backend because there was no reachable `.docx` approval print template upload/settings path.

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccApprovalPrintTemplateServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 9 tests, 0 failures, covering valid save/update, invalid docx, missing required placeholder, unsupported placeholder, rendered export, requester export, no-permission denial, cross-tenant missing file rejection, and non-XML zip entry preservation.

GREEN: `python -X utf8 -m pytest script\tests\test_dcc_approval_print_template_r12_schema.py -q` -> PASS, 2 tests, covering migration DDL and H2 test schema contract.

GREEN: `node scripts/dcc-screenshot-r12-approval-print-template.test.mjs` -> PASS, 3 tests, covering frontend API contract, settings page upload/save/error handling, route entry, and detail custom-template print/export integration.

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS, rebuilt `yudao-server.jar`; backend 48104 was restarted from the rebuilt jar and `Get-NetTCPConnection -LocalPort 48104 -State Listen` showed process `39956` listening.

GREEN: `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_approval_print_template_r12_e2e.py -q` -> PASS, 1 test in 33.93s, real frontend configured a valid `.docx`, exported Word from a real DCC detail, verified the downloaded `.docx` contained actual controlled file and approval data, opened the backend print popup, saved an invalid/missing-placeholder template and observed fail-fast validation, and verified no-permission export denial with real tenant data.

REGRESSION: `mvn -pl yudao-module-dcc "-Dtest=DccApprovalPrintTemplateServiceTest,DccControlledFileQueryServiceTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `DccApprovalPrintTemplateServiceTest` passed 9 tests and `DccControlledFileWorkflowServiceImplTest` passed 64 tests, but `DccControlledFileQueryServiceTest` had 8 pre-existing errors because `DccControlledFileQueryServiceImpl.toRespVO` dereferenced `externalReviewMapper` when the test had not injected that mapper. R12 did not modify that service behavior.

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccApprovalPrintTemplateServiceTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 73 tests, 0 failures, 0 errors, confirming the R12 backend template/export behavior and affected DCC workflow tests are green when the unrelated query-service fixture issue is excluded.

BDD: T7 pre-regression DCC query service fixture -> Given R07 external-review metadata is exposed from controlled-file query responses / When `DccControlledFileQueryServiceTest` builds detail or page response VOs without an external-review row / Then the unit test fixture must inject `DccExternalFileReviewMapper` and allow the mapper's default null result to produce a null `externalReview` field without masking missing dependencies.

RED: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 18 tests run with 8 errors, all `NullPointerException` at `DccControlledFileQueryServiceImpl.toRespVO` because `externalReviewMapper` was not injected in `DccControlledFileQueryServiceTest`.

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 18 tests, 0 failures, 0 errors, after adding the missing `DccExternalFileReviewMapper` Mockito mock to the query-service test fixture.

GREEN: R07 related backend regression `mvn -pl yudao-module-dcc "-Dtest=DccExternalFileReviewServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests, 0 failures, 0 errors, confirming the external-review service contract remains green; no production external-review field behavior was changed.

Bug: T7 pre-regression target test failed with 8 NPEs because the query-service unit test did not inject the new external-review mapper dependency.

Expected: `DccControlledFileQueryServiceTest` should exercise controlled-file page/detail response building with all production `@Resource` mapper dependencies injected; absent external-review rows should remain `externalReview=null`.

Reproduction: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` failed before the fixture fix with 8 `externalReviewMapper` NPE errors.

Root Cause: `DccControlledFileQueryServiceImpl` already had `@Resource private DccExternalFileReviewMapper externalReviewMapper;`, but `DccControlledFileQueryServiceTest` had not added the matching Mockito `@Mock`, so `@InjectMocks` left the field null.

Verification: The target query-service test and R07 external-review service regression both passed after adding the missing test fixture mock.

Blockers: None for this worker fix; no production code, frontend code, fallback branch, or silent exception handling was introduced.

BDD: T7 runtime base schema covers R07/R12 DCC DO tables -> Given a fresh runtime database applies `sql/mysql/20260513_dcc_base_schema.sql` / When `DccBaseSchemaTest` scans every DCC DO table and required base columns / Then runtime schema must contain non-destructive idempotent `CREATE TABLE IF NOT EXISTS` blocks for `dcc_external_file_review` and `dcc_approval_print_template`, and fresh runtime menu seed for the approval print template page must avoid id, permission, and path conflicts.

RED: `mvn -pl yudao-module-dcc "-Dtest=DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 4 tests run with 1 failure; `mysqlSchemaShouldCoverEveryDccDoTableAndColumn` reported `Missing idempotent CREATE TABLE for dcc_approval_print_template in runtime schema`, confirming R12 migration/test schema had not been backfilled into the runtime base schema. R07 `dcc_external_file_review` was also present only in migration/test schema and was included in the same runtime-base repair scope.

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 4 tests, 0 failures, 0 errors, after adding runtime-base `CREATE TABLE IF NOT EXISTS` blocks for `dcc_external_file_review` and `dcc_approval_print_template` with all DO fields plus base columns, and adding the idempotent `DCC审批打印模板` menu seed using id 6817 guarded by id, permission, and path.

GREEN: `mvn -pl yudao-module-dcc test` -> PASS, 257 tests, 0 failures, 0 errors, 0 skipped; full DCC module unit regression passed with the runtime base schema gap closed.

Root Cause: R07/R12 feature migrations and H2 test schema were updated for new DCC tables, but the long-lived MySQL runtime base schema stayed behind, so fresh runtime installs could miss current DO tables even though incremental migrations existed.

Blockers: None for this worker fix; no DROP/TRUNCATE/DELETE, fallback branch, unrelated source change, staging, or commit was introduced.

BDD: T7-R11 detail load isolation from R12 print-html -> Given a DCC detail page has R11 paper distribution records and an active R12 approval print template / When the detail page loads and the R12 `approval-print/print-html` endpoint is unavailable or rejects rendering / Then detail main data, distribution statuses, and paper receipt buttons must still load from their own APIs, while `流程打印` requests backend print-html only when clicked and fails fast without silently switching to the built-in template.

RED: `node scripts/dcc-screenshot-r12-approval-print-template.test.mjs` -> FAIL, expected because `loadData()` still contained `getControlledFileApprovalPrintHtml(controlledFileId.value)`, so R12 print-html rendering failures could block detail main data and R11 paper-record UI state.

GREEN: `node scripts/dcc-screenshot-r12-approval-print-template.test.mjs` -> PASS, 4 tests, after removing detail-load print-html prefetch and adding static coverage that `loadData()` only fetches active template metadata while `handlePrintProcess()` requests print-html on demand.

GREEN: `node scripts/dcc-screenshot-r11-paper-records.test.mjs` -> PASS, 2 tests, confirming R11 paper record API contract and detail export/print data source still use dedicated paper-distribution records.

GREEN: `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_paper_distribution_r11_e2e.py -q` -> PASS, 1 test in 18.10s, real paper issue selection, acknowledgement, recovery, records endpoint, export, and print assertions completed against frontend 8104/backend 48104.

Root Cause: R12 detail integration fetched rendered approval print HTML during `loadData()`. That made a side-path print rendering failure part of the main detail load path, so R11 paper receipt state could remain disabled even though its own detail and paper-record APIs were not the failing dependency.

Blockers: None for this worker fix; no backend behavior change, fallback branch, staging, or commit was introduced.

BDD: T7-R12 approval print popup async validation -> Given a DCC controlled file has an active custom approval print template and real approval data / When the user clicks `流程打印` from the detail page after R12 moved `approval-print/print-html` to click-time loading / Then the E2E must wait for the popup body to contain the submitted file number, file name, and `审批记录`, and fail explicitly if the real backend response or popup DOM does not contain that DCC data.

RED: `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_approval_print_template_r12_e2e.py -q` -> FAIL, 1 failed in 24.63s; `_print_template_from_detail` received the popup but immediately read an empty `body`, so it failed with `custom print popup is missing DCC data:` before click-time print HTML could be rendered or diagnosed.

RED: `mvn -pl yudao-module-dcc "-Dtest=DccApprovalPrintTemplateServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 10 tests run with 1 error after adding the print HTML regression case; `getApprovalPrintHtml_activeTemplate_returnsEscapedHtmlWithControlledFileAndApprovalData` reproduced `java.util.UnknownFormatConversionException: Conversion = ';'` from `buildPrintHtml`, because Java `.formatted(...)` interpreted the CSS `width: 100%;` percent sign as a formatter token.

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccApprovalPrintTemplateServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 10 tests, 0 failures, 0 errors; print HTML now returns escaped real DCC file and approval data while preserving rendered CSS `width: 100%;`.

GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component backend -WorktreeName 20260526-dcc-gap8-implementation` -> PASS, backend 48104 was rebuilt and restarted from `D:\ProjectPackage\Int\IntRuoyi\output\runtime\20260526-dcc-gap8-implementation\backend-runtime-control-20260527-062056.jar`; `http://127.0.0.1:48104/actuator/health` returned `UP`.

GREEN: `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_approval_print_template_r12_e2e.py -q` -> PASS, 1 test in 59.72s; R12 E2E now waits for the real `print-html` response and popup body to contain the submitted file number, file name, and `审批记录`, while still verifying Word export and no-permission denial with real tenant data.

GREEN: `$env:DCC_E2E_FRONTEND_URL='http://127.0.0.1:8104'; $env:DCC_E2E_BACKEND_URL='http://127.0.0.1:48104'; $env:DCC_E2E_HEADLESS='1'; python -X utf8 -m pytest script\tests\test_dcc_paper_distribution_r11_e2e.py -q` -> PASS, 1 test in 17.20s; R11 paper issue, acknowledgement, recovery, records, export, and print regression stayed green after the R12 print popup repair.

Root Cause: The final E2E first exposed the popup async race because the test read `body` immediately after `window.open`. After adding response diagnostics, the same path also revealed that backend `buildPrintHtml()` used `.formatted(...)` with a literal CSS `%`, causing the click-time `print-html` endpoint to return `code=500` and the frontend to close the popup instead of writing content.

Blockers: None for this worker fix; no mock data, skip, fallback branch, staging, or commit was introduced.

## T7 Final Reviewer Gate

REGRESSION: `mvn -pl yudao-module-dcc test` -> PASS, 258 tests, 0 failures, 0 errors.

REGRESSION: `mvn -pl yudao-module-system test` -> PASS, 488 tests, 0 failures, 0 errors, 9 skipped.

REGRESSION: `mvn -pl yudao-server -am -DskipTests package` -> PASS, reactor build success.

REGRESSION: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS.

REGRESSION: frontend static contract suite -> PASS, 29 tests across password policy, DCC upload/withdraw, workflow actions, R07, R09/R10, R11, and R12 scripts.

REGRESSION: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm build:test` -> PASS.

GREEN: service precheck -> backend `http://127.0.0.1:48104/actuator/health` returned `UP`, frontend `http://127.0.0.1:8104/` returned HTTP 200.

GREEN: final isolated real Playwright E2E loop -> PASS, 16 tests covering R01/R02/R05/R07/R09/R10/R11/R12 plus existing DCC workflow-action regression:

- `test_dcc_screenshot_e2e_suite.py` -> PASS, 1 test.
- `test_dcc_screenshot_upload_download_e2e.py` -> PASS, 1 test.
- `test_dcc_screenshot_admin_policy_e2e.py` -> PASS, 7 tests.
- `test_dcc_withdrawn_actions_r05_e2e.py` -> PASS, 1 test.
- `test_dcc_external_file_review_r07_e2e.py` -> PASS, 1 test.
- `test_dcc_training_distribution_r09_r10_e2e.py` -> PASS, 1 test.
- `test_dcc_paper_distribution_r11_e2e.py` -> PASS, 1 test.
- `test_dcc_approval_print_template_r12_e2e.py` -> PASS, 1 test.
- `test_dcc_screenshot_workflow_actions_e2e.py` -> PASS, 1 test.

T7 is completed. Final release gate passed after independent reviewer verification.
