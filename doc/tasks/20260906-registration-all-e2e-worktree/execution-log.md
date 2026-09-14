# Execution Log

## Preflight

- GREEN: read-quality-assurance-test-suite-skill -> PASS.
- GREEN: read-playwright-skill -> PASS.
- GREEN: read-AGENTS -> PASS.
- GREEN: read-docs-task-closeout-rules -> PASS.
- GREEN: read-docs-e2e-rules -> PASS.
- GREEN: read-docs-login-access -> PASS.
- GREEN: read-docs-worktree-restrictions -> PASS.
- GREEN: read-docs-local-runtime -> PASS.
- GREEN: read-docs-branch-runtime-ports -> PASS.

## Worktree Setup

- GREEN: create-worktree -> PASS. Created `D:\IntRuoyiWorktree\20260906-registration-all-e2e-worktree` on branch `codex/20260906-registration-all-e2e-worktree`.
- GREEN: reserve-worktree-slot -> PASS. Reserved `int_main` slot `24`, frontend `8158`, backend `48158`.
- GREEN: npx-prerequisite-check -> PASS. `npx --version` returned `11.6.2`.
- GREEN: runtime-env-preflight -> PASS. Required DCC download encryption environment variable names are present; values were not printed.
- GREEN: port-preflight -> PASS. Worktree ports `8158` and `48158` were not listening before runtime startup.
- BLOCKED: backend-jar-preflight -> `IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar` was missing; backend startup must run with `-Build` before Playwright E2E.

## BDD

- BDD: 全量注册证 E2E 结果记录 -> Given `E:\IntRuoyi\e2e_test\registration` 下存在多份注册证 E2E 验收文档；When 在专用 worktree 逐个执行每个 E2E 小节；Then 每个用例都有 PASS/FAIL/SKIPPED/BLOCKED 结果、证据路径和原因说明。
- BDD: 先分析后集中修复 -> Given 第一轮 E2E 存在失败；When 所有用例第一轮执行完成；Then 先完成失败原因代码分析，再集中修改，修改后重新执行验证。

## Case Matrix

- GREEN: parse-registration-e2e-docs -> PASS. Parsed 37 E2E cases from `e2e_test\registration` into `doc\tasks\20260906-registration-all-e2e-worktree\e2e-case-matrix.md`.

## 2026-09-06 Resume

- GREEN: previous-turn-classification -> PROGRESS. The prior turn synchronized registration E2E document wording in the main workspace; this resume synchronized the same document contract into the active worktree before continuing runtime verification.
- GREEN: sync-current-registration-doc-contract -> PASS. Worktree E2E docs now include the current skip-note, `artifacts/downloads` wording, approval permission labels, fixed `wanglixuan` / `chudongchuan` context, and no longer include the removed password write prohibition wording in the scoped document rules.
- GREEN: skip-note-structure-check -> PASS. Verified download E2E-2..E2E-8, upload E2E-7..E2E-8, and biangeng E2E-8..E2E-9 begin with `若已触发跳过条件，本节记为 SKIPPED。`.
- GREEN: runtime-health-current -> PASS. Worktree frontend `http://127.0.0.1:8158/` returned HTTP 200 and backend `http://127.0.0.1:48158/actuator/health` returned `UP`.

## Fix Batch 7

- Cause addressed: `change-submit-approval` reused the single upload fixture whose selected company could fall outside `wanglixuan` change scope. The upload approval E2E now creates two task-owned approved certificates and selects an owner company available to the applicant when no explicit company override is supplied.
- Cause addressed: `change-remaining` reused the same changed certificate and the production-relation select did not prove the selected value before submit. The round runner now passes a second uploaded certificate to the remaining-fields E2E, and the select helper polls the rendered selected value before submitting.
- Cause addressed: `real-flow` selected a CURRENT row without ensuring project code/file prerequisites for download-request evidence. Candidate selection now requires project code and registration file when write-fixture mode is enabled.
- Cause addressed: `renewal-lifecycle` reached activation but timed out while closing the auxiliary context near the test deadline. The activation evidence is recorded immediately after the page-triggered daily run and the test timeout is increased.
- CHECK: `node --check` passed for the four changed E2E scripts.
- CHECK: PowerShell parser tokenization passed for `run-registration-e2e-round.ps1`.

## Round 8 After Fix 7

