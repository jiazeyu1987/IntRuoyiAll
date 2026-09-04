# Execution Log

## 2026-09-04

- Objective: run registration certificate upload E2E acceptance in the worktree and analyze failures without modifying code.
- Acceptance source: `e2e_test/registration/upload/registration-certificate-upload-e2e-acceptance.md`.
- User path correction: original `e2e/_test/...` path was corrected to existing `e2e_test/...`.
- Rules read: `docs/task-closeout-rules.md`, `docs/worktree-restrictions.md`, `docs/login-access.md`, `docs/branch-runtime-ports.md`, `docs/local-runtime.md`, `docs/e2e-rules.md`.
- Skills read: `playwright`, `independent-verification-gate`.
- Constraint: do not record raw passwords, cookies, or tokens.

## Case Results

| Case | Result | Evidence | Analysis |
| --- | --- | --- | --- |
| E2E-1 | PASS | `wanglixuan` 在真实页面打开上传弹框，提交 `E2E-UPLOAD-20260904052851-SELF`，自然上传 POST 返回 HTTP 200 / code 0。截图：`artifacts/self-production-upload-before-submit.png`、`artifacts/self-production-upload-after-submit.png`。 | 上传入口、表单、附件上传和提交审批请求可用。 |
| E2E-2 | FAIL | 当前列表按目标注册证编号查询 total=0；上传人待办不包含目标；上传人“我发起的”按目标注册证编号不包含目标。截图：`artifacts/self-production-current-before-approval.png`。 | 审批中心 MY_INITIATED 的 keyword 仅映射为流程名称查询，而注册证上传审批标题没有包含注册证编号。 |
| E2E-3 | BLOCKED | `chudongqian` 在真实登录页返回 HTTP 200 / code 1002000000，提示账号密码不正确。 | 验收文档指定账号密码不可用，不能切换账号或接口绕过。 |
| E2E-4 | BLOCKED | 依赖 E2E-3 审批通过。 | 未能使用注册经理完成审批，不能验证审批后入库。 |
| E2E-6 | BLOCKED | 自行生产上传已提交，但审批未完成；委托生产组未执行。 | 生产方式入库展示依赖审批后详情。 |
| E2E-7 | BLOCKED | 依赖 E2E-3/E2E-4。 | 注册经理无法登录，无法在详情中直接下载。 |
| E2E-8 | BLOCKED | 依赖 E2E-4；且普通用户 C 没有可确认的可登录账号。 | 验收文档要求普通用户能看详情且无直下载权限，当前上下文不能通过前端确认候选。 |
| E2E-9 | BLOCKED | 依赖 E2E-8，且需要超过 24 小时授权状态。 | 未完成首次授权；不能直接改库/API 推进时间。 |

## Additional Test Asset Finding

- Existing script command: `npx playwright test tests/e2e/registration-certificate-upload-submit-repro.spec.js --project=chromium --reporter=line`.
- Result: FAIL before submission.
- Reason: script expects upload form label `DCC项目代码`, but current page label is `实际项目代码`.
- Evidence: Playwright error context under `IntRuoyiFronted/test-results/tests-e2e-registration-cer-12180-without-SkyWalking-trace-id-chromium/error-context.md`.

## 2026-09-04 Follow-up: Permission and Frontend-only E2E

- User requested account correction: registration manager account is `chudongchuan`; password changed from the old test value to the updated credential, which must not be logged in raw command output.
- User requested setup: add permission `dcc:registration-certificate:upload:approve` to `chudongchuan` before continuing E2E.
- Constraint reaffirmed: E2E business actions must be performed only through real frontend operations; no direct API/fetch/apiGet is allowed for acceptance actions.
- Current worktree: `D:\IntRuoyiWorktree\20260904-dcc-upload-related-files-e2e-worktree`, frontend `8097`, backend `48097`.

## 2026-09-04 Frontend-only permission setup and rerun

