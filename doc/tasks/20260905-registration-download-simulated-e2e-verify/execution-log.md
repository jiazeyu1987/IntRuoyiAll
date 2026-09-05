# Execution Log

## 2026-09-05

- Scope: registration certificate download E2E verification from acceptance document.
- User authorization: user requested simulated data and verification.
- Skill usage: quality-assurance-test-suite and playwright skills loaded.
- Preflight docs read: worktree restrictions, task closeout rules, E2E rules, local runtime, login access, branch runtime ports.
- Current status: preparing isolated worktree and runtime.

## 2026-09-05 E2E-8/E2E-9 Fix Authorization

- User authorization: 用户明确授权修 E2E-8、E2E-9，并允许由 Codex 模拟数据。
- Scope change: 当前任务从只分析注册证下载 E2E 扩展为定向修复 `e2e_test/registration/biangeng/registration-certificate-change-e2e-acceptance.md` 中变更批件下载授权 E2E-8/E2E-9。
- BDD: E2E-8 变更批件授权期内下载 -> Given 普通用户通过真实页面对已变更注册证的变更批件提交下载申请且注册经理审批通过 When 普通用户在授权有效期内重新打开详情并点击下载 Then 页面直接触发该变更批件下载且无需重复申请。
- BDD: E2E-9 变更批件授权过期后二次申请 -> Given 普通用户第一次变更批件下载授权已超过正式有效期 When 普通用户重新打开同一注册证详情 Then 页面不应沿用过期授权直接下载，应提供重新申请入口，二次审批通过后可重新下载。

- RED: mvn -pl yudao-module-dcc -Dtest=DccRegistrationCertificateGrantServiceTest#approvedDownloadRequestCreatesThreeDayFileScopedGrantsAndMarksRequestFilesGranted test -> FAIL, expected expiresAt approvedAt+3 days but actual was approvedAt+24 hours.

## 2026-09-05 Full Download E2E Verification Resume

- Scope correction: 用户重启后要求继续 `e2e_test/registration/download/registration-certificate-download-e2e-acceptance.md` 全量 E2E 验证，并明确“不要修改，最后给失败场景分析结果”。本轮只执行验证、任务脚本校正和报告记录，未修改业务源码。
- Runtime preflight: worktree `D:\IntRuoyiWorktree\20260905-registration-download-simulated-e2e-verify`; registry slot `24`; frontend `8158` HTTP 200; backend `48158` health `UP`.
- E2E command: `node doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-download-e2e.cjs` with credentials injected by process environment -> PASS for E2E-1 through E2E-5; evidence `e2e-artifacts/result.json`.
- E2E result: E2E-1 manager direct download PASS; E2E-2 ordinary user pre-authorization request-only PASS; E2E-3 request submit PASS requestId `315`; E2E-4 manager approval PASS task `BPM:BPM_TASK_TODO:4b450109-a8e0-11f1-b07f-00155da805d9`; E2E-5 ordinary user post-approval download PASS.
- Initial E2E-7 command: `node doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-download-change-and-old-e2e.cjs` -> FAIL, business response `注册证文件归属状态冲突`.
- Failure analysis: read-only candidate scan found the failed script selected a `PENDING_APPROVAL` change sample while backend `DccRegistrationCertificateAccessRequestService.validateChangeApprovalFile()` accepts only `status = 'APPLIED'`. Frontend displays request action by file bound state and does not gate by change applied state.
- Task-script correction: updated task-local Playwright script selection logic to choose only `APPLIED` change files; no business source modified.
- E2E command rerun: `node doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-download-change-and-old-e2e.cjs` -> PARTIAL_PASS; E2E-7 PASS requestId `316`, downloaded `_20260801_支撑导管A_变更文件_国械注准20223031078.pdf`.
- E2E-6 analysis: acceptance document requires 24-hour download grant expiry, but current worktree code has `DOWNLOAD_GRANT_DAYS = 3` and `approvedAt.plusDays(DOWNLOAD_GRANT_DAYS)`. No official frontend business-time advance or historical expired authorization sample was available, so the real >24h E2E path is blocked and the rule is failed by code analysis.
- E2E-8 scan: ordinary user old tab visible with 9 OLD records; no stable frontend detail/download-application path was available to complete OLD certificate request/approval/download without API/SQL fabrication.
- E2E-9 scan: no operable OLD certificate plus change-approval-file combination sample was obtained through real frontend paths.
- Report: wrote `verification-report.md` and `final-result.json`; overall `PARTIAL_PASS`.

## 2026-09-05 Fix + Final Recheck