- GREEN: round8-runner -> completed. Summary path: `doc/tasks/20260906-registration-all-e2e-worktree/artifacts/round8-after-fix7/summary.json`.
- RESULT: 11 PASS, 4 FAIL. Failing cases: `change-submit-approval`, `change-remaining`, `real-flow`, `renewal-lifecycle`.
- FAIL_ANALYSIS: `change-submit-approval` -> upload owner-company selector showed a company that later failed submit-time company-scope validation: `当前账号无该公司注册证上传权限，请选择已授权公司`.
- FAIL_ANALYSIS: `change-remaining` -> second production mode select timed out because the helper reused/observed a stale global dropdown instead of reopening the specific select.
- FAIL_ANALYSIS: `real-flow` -> `wanglixuan` lacks `dcc:registration-certificate:config:query`, so `/user/profile?tab=config&config=registrationCertificate` resolves to the default workbench; the config subsection should be SKIPPED for this account.
- FAIL_ANALYSIS: `renewal-lifecycle` -> initial upload and renewal approval both completed, but the auxiliary browser context close raised `Target.disposeBrowserContext` after activation, masking the remaining lifecycle assertions.

## Fix Batch 8

- RED/GREEN: `mvn -pl yudao-module-dcc -Dtest=DccRegistrationCertificateUploadServiceTest#listOwnerCompaniesReturnsOnlyTenantOwnedCandidatesInUserCompanyScope test` -> PASS after changing upload owner-company candidates to filter by `companyScopeApi.getEnabledCompanyIdsForUser(actorId)`.
- FIX: `registration-certificate-change-remaining-real.spec.js` now reopens the concrete select each time and avoids stale dropdown reuse between production-mode fields.
- FIX: `registration-certificate-real-flow.spec.js` now treats the registration certificate config subsection as SKIPPED when the logged-in account lacks the config query permission, while preserving the config assertions when permission exists.
- FIX: `registration-certificate-renewal-lifecycle-real.spec.js` records activation evidence before cleanup and captures auxiliary context close errors instead of failing the business lifecycle on cleanup.
- CHECK: `node --check` passed for changed E2E scripts in Fix Batch 8.

## 2026-09-07 Resume

- GREEN: previous-turn-classification -> PROGRESS. The prior continuation added dynamic upload-company selection and executed round 11, producing new failure evidence under `doc/tasks/20260906-registration-all-e2e-worktree/artifacts/round11-after-fix10/`.
- GREEN: runtime-health-current -> PASS. Worktree frontend `http://127.0.0.1:8158/` returned HTTP 200 and backend `http://127.0.0.1:48158/actuator/health` returned `UP`.
- CHECK: `node --check` -> PASS for `registration-certificate-upload-admin-role-approval-real.spec.js`, `registration-certificate-upload-submit-repro.spec.js`, and `registration-certificate-renewal-lifecycle-real.spec.js`.

## Round 11 After Fix 10

- GREEN: round11-runner -> completed. Summary path: `doc/tasks/20260906-registration-all-e2e-worktree/artifacts/round11-after-fix10/summary.json`.
- RESULT: 10 PASS, 3 FAIL, 2 BLOCKED. Failing cases: `upload-admin-role-approval`, `upload-submit-repro`, `renewal-lifecycle`. Blocked cases: `change-submit-approval`, `change-remaining`, both pending task-owned approved upload certificates from the upload approval E2E.
- FAIL_ANALYSIS: `upload-admin-role-approval` -> after selecting the first available project code, the upload company dropdown had no enabled option and the page emitted `当前账号未配置可用授权公司`; the applicant account `wanglixuan` currently has no usable company scope for upload.
- FAIL_ANALYSIS: `upload-submit-repro` -> same root cause as upload approval; `wanglixuan` can open the upload dialog, but company candidates are empty because company-scope prerequisites are missing.
- FAIL_ANALYSIS: `renewal-lifecycle` -> same root cause while creating the initial task-owned certificate required for the renewal lifecycle; no renewal business action could start because the upload precondition failed.

## Company Scope Precondition Attempt

- GREEN: readonly-menu-probe -> PASS. Logged in as `admin` through the frontend and confirmed `get-permission-info` does not include `mdm:company-scope:*`; visible menus include `基础数据 / 注册证管理 / 关联公司 / 注册证` but not `授权公司`.
- BLOCKED: frontend-company-scope-precondition -> current frontend session cannot open `/mdm/company-scope`, `/mdm/company-scope/index`, `/company-scope`, or `/basic-data/company-scope`; each route renders the 404 page. Because project rules forbid API/DB writes for E2E setup without explicit authorization, `wanglixuan` company-scope setup cannot be completed in this turn.