- Permission setup was performed through the real frontend only: admin logged into `http://127.0.0.1:8097`, opened 系统管理 / 角色管理 / 权限角色, selected category 注册部, filtered role code `dcc_registration_certificate_approver`, opened 菜单权限, checked `注册证上传审批` (`dcc:registration-certificate:upload:approve`), and clicked 确定.
- Permission save evidence: frontend request `POST /admin-api/system/permission/assign-role-menu` returned HTTP 200 / business code 0.
- Follow-up permission-info check after `chudongchuan` relogin showed role `dcc_registration_certificate_approver` and permission `dcc:registration-certificate:upload:approve` present.
- Frontend-only E2E rerun data: certificate `E2E-UPLOAD-20260904061314`, product `注册证上传E2E产品-20260904061314`, project `IDI`, company `上海七木医疗器械有限公司`, upload file `e2e_test/registration/upload/upload_file.pdf`.
- E2E-1 result: PASS. `wanglixuan` submitted upload from the real upload dialog; natural frontend request `POST /admin-api/dcc/registration-certificates/uploads` returned HTTP 200 / business code 0 and page toast `已提交审批`.
- E2E-3 result: FAIL. After relogin as `chudongchuan`, Approval Center / 待办 filtered by `E2E-UPLOAD-20260904061314` returned HTTP 200 / business code 0 / total 0, page displayed `暂无审批任务`.
- E2E-4/E2E-6/E2E-7/E2E-8/E2E-9 remain BLOCKED because manager approval is not reachable from `chudongchuan`待办 after successful upload submission.

### Failure analysis: manager todo still empty after permission is present

- Frontend evidence proves the missing condition is no longer `chudongchuan` login, role, or permission package: login succeeded and permission-info contains both `dcc_registration_certificate_approver` and `dcc:registration-certificate:upload:approve`.
- Code path for Approval Center TODO uses `BpmNativeApprovalTaskProvider.pageTodo()`, which calls `taskService.getTaskTodoPage(resolveQueryUserId(context), reqVO)`; the TODO page first asks Flowable for tasks visible to the current user.
- The same provider only has a special `claimRegistrationUploadTaskIfPermitted(...)` path during review submission, not during listing. That means a user with the right role/permission may be allowed to claim/review a task if they can submit review context, but the TODO list itself still depends on the BPM task being assigned to that user or otherwise returned by `getTaskTodoPage`.
- Therefore the current failure is most likely in BPM task visibility/assignment after upload submission: the upload request is created successfully, but the runtime task visible to `chudongchuan` is not returned by his personal TODO query.
- This is different from the earlier pure permission issue: permission is now present; the remaining problem is candidate/assignee/listing linkage for the generated BPM task.

## 2026-09-04 Follow-up: Updated backend E2E rerun

- Precondition: earlier `48097` backend was started before the registration approval title search code was built into the executable jar.
- Restart attempt: `powershell -ExecutionPolicy Bypass -File scripts\runtime\start-branch-backend.ps1 -Build`.
- Result: FAIL. Maven compile stopped in BPM because `target/generated-sources/annotations/.../BpmProcessInstanceConvertImpl.java` was truncated at EOF. This was a generated target artifact, not source code.
- Recovery: `mvn.cmd -pl yudao-server -am -DskipTests clean package`.
- Result: PASS. Clean package regenerated the MapStruct file and built `yudao-server-exec.jar`.
- Backend runtime: `48097` health returned UP during E2E from a Java process using the current worktree `yudao-server-exec.jar`.
- Shutdown: the `48097` listener was stopped after verification; no task-owned backend process was left running.
- E2E command: node `doc\tasks\20260904-registration-certificate-upload-e2e-acceptance-worktree\registration-upload-ui-only-e2e.mjs` through the real frontend `http://127.0.0.1:8097`, with uploader `wanglixuan` and manager `chudongchuan`.

## 2026-09-04 Updated Case Results

| Case | Result | Evidence | Analysis |
| --- | --- | --- | --- |
| E2E-1 | PASS | `wanglixuan` submitted `E2E-UPLOAD-20260904070732-SELF`; upload POST returned HTTP 200 / code 0. | Upload form, company/project selection, classification, dates, production mode and file upload path are usable. |
| E2E-2 | FAIL | Current list total before approval was 0; uploader TODO did not contain the target; uploader MY_INITIATED did not contain the target. Screenshot: `artifacts/self-production-current-before-approval.png`. | Flow process title is still overwritten after process creation, so MY_INITIATED keyword search by certificate number cannot match the process instance name. |
| E2E-3 | FAIL | Manager TODO found the target and opened detail; review request returned HTTP 200 / business code `1080000151`. Screenshot: `artifacts/self-production-approval-detail.png`, `artifacts/self-production-approval-after.png`. | `chudongchuan` has role and approval permission, but has no enabled DCC electronic signature image; approval center requires an active signature image before BPM review. |
| E2E-4 | BLOCKED | Depends on E2E-3 approval success. | Certificate cannot enter the current list until manager approval completes. |
| E2E-6 | BLOCKED | Depends on E2E-3/E2E-4 for both self-production and entrusted-production detail checks. | Production mode persistence cannot be verified until approval入库 succeeds. |
| E2E-7 | BLOCKED | Depends on E2E-4 approved certificate detail. | Manager direct download cannot be verified without an approved current certificate. |
| E2E-8 | BLOCKED | Depends on E2E-4 and a confirmed ordinary user C. | Download authorization flow cannot start without an approved certificate and ordinary user candidate evidence. |
| E2E-9 | BLOCKED | Depends on E2E-8 and an over-24-hour authorized state. | Expiration behavior cannot be validated before first authorization exists; no API/DB time manipulation was used. |