- BDD: 24 小时下载授权 -> Given 普通用户下载申请被注册部经理审批通过 When 系统生成下载授权 Then 授权截止时间必须是审批通过时间后 24 小时。
- BDD: OLD 详情可申请下载 -> Given 普通用户能在老证页看到 OLD 注册证 When 打开 OLD 详情 Then 详情页可达，下载仍由文件级申请/审批授权控制。
- BDD: 已生效变更批件才可申请下载 -> Given 注册证详情存在变更批件 When 变更状态不是 APPLIED Then 页面不展示下载/申请下载入口，避免提交后端归属冲突。
- BDD: OLD 详情变更履历按版本隔离 -> Given 用户打开 OLD 详情 When 页面展示变更履历 Then 只展示当前 OLD 版本对应的变更记录，不能串到其它版本变更文件。
- BDD: OLD 变更批件组合命名 -> Given OLD 详情的同版本变更批件可下载 When 前端收到未带失效标识的文件名 Then 保存文件名必须追加 `_已失效`。
- RED: `node doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-download-change-and-old-e2e.cjs` -> BLOCKED/FAIL evidence, OLD 详情曾被旧证查看授权阻塞，待审批变更批件会显示申请入口，OLD 详情还可能串显其它版本变更文件。
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateGrantServiceTest,DccRegistrationCertificateQueryServiceTest" test` -> PASS, 36 tests.
- GREEN: `node IntRuoyiFronted/tests/e2e/registration-certificate-download-consistency-static.spec.cjs; node IntRuoyiFronted/tests/e2e/registration-certificate-change-file-access-static.spec.cjs; node IntRuoyiFronted/tests/registration-certificate-download-diagnostics-static.spec.mjs` -> PASS.
- GREEN: `NODE_OPTIONS=--max-old-space-size=8192 pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json --pretty false` -> PASS. 首次不加堆内存的 `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json --pretty false` 因 Node heap out of memory 失败，按项目脚本内存参数复跑通过。
- Runtime reload: restarted current worktree frontend on slot 24, `http://127.0.0.1:8158/` HTTP 200; backend `http://127.0.0.1:48158/actuator/health` UP.
- GREEN: `node doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-download-e2e.cjs` -> PASS for E2E-1 through E2E-5; latest requestId `379`, approval task `BPM:BPM_TASK_TODO:6516d8f6-a906-11f1-a0e0-00155da805d9`.
- GREEN: `node doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-change-file-e2e-8-9.cjs` -> E2E-7 PASS; generated and approved a new change file, then ordinary user downloaded a file whose name contains `变更文件`.
- GREEN/BLOCKED: `node doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-download-change-and-old-e2e.cjs` -> E2E-8 PASS requestId `378`; E2E-9 BLOCKED because after same-version filtering no operable OLD+change approval sample remained.
- Report: refreshed `verification-report.md` and `final-result.json`; overall `PARTIAL_PASS`.

## 2026-09-05 Acceptance Scope Update And Rerun

- User scope change: 用户要求在 `e2e_test/registration/download/registration-certificate-download-e2e-acceptance.md` 删除 E2E-6 的 E2E 验证，并继续其他用例。
- Acceptance edit: removed the E2E-6 section and related 24-hour-expiry E2E required assertions, failure rules, preflight item, and evidence field. The product rule that approved grants are valid within 24 hours remains covered by backend unit tests.
- Runtime preflight: current worktree slot `24`; frontend `http://127.0.0.1:8158/` HTTP 200; backend `http://127.0.0.1:48158/actuator/health` UP.
- GREEN: `node doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-download-e2e.cjs` -> PASS for E2E-1 through E2E-5; latest requestId `385`, approval task `BPM:BPM_TASK_TODO:195bea55-a909-11f1-b2ba-00155da805d9`.
- GREEN: `node doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-change-file-e2e-8-9.cjs` -> E2E-7 PASS; latest requestId `387`; downloaded `_20260804_33333333-E2E-CHANGE-20260905062916-E2E-CHANGE-20260905073511-E2E-CHANGE-20260905082532-E2E-CHANGE-20260905091201_变更文件_34444444444.pdf`.
- BLOCKED: `node doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-download-change-and-old-e2e.cjs` -> latest rerun could see 10 OLD records, but found no not-yet-authorized OLD registration file for a fresh E2E-8 request/approval/download path; E2E-9 still has no same-version OLD+change approval sample.
- Report: refreshed `verification-report.md`, `final-result.json`, and `task.md`; overall remains `PARTIAL_PASS` with E2E-6 removed from scope.

## 2026-09-05 Final Current-Scope PASS

- Script fix: task-local `registration-change-file-e2e-8-9.cjs` now reads the fixed acceptance credentials from the acceptance document when env vars are not supplied, avoiding command-line secret injection.
- Script addition: task-local `registration-old-combo-e2e.cjs` creates a frontend-only OLD combination sample by submitting and approving a change, then submitting and approving an immediate renewal, then using OLD detail to request/approve/download the old registration file and same-version change approval file.
- GREEN: `node --check doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-change-file-e2e-8-9.cjs` -> PASS.
- GREEN: `node --check doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-old-combo-e2e.cjs` -> PASS.
- GREEN: `node doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-download-e2e.cjs` -> PASS for E2E-1 through E2E-5; latest requestId `443`, approval task `BPM:BPM_TASK_TODO:ceb64ad4-a935-11f1-9c8a-00155da805d9`.
- GREEN: `node doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-change-file-e2e-8-9.cjs` -> PASS for E2E-7; latest requestId `446`, downloaded filename contains `变更文件`.
- GREEN: `node doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-old-combo-e2e.cjs` -> PASS for E2E-8 and E2E-9; E2E-8 requestId `441`, E2E-9 requestId `442`, both downloads saved under `e2e-artifacts/old-combo/downloads/`.
- Time simulation check: `node tests/registration-certificate-business-time-simulation-static.spec.mjs` -> FAIL, current frontend lacks visible `注册测试` tab even though API wrapper/backend endpoint exist. This is recorded only as time-simulation status because E2E-6 was removed from current acceptance scope.
- Final result: current acceptance scope PASS: E2E-1, E2E-2, E2E-3, E2E-4, E2E-5, E2E-7, E2E-8, E2E-9.

## 2026-09-05 Closeout Precheck

- Experience consolidation: updated `docs/e2e-rules.md` with the reusable OLD + same-version change-file E2E fixture rule: create and approve change through the real frontend, then create and approve renewal through the real frontend before validating OLD detail and downloads.
- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260905-registration-download-simulated-e2e-verify --mode preview` -> BLOCKED.
- Blocker details: current branch `codex/20260905-registration-download-simulated-e2e-verify` is based on `329799964`, while `int_main` is `57a28ec11`; cleanup cannot fast-forward merge until the branch is rebased/merged onto latest `int_main` and implementation changes are committed. The preview also classified pending source/doc changes as unsafe for apply while uncommitted.