## 2026-09-07 Company Scope Recheck

- GREEN: runtime-health-current -> PASS. Worktree frontend `http://127.0.0.1:8158/` returned HTTP 200 and backend `http://127.0.0.1:48158/actuator/health` returned `UP`; listener PIDs belong to the current worktree paths.
- GREEN: company-scope-route-recheck -> PASS. The current worktree frontend can now open `/mdm/company-scope` and render `data-testid="mdm-company-scope-page"`.
- GREEN: company-scope-filter-script-fix -> PASS. Updated the task-local UI setup script to use the current `TableMultiFilter` quick-filter structure; `node --check` passed.
- BLOCKED: company-scope-create-permission -> The real page shows no `新增授权公司` button for the logged-in admin session, and filtering for `wanglixuan` returns no enabled authorization company rows. The upload, change-baseline, and renewal-baseline E2E paths remain blocked by missing frontend-manageable company scope. Project rules require explicit current-task authorization before modifying role/menu/user/data baselines, so no API/DB write or permission rewrite was performed.

## 2026-09-07 Company Scope Code Cause Recheck

- GREEN: previous-turn-classification -> PROGRESS. The prior continuation fixed the task-local company-scope preflight script and proved the page-level blocker with real browser evidence.
- GREEN: company-scope-permission-code-review -> PASS. `IntRuoyiFronted/src/views/mdm/company-scope/index.vue` guards the `新增授权公司` button with `v-hasPermi="['mdm:company-scope:create']"`, while `MdmCompanyScopeController#createCompanyScope` requires `@ss.hasPermission('mdm:company-scope:create')`.
- GREEN: super-admin-wildcard-code-review -> PASS. The current worktree code adds `*:*:*` only when the authenticated role code is `super_admin`; the real admin session captured from the page has registration/DCC roles but not `super_admin`, so the hidden create button is consistent with current permission data rather than a frontend selector issue.
- BLOCKED: company-scope-baseline-repeat -> Same blocker remains: no current-task authorization has been provided to change role/menu/user/data baselines, and there is still no frontend-visible enabled company-scope row for `wanglixuan`. Per `docs/login-access.md`, this prevents upload/change/renewal writing E2E from creating task-owned approved certificates.

## 2026-09-07 Blocker Revalidation

- GREEN: current-state-reload -> PASS. Re-read QA skill, task-closeout rules, E2E rules, login access, local runtime, and worktree restrictions before continuing the goal.
- GREEN: runtime-health-current -> PASS. Worktree frontend `http://127.0.0.1:8158/` returned HTTP 200 and backend `http://127.0.0.1:48158/actuator/health` returned `UP`.
- BLOCKED: company-scope-baseline-repeat -> Re-ran task-local Playwright UI setup script `ensure-registration-company-scope-ui.cjs`; it logged in through the real frontend and again failed waiting for visible `新增授权公司`. This repeats the same frontend-manageable company-scope baseline blocker and prevents upload, change-baseline, and renewal-baseline E2E paths from progressing without explicit authorization to change role/menu/user/data baseline.

## 2026-09-07 Authorization Baseline Repair

- USER_AUTHORIZATION: User explicitly authorized adjusting the role/menu permissions required for this task.
- GREEN: mdm-company-scope-menu-migration -> PASS. Applied existing migration `IntRuoyiBackend/sql/mysql/20260830_mdm_company_scope_crud_menu.sql` to restore active `mdm:company-scope:query/create/update/delete` menu records and bind them to tenant 1 `super_admin`.
- GREEN: company-scope-ui-setup-script-fix -> PASS. Updated task-local `ensure-registration-company-scope-ui.cjs` for current `UserSelectV2` dialog structure; `node --check` passed.
- GREEN: company-scope-real-ui-create -> PASS. Through real frontend at `http://127.0.0.1:8158`, admin opened `/mdm/company-scope`, used visible `新增授权公司`, selected `wanglixuan`, selected `上海七木医疗器械有限公司`, and POST `/admin-api/mdm/company-scope/create` returned HTTP 200.

## 2026-09-07 Final Repair And Verification