## 2026-09-04 Fix loop: registration manager cannot see upload approval todo

- BDD: claimable registration certificate upload approval is visible in manager todo -> Given a registration certificate upload approval task is assigned to another registration manager, and the current user has role `dcc_registration_certificate_approver` plus permission `dcc:registration-certificate:upload:approve`; When the current user opens Approval Center / 待办 and searches by the uploaded certificate number; Then the claimable upload approval task is returned so the manager can open and approve it through the frontend.
- RED: `mvn.cmd -pl yudao-module-bpm -Dtest=BpmNativeApprovalTaskProviderTest#pageTodoFindsClaimableRegistrationCertificateUploadTaskWhenKeywordIsCertificateNo test` -> INTERRUPTED after long no-output Maven startup; expected failure before production fix is that `pageTodo()` only returns personal assigned tasks and does not query claimable registration certificate upload approvals.

## 2026-09-04 Fix loop: no company authorization required and reminder config migration

- User decision: registration certificate upload/current-list verification no longer requires company authorization; E2E business actions must continue through frontend only.
- BDD: current registration certificate page is not blocked by company authorization -> Given `wanglixuan` has registration certificate page/upload permission but no enabled company scope; When the user opens the registration certificate page and upload dialog through the frontend; Then the current list and upload candidate endpoints return code 0 and the user can submit the upload form.
- RED: frontend-only E2E `upload-front-only-20260904074118.json` -> FAIL. `/admin-api/dcc/registration-certificates/page` returned business code `1081001002` / `User has no enabled company scope`; upload owner-company/project-code candidates then failed in the page flow.
- Root cause: the running `yudao-server-exec.jar` embedded an old `yudao-module-dcc-2026.04-SNAPSHOT.jar`; `DccRegistrationCertificateQueryServiceImpl.scopedCompanyIds()` in the executable jar still called `MdmCompanyScopeApi.getEnabledCompanyIdsForUser(...)`, while the source code had already been changed to return an empty no-scope filter. Repackaging server without first installing the changed DCC module pulled stale bytes from the local Maven repository.
- Regression tests updated:
  - `DccRegistrationCertificateUploadServiceTest#listOwnerCompaniesReturnsTenantOwnedCandidatesWithoutCompanyScope`
  - `DccRegistrationCertificateQueryServiceTest#pageListsTenantCurrentCertificatesWithoutCompanyScopeAndAuditsReturnedObjects`
- GREEN:
  - `mvn.cmd -pl yudao-module-dcc -Dtest=DccRegistrationCertificateUploadServiceTest#listOwnerCompaniesReturnsTenantOwnedCandidatesWithoutCompanyScope test` -> PASS, 1 test.
  - `mvn.cmd -pl yudao-module-dcc -Dtest=DccRegistrationCertificateQueryServiceTest#pageListsTenantCurrentCertificatesWithoutCompanyScopeAndAuditsReturnedObjects test` -> PASS, 1 test.
  - `mvn.cmd -pl yudao-module-dcc -DskipTests install` -> PASS.
  - `mvn.cmd -pl yudao-server -DskipTests package` -> PASS.
  - Executable jar class check: extracted nested `yudao-module-dcc-2026.04-SNAPSHOT.jar`; `DccRegistrationCertificateQueryServiceImpl.scopedCompanyIds()` now returns `List.of()` and no longer invokes `getEnabledCompanyIdsForUser`.