- GREEN: company-scope-readonly-confirmation -> PASS. Confirmed `wanglixuan` has enabled company scope for `上海七木医疗器械有限公司`, matching the owner company used by the upload, change, and renewal E2E data.
- FIX: `DccRegistrationCertificateChangeService.resolveOwnerCompanyName` now queries active owned companies with status `ENABLE`, matching current `mdm_enterprise` data and removing the false company-scope denial during change submit.
- GREEN: `mvn.cmd --% -pl yudao-module-dcc -Dtest=DccRegistrationCertificateChangeServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS. Tests run: 12, failures: 0, errors: 0, skipped: 0.
- GREEN: `mvn.cmd --% -pl yudao-server -am -DskipTests package` -> PASS. Rebuilt `yudao-server-exec.jar` and restarted only the slot-24 worktree backend on port `48158`; health returned `UP`.
- FIX: `registration-certificate-change-remaining-real.spec.js` now selects Element Plus multiselect options from the active dropdown DOM and verifies each structured field appears before filling it.
- FIX: `registration-certificate-renewal-lifecycle-real.spec.js` now logs in to the admin activation page with the same robust login flow as the business-time simulation test, derives an unused-safe business date from the run key when not supplied, and applies the documented current-task skip before requiring old-view access authorization controls.
- CHECK: `node --check` -> PASS for `registration-certificate-change-remaining-real.spec.js` and `registration-certificate-renewal-lifecycle-real.spec.js`.
- GREEN: `change-remaining` targeted rerun `round18-targeted-change-remaining` -> PASS.
- GREEN: `renewal-lifecycle` targeted rerun `round22-targeted-renewal-lifecycle` -> PASS with business date `2026-09-06`.
- GREEN: `round23-final-registration-all-e2e` -> PASS. Summary path: `doc/tasks/20260906-registration-all-e2e-worktree/artifacts/round23-final-registration-all-e2e/summary.json`.
- RESULT: 15 PASS, 0 FAIL, 0 BLOCKED in the final round: `action-panel`, `upload-admin-role-approval`, `change-continue-approval`, `change-submit-approval`, `change-remaining`, `change-ui-smoke`, `download-search-targeted`, `list-sort`, `real-flow`, `renewal-lifecycle`, `renewal-row-dialog`, `upload-button`, `upload-submit-repro`, `reminder-sort-runtime`, and `business-time-simulation`.
- GREEN: project-experience-consolidation -> PASS. Added reusable E2E lessons to `docs/e2e-rules.md` for Element Plus active multiselect dropdown selection and daily-task business-date reuse avoidance.
- BLOCKED: closeout-cleanup-preview -> `task-closeout-cleanup` preview reported cleanup cannot safely apply because the main worktree `E:\IntRuoyi` is dirty, the current branch cannot be fast-forward merged into `int_main`, and this turn has no Git commit/push authorization. E2E verification remains PASS; repository closeout is intentionally left at `ready_for_closeout`.

## 2026-09-07 Reminder Document Closure

- GREEN: reminder-r25-data-prep -> PASS. Through real frontend upload and approval pages, created and approved task-owned certificates `E2E-REMINDER-30M-R25-30M-0`, `E2E-REMINDER-8M-R25-8M-0`, `E2E-REMINDER-2M-R25-2M-0`, and `E2E-REMINDER-1M-R25-1M-0`; all are owned by `上海七木医疗器械有限公司` and hit business date `2026-09-03`.
- GREEN: reminder-r25-config-and-delivery -> PASS. `registration-reminder-config-and-delivery-real.cjs` saved threshold recipients through the registration certificate page, triggered the `2026-09-03` daily run through the `注册测试` tab, and verified recipient/non-recipient visibility in `我的站内信`.
- GREEN: reminder-r25-visual-state -> PASS. Real detail pages showed `提醒：到期前 8 个月` for the T_8 certificate and `提醒：到期前 2 个月` for the T_2 certificate; screenshots stored under `artifacts/round25-reminder-doc/`.
- GREEN: reminder-r26-data-prep -> PASS. Through real frontend upload and approval pages, created and approved `E2E-REMINDER-30M-R26-30M-0`, effective for a T_30 hit on business date `2026-09-01`.
- GREEN: reminder-r26-reconfiguration -> PASS. Through real frontend notification settings, changed the T_30 recipient to `wanglixuan`, triggered `2026-09-01` through the `注册测试` tab, verified `wanglixuan` saw the T_30 reminder and `chudongchuan` did not, then restored the T_30 recipient to `chudongchuan` through the real frontend.
- GREEN: document-case-matrix-finalization -> PASS. Updated `e2e-case-matrix.md` and `verification-report.md` so every registration E2E document case has a final `PASS` or document-rule `SKIPPED` result.
- GREEN: round27-post-reminder-closure -> PASS. Re-ran the complete registration E2E runner after the upload script parameterization and supplemental reminder closure. Summary path: `doc/tasks/20260906-registration-all-e2e-worktree/artifacts/round27-post-reminder-closure/summary.json`.
- RESULT: 15 PASS, 0 FAIL, 0 BLOCKED in Round 27: `action-panel`, `upload-admin-role-approval`, `change-continue-approval`, `change-submit-approval`, `change-remaining`, `change-ui-smoke`, `download-search-targeted`, `list-sort`, `real-flow`, `renewal-lifecycle`, `renewal-row-dialog`, `upload-button`, `upload-submit-repro`, `reminder-sort-runtime`, and `business-time-simulation`.
- GREEN: project-experience-consolidation-reminder -> PASS. Added the reusable notification-message false-positive gate to `docs/e2e-rules.md`.

## 2026-09-07 int_main Fusion

- GREEN: worktree-task-commit -> PASS. Created source worktree commit `c8f4498da` with the verified registration E2E implementation, E2E scripts, document updates, and core task records.
- GREEN: int_main-clean-partial-apply -> PASS. Applied the clean code/script/task-record portion into `E:\IntRuoyi` without touching unrelated unstaged MES/frontline changes.
- GREEN: int_main-document-merge -> PASS. Manually merged the six document files that already had `int_main` local edits, preserving the current authorization notes and adding the missing registration E2E wording.
- GREEN: int_main-commit -> PASS. Created `int_main` commit `acfcd7a39` for the fused registration E2E verification changes.

## 2026-09-07 int_main Post-Fusion E2E Attempt

- USER_AUTHORIZATION: User explicitly authorized restarting `int_main` local backend `48081` and frontend `8081` if needed before the post-fusion E2E.
- GREEN: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS. Confirmed `int_main` runtime ports remain frontend `8081` and backend `48081`.
- BLOCKED: `IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main` -> FAIL. Standard full restart could not build `yudao-server` because `yudao-module-mes` test compilation failed at `MesProScheduleOrderServiceImplTest.java:3066` with `MesProScheduleOrderBatchReqVO` not assignable to `MesProScheduleOrderDeleteReqVO`.
- BLOCKED: post-fusion-registration-e2e -> `48081` is not listening after the failed standard restart, so the registration E2E cannot prove behavior against the fused `int_main` backend runtime. Per the runtime code-source gate, no Playwright registration E2E was started against an unavailable or stale backend.
- USER_AUTHORIZATION: User requested fixing the blocker first and then continuing the E2E.
- GREEN: `IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main` -> PASS on rerun. Standard full restart rebuilt and started the fused `int_main` runtime: frontend `8081`, backend `48081`, backend health `UP`.
- REGRESSION: `int-main-post-fusion-r1` -> 14 PASS, 1 FAIL. `download-search-targeted` timed out because the Element Plus date picker exposed month navigation buttons as `Previous Month` / `Next Month`, while the script only looked for Chinese accessible names.
- FIX: `registration-certificate-download-search-targeted.spec.cjs` now accepts both Chinese and English date picker month button accessible names.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\registration-certificate-download-search-targeted.spec.cjs` -> PASS.
- GREEN: targeted `download-search-targeted` rerun `int-main-post-fusion-download-r2` -> PASS.
- GREEN: `int-main-post-fusion-r2` -> PASS. Summary path: `doc/tasks/20260906-registration-all-e2e-worktree/artifacts/int-main-post-fusion-r2/summary.json`.
- RESULT: 15 PASS, 0 FAIL, 0 BLOCKED in the final fused `int_main` round: `action-panel`, `upload-admin-role-approval`, `change-continue-approval`, `change-submit-approval`, `change-remaining`, `change-ui-smoke`, `download-search-targeted`, `list-sort`, `real-flow`, `renewal-lifecycle`, `renewal-row-dialog`, `upload-button`, `upload-submit-repro`, `reminder-sort-runtime`, and `business-time-simulation`.