- Follow-up frontend-only E2E: `upload-front-only-20260904080710.json` -> E2E-1 PASS, E2E-3 PASS, E2E-3-approve FAIL with code `1080000275` / `未配置注册证提醒任务`.
- BDD: business event notification uses configured reminder recipients instead of Quartz job parameters -> Given migration `20260903_dcc_registration_certificate_threshold_recipient_config.sql` moved reminder recipients to `dcc_registration_certificate_reminder_config.threshold_recipient_user_ids_json` and reduced `registrationCertificateReminderDailyJob.handler_param` to actor metadata; When upload approval formalizes a new registration certificate and sends the business event notification; Then notification recipients are resolved from tenant reminder config and the approval transaction is not blocked by missing old `roleIds` in the job parameter.
- RED: frontend-only E2E `upload-front-only-20260904080710.json` -> FAIL. Manager review request `POST /admin-api/approval-center/tasks/review` returned HTTP 200 / business code `1080000275` / `未配置注册证提醒任务`.
- Root cause: `DccRegistrationCertificateBusinessEventNotificationConfigService.resolveRecipientScope()` still read `infra_job.handler_param` and required `roleIds` plus `permission`; the newer reminder recipient migration intentionally stores recipients in `dcc_registration_certificate_reminder_config.threshold_recipient_user_ids_json`, so the old reader rejected valid migrated runtime data.
- Regression tests added/updated:
  - `DccRegistrationCertificateBusinessEventNotificationConfigServiceTest#resolveRecipientUserIdsReadsThresholdRecipientConfigInsteadOfReminderJobParam`
  - `DccRegistrationCertificateBusinessEventNotificationConfigServiceTest#missingActiveConfigFailsClearlyWithoutDefaultingToJobParam`
  - `DccRegistrationCertificateBusinessEventNotificationTest#configuredRecipientUserIdsSendWithoutRoleCompanyScopeAndIncludeActor`
- GREEN:
  - `mvn.cmd -pl yudao-module-dcc -Dtest=DccRegistrationCertificateBusinessEventNotificationConfigServiceTest test` -> PASS, 2 tests.
  - `mvn.cmd -pl yudao-module-dcc -Dtest=DccRegistrationCertificateBusinessEventNotificationTest#configuredRecipientUserIdsSendWithoutRoleCompanyScopeAndIncludeActor test` -> PASS, 1 test.
  - `mvn.cmd -pl yudao-module-dcc -Dtest=DccRegistrationCertificateBusinessEventNotificationTest test` -> PASS, 8 tests.
  - `mvn.cmd -pl yudao-module-dcc -DskipTests install` -> PASS.
  - `mvn.cmd -pl yudao-server -DskipTests package` -> PASS.
- Runtime: stopped only the task-owned backend listener on `48097`, verified command line contained this worktree and `--server.port=48097`, then restarted the same worktree jar. Health endpoint returned UP.
- Final frontend-only E2E command: node `doc\tasks\20260904-registration-certificate-upload-e2e-acceptance-worktree\upload-front-only.cjs` against `http://localhost:8097`, using real frontend pages and natural browser requests only for business actions.
- Final E2E result: PASS, result file `artifacts/upload-front-only-20260904082602.json`.
  - PREP manager signature image: PASS, active signature image already present.
  - E2E-1 upload submit: PASS, `wanglixuan` submitted `E2E-UPLOAD-20260904082602`, upload POST returned HTTP 200 / code 0.
  - E2E-3 manager todo: PASS, `chudongchuan` found the target approval task by certificate number.
  - E2E-3-approve: PASS, manager review POST returned HTTP 200 / code 0 and page showed `审核已通过`.
  - E2E-4 current list: PASS, `wanglixuan` filtered current registration certificates by `E2E-UPLOAD-20260904082602` and found one CURRENT row with company `上海七木医疗器械有限公司`, product `注册证上传E2E产品-20260904082602`, project code `IDI`, registration file present.
- Experience consolidation: merged reusable lessons into `docs/e2e-rules.md` and `docs/backend-development.md`: verify nested module bytes in executable Spring Boot Jar after child-module fixes, and do not read migrated configured notification recipients from Quartz `handler_param`.

## 2026-09-04 Full upload E2E rerun after script synchronization

- Script issue: `registration-upload-ui-only-e2e.mjs` still filtered Approval Center by URL query parameters and treated the filter chip text as row evidence; current Approval Center requires real `TableMultiFilter` interaction. The script also captured detail screenshots while the detail page was still on skeleton loading.
- Script fix: updated the script to use the real frontend filter controls, wait for keyword-bearing natural pagination requests, assert table rows instead of page text, and wait until the target certificate number is visible on the detail page before screenshot/assertion. No API/fetch business action was introduced.
- Frontend-only E2E command: node `doc\tasks\20260904-registration-certificate-upload-e2e-acceptance-worktree\registration-upload-ui-only-e2e.mjs` from `IntRuoyiBackend`, targeting `http://127.0.0.1:8097`.
- Final full-script result file: `artifacts/registration-upload-ui-only-e2e-result.json`, run key `20260904084729`.
- Final full-script case results:
  - E2E-1 self-production upload: PASS, `E2E-UPLOAD-20260904084729-SELF`.
  - E2E-2 self-production pre-approval state: PASS; current list total 0 before approval, uploader TODO row absent, uploader MY_INITIATED row present.
  - E2E-3 self-production manager approval: PASS; `chudongchuan` approved through Approval Center, review HTTP 200 / code 0.
  - E2E-4 self-production post-approval detail/current state: PASS; detail contains target certificate and uploaded file.
  - E2E-6 production-mode display: PASS for both self-production `E2E-UPLOAD-20260904084729-SELF` and entrusted-production `E2E-UPLOAD-20260904084729-ENTR`.
  - E2E-7 manager direct download: PASS; natural frontend download request returned HTTP 200.
  - E2E-1/E2E-2/E2E-3/E2E-4 entrusted-production chain: PASS, `E2E-UPLOAD-20260904084729-ENTR`.
  - E2E-8 ordinary-user download application and 24-hour download: BLOCKED; no confirmed ordinary user C credentials available through frontend-only constraints.
  - E2E-9 post-24-hour reapplication: BLOCKED; depends on E2E-8 and a natural or product-approved time advancement path.
- Log observation: latest backend log no longer contains `User has no enabled company scope` or `未配置注册证提醒任务` for the final path. It still logs `BpmTaskServiceImpl.processTaskAssigned ... 没有找到流程实例` after approval completion; this did not block E2E and should be tracked separately if BPM assignment event cleanup is in scope.

## 2026-09-04 Final rerun after table/action locator hardening

- Script issue: later full-script reruns exposed two Playwright-only stability problems, not product failures:
  - Registration certificate current-list detail action was rendered in the table fixed action column, so `.registration-certificate-current-table button` could miss the visible `详细` button.
  - Approval Center keyword text in the filter input could be mistaken for table evidence, and negative uploader TODO checks retried longer than needed.
- Script fix: use the visible button text for current-list `详细`; use `data-approval-action="review"` for Approval Center review action; treat only `.approval-center__table` text as approval-table evidence; keep one attempt for expected-absent uploader TODO and retries only for expected-present approval records.
- Final frontend-only E2E command: node `doc\tasks\20260904-registration-certificate-upload-e2e-acceptance-worktree\registration-upload-ui-only-e2e.mjs` from `IntRuoyiFronted`, targeting `http://127.0.0.1:8097`.
- Final full-script result file: `artifacts/registration-upload-ui-only-e2e-result.json`, run key `20260904101823`.
- Final full-script case results:
  - E2E-1 self-production upload: PASS, `E2E-UPLOAD-20260904101823-SELF`.
  - E2E-2 self-production pre-approval state: PASS; current list total 0 before approval, uploader TODO row absent, uploader MY_INITIATED row present.
  - E2E-3 self-production manager approval: PASS; `chudongchuan` approved through Approval Center, review HTTP 200 / code 0.
  - E2E-4 self-production post-approval detail/current state: PASS; detail contains target certificate and uploaded file.
  - E2E-6 production-mode display: PASS for both self-production `E2E-UPLOAD-20260904101823-SELF` and entrusted-production `E2E-UPLOAD-20260904101823-ENTR`.
  - E2E-7 manager direct download: PASS; natural frontend download request returned HTTP 200.
  - E2E-1/E2E-2/E2E-3/E2E-4 entrusted-production chain: PASS, `E2E-UPLOAD-20260904101823-ENTR`.
  - E2E-8 ordinary-user download application and 24-hour download: BLOCKED; no confirmed ordinary user C credentials available through frontend-only constraints.
  - E2E-9 post-24-hour reapplication: BLOCKED; depends on E2E-8 and a natural or product-approved time advancement path.
