# Execution Log

## User Intent

- 用户要求在 `E:\IntRuoyi` 对 DCC 文控“文件作废/废止”进行真实 Playwright E2E 验证。
- 初始要求覆盖手动作废申请、审批/签名、作废生效、受控浏览最终效果和只读 API/DB 核验。
- 用户后续明确变更验收口径：`作废先不走审批, 文件升版本的时候老的版本自动作废, 走这条链路`。
- 当前任务按最新口径验证“升版发布后旧版本自动失效/SUPERSEDED”，不再把手动作废审批作为完成门禁。
- 2026-08-02 19:30:25 +08:00：用户要求“进行一次完整的修改之后的作废链路 E2E 验证”，本轮将跑全新任务自有文件的原版上传、四级审批/签名、升版发布、旧版自动失效、受控浏览和只读 DB 核验。
- 2026-08-02 21:28:23 +08:00：用户再次要求“进行一次完整的 E2E 验证”，本轮继续按最新口径执行全新任务自有文件的升版自动失效链路，不复用旧链路结果。
- 2026-08-02 22:27:23 +08:00：用户确认后端已连上后继续执行全新任务自有文件完整链路，并以该轮作为最新验收结果。

## Preconditions Read

- 2026-08-02：已读取 `AGENTS.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/frontend-development.md`、`docs/task-closeout-rules.md`。
- 2026-08-02：已补充读取 `docs/database-rules.md`、`docs/local-runtime.md`、`docs/powershell-encoding.md`。
- 2026-08-02：已读取 Playwright 技能 `C:\Users\BJB110\.codex\skills\playwright\SKILL.md`。
- 2026-08-02：`npx --version` 返回 `11.6.2`，满足 Playwright CLI 技能前置。
- 2026-08-02 21:25 +08:00：本轮复验前重新读取 `AGENTS.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/frontend-development.md`、`docs/task-closeout-rules.md`，并按运行态/数据库/编码触发补读 `docs/local-runtime.md`、`docs/database-rules.md`、`docs/powershell-encoding.md`、`docs/worktree-restrictions.md`、`docs/backend-development.md`、`docs/branch-runtime-ports.md`。
- 2026-08-02 22:27 +08:00：继续前再次复核必读规则与 Playwright 技能；本轮仅验证 DCC 升版自动失效作废链路，不扩展其它 DCC 场景。

## BDD

- BDD: DCC revision publish automatically invalidates old current version -> Given a task-owned DCC controlled file V1.0 is the current effective version, When a non-admin user completes V2.0 revision upload, approval/signature, and effective publish through the real UI path, Then V1.0 becomes `SUPERSEDED`, V2.0 becomes `ACTIVE`, master current active version points to V2.0, controlled browsing no longer returns V1.0 as a current active row, and version history/traceability shows the revision reason and approval/signature evidence.
- BDD: Manual obsolete approval remains out of current scope after clarification -> Given the manual obsolete dialog can be opened but no published runtime OBSOLETE policy exists, When the user clarifies that obsolete should be verified via revision publish instead, Then the manual policy blocker is recorded but not used as the acceptance path.
- BDD: Full fresh rerun after modification -> Given a new task-owned DCC controlled file is created by the E2E, When the full original-release and revision-publish UI chain completes with non-admin approvers/signers, Then the newly created V1 is automatically `SUPERSEDED`, the newly created V2 is `ACTIVE`, master points to the newly created V2, and controlled browsing no longer exposes the newly created V1 as current effective.

## TDD / E2E Evidence

- GREEN: runtime-preflight -> PASS, frontend `http://127.0.0.1:8081/` returned HTTP `200` and backend `http://127.0.0.1:48081/actuator/health` returned `UP`.
- GREEN: existing-revision-chain-readonly-precheck -> PASS, V1 `2054545668044070271` is `SUPERSEDED`, V2 `2054545668044070272` is `ACTIVE`, master `2054545668044062882` points to V2, publish instance `437` is `EFFECTIVE`.
- RED: supplemental Playwright run 1 -> FAIL, expected reason: verification script still expected old `viewer=1` URL, while current controlled browser opened V2 traceability detail with `traceability=1&from=browser`.
- RED: supplemental Playwright run 2 -> FAIL, expected reason: SPA route did not emit a `load` navigation event for `page.waitForURL`; script changed to poll current URL without weakening business assertions.
- RED: supplemental Playwright run 3 -> FAIL, expected reason: current traceability detail page does not render old viewer-only `dcc-controlled-preview-layout`; script changed to verify `dcc-detail-handling-summary` plus embedded version history table.
- GREEN: `node --check doc/tasks/20260802-dcc-revision-publish-real-e2e/dcc-revision-publish-real-e2e.cjs` -> PASS after script alignment.
- GREEN: real Playwright revision-auto-obsolete verification -> PASS, result file `doc/tasks/20260802-dcc-controlled-file-obsolete-e2e/revision-auto-obsolete-e2e-result.json`.
- GREEN: final read-only DB verification -> PASS, V1 `SUPERSEDED`, V2 `ACTIVE`, master current active V2, V1 successor V2, publish instance effective, approval/signature evidence valid.
- GREEN: result-json-final-assertion -> PASS, result status `PASS`, V1 status `SUPERSEDED`, V2 status `ACTIVE`, master current V2, `targetNetworkFailures=0`, `consoleErrors=0`, `pageErrors=0`.
- RED: full fresh clean-gate inspection for run `20260802193142` -> FAIL, expected reason: wrapper result `full-rerun-e2e-result.json` records business state `PASS`, but underlying full chain result `doc/tasks/20260802-dcc-revision-publish-real-e2e/chain-result.json` contains repeated pageerrors on publish approval page: `Cannot read properties of null (reading 'nextSibling')`; this does not satisfy the current task gate requiring target DCC/approval chain `pageErrors=0`.
- RED: full fresh real Playwright rerun with new task-owned file `CODX-DCC-REV-FULL-20260802-20260802194027` -> FAIL/BLOCKED, expected reason: V1 upload succeeded but the first V1 approval page for non-admin `zhaohaichen` threw `Cannot read properties of undefined (reading 'visible')`; the real page never rendered `审批阶段进度`, causing `locator.waitFor('text=审批阶段进度')` timeout.
- GREEN: read-only DB blocker impact check for run `20260802194027` -> PASS, no SQL/API mutation; V1 `2054545668044070296` remains `PENDING_DOC_CONTROL_REVIEW`, process `fa9edf24-8e66-11f1-93ff-00155d2984a0` has one unfinished `DOC_CONTROL_REVIEW` task assigned to `376`, and no V2/current-active switch was created.
- RED: `node tests/e2e/dcc-detail-approval-render-safety-static.spec.js` -> FAIL, expected reason: focused regression contract did not exist yet, so the approval detail render-safety gap was not locked.
- GREEN: `node tests/e2e/dcc-detail-approval-render-safety-static.spec.js` -> PASS, detail page has no `})const openControlledBrowserLocation` glue, keeps `审批阶段进度`, and all dialog `*.visible` v-models including `controlledPrintDialog` have initialized reactive state.
- GREEN: `node --check tests/e2e/dcc-detail-approval-render-safety-static.spec.js` -> PASS.
- GREEN: `pnpm e2e:dcc:detail-approval-render-safety:static` -> PASS.
- GREEN: `node tests/e2e/dcc-detail-approval-own-task-without-process-query-static.spec.js` -> PASS.
- GREEN: `pnpm e2e:dcc:detail-handling-summary:static` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: full fresh real Playwright rerun with new task-owned file `CODX-DCC-REV-FULL-20260802-20260802201023` -> PASS, V1 `2054545668044070300` became `SUPERSEDED`, V2 `2054545668044070301` became `ACTIVE`, master `2054545668044062907` points to V2, controlled browser current row is V2, traceability history shows V1/V2 and revision reason, and both wrapper and chain results have `targetNetworkFailures=[]`, `consoleErrors=[]`, `pageErrors=[]`.
- RED: runtime preflight for fresh rerun `20260802212823` -> FAIL, expected reason: backend `48081` initially refused connection after prior restart attempt, blocking real Playwright login/API calls.
- GREEN: runtime artifact diagnosis -> PASS, source `MesProProcessPoolTimelineReadMapper.xml` started with valid XML header, but generated `target/classes` copy started with null bytes; jar rebuilt later contained mapper size `10894` and XML header `3C 3F 78 6D ...`.
- RED: `mvn.cmd -pl yudao-server -am "-DskipTests" package` -> FAIL, expected reason: unrelated MDM testCompile references missing test symbols (`MdmProductImportExcelVO`, `MdmProductSaveReqVO`, import mappers), so runtime packaging could not complete with test source compilation.
- GREEN: `mvn.cmd -pl yudao-server -am "-Dmaven.test.skip=true" package` -> PASS, runtime jar packaging completed with reactor modules and current MES mapper embedded; this was used only to restore local runtime, not as product regression evidence.
- RED: full fresh real Playwright rerun with new task-owned file `CODX-DCC-REV-FULL-20260802-20260802212823` -> BLOCKED, expected reason: V1/V2 upload and DCC approvals completed, but publish approval second node `zhaojie` hit HTTP `500` on `/admin-api/bpm/task/approve`; wrapper result `full-rerun-e2e-result-20260802212823.json` and chain result are `BLOCKED`.
- RED: post-blocker backend health -> FAIL, expected reason: after reactor runtime rebuild, backend startup failed with `APPROVAL_ADAPTER_DECLARED_BUT_NOT_REGISTERED: SHOWROOM`; `http://127.0.0.1:48081/actuator/health` refused connection, so no further real E2E could safely proceed.
- GREEN: full fresh real Playwright rerun with new task-owned file `CODX-DCC-REV-FULL-20260802-20260802222723` -> PASS, V1 `2054545668044070307` became `SUPERSEDED`, V2 `2054545668044070308` became `ACTIVE`, master `2054545668044062911` points to V2, controlled browser current row is V2, traceability history shows V1/V2 and revision reason, final read-only DB verification passed, and both wrapper and chain results have `targetNetworkFailures=[]`, `consoleErrors=[]`, `pageErrors=[]`.

## Milestone Updates

- 2026-08-02：手动作废链路曾通过真实 Playwright 进入受控浏览、目标详情和“作废当前版本”弹窗，但 `/form-center/actions/resolve` 返回 `No published business approval policy matched action OBSOLETE`；该阻塞按原始口径记录，未用 API/SQL/admin 绕过。
- 2026-08-02：用户确认“作废先不走审批，文件升版本时老版本自动作废”，当前任务切换到升版自动失效链路。
- 2026-08-02：复用任务自有、已完整发布的升版链路数据 `CODX-DCC-REV-FULL-20260802-20260802091213`，不新建额外业务文件，不改状态。
- 2026-08-02：修正验证脚本对当前页面的等待锚点：受控浏览详情入口当前是 `traceability=1&from=browser`，详情页使用内嵌“版本历史”表，而非 viewer 弹窗。
- 2026-08-02：真实 Playwright 复验 PASS：非 admin `wangsiyu` 登录，受控浏览 `status=ACTIVE` 当前行是 V2，V1 不作为当前有效行返回；从受控浏览打开 V2 追溯详情，页面可见版本历史和升版原因。
- 2026-08-02：只读 DB 复验 PASS：V1 `SUPERSEDED`、V2 `ACTIVE`、master 当前有效版本 `2054545668044070272`、发布 BPM 和 DCC 电子签名证据完整。
- 2026-08-02 22:27:23 +08:00：最新全新真实 Playwright 链路 PASS：非 admin 账号完成 V1/V2 上传审批签名、发布审批和最终只读核验；V1 `2054545668044070307` 自动 `SUPERSEDED`，V2 `2054545668044070308` 为 `ACTIVE`，master `2054545668044062911` 当前有效版本为 V2，受控浏览不再把 V1 作为当前有效行返回。

## Commands / Evidence

- `node --check E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\dcc-revision-publish-real-e2e.cjs` -> PASS.
- Real Playwright command with password injected via `DCC_E2E_PASSWORD` PowerShell expression and `DCC_E2E_USE_EXISTING_CHAIN=1` -> PASS.
- Result JSON: `E:\IntRuoyi\doc\tasks\20260802-dcc-controlled-file-obsolete-e2e\revision-auto-obsolete-e2e-result.json`.
- Screenshots: `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\browser-current-v2.png`, `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\detail-version-history.png`.
- Source PASS evidence reused and revalidated: `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\e2e-result.json`.
- Latest full fresh PASS result JSON: `E:\IntRuoyi\doc\tasks\20260802-dcc-controlled-file-obsolete-e2e\full-rerun-e2e-result-20260802222723.json`.
- Latest full fresh chain result JSON: `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\chain-result.json`.
- Latest full fresh screenshots: `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\browser-current-v2.png`, `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\detail-version-history.png`.
- Secret scan: checked for plaintext passwords and quoted `DCC_E2E_PASSWORD` assignments in task evidence directories -> PASS, no matches.
- Final secret scan after report update: `SECRET_SCAN_PASS`; no plaintext password or quoted `DCC_E2E_PASSWORD` assignment found in current DCC task evidence directories.

## Full Fresh Rerun 2026-08-02

- Intent: run full real Playwright chain without `DCC_E2E_USE_EXISTING_CHAIN`, creating a fresh task-owned DCC file and validating old-version automatic invalidation through revision publish.
- Planned result path: `E:\IntRuoyi\doc\tasks\20260802-dcc-controlled-file-obsolete-e2e\full-rerun-e2e-result.json`.
- 2026-08-02 19:31:42 +08:00: business-state run completed with result `PASS` for file number `CODX-DCC-REV-FULL-20260802-20260802193142`; evidence showed V1 `2054545668044070293` -> `SUPERSEDED`, V2 `2054545668044070294` -> `ACTIVE`, master `2054545668044062902` -> V2,受控浏览当前有效行为 V2。但底层链路 `chain-result.json` 记录 publish approval pageerrors `Cannot read properties of null (reading 'nextSibling')`，因此未作为干净 E2E 放行结论。
- 2026-08-02 19:40:27 +08:00: reran the full chain with password injected by `DCC_E2E_PASSWORD` PowerShell expression and without `DCC_E2E_USE_EXISTING_CHAIN`; command exited `1`. Result path: `E:\IntRuoyi\doc\tasks\20260802-dcc-controlled-file-obsolete-e2e\full-rerun-e2e-result-20260802194027.json`; blocker: `locator.waitFor: Timeout 30000ms exceeded` waiting for `text=审批阶段进度`; chain pageerrors: `Cannot read properties of undefined (reading 'visible')` in `src/views/dcc/controlled-file/detail/index.vue`.
- 2026-08-02 20:10:23 +08:00: reran the full chain with password injected by `DCC_E2E_PASSWORD` PowerShell expression and without `DCC_E2E_USE_EXISTING_CHAIN`; command exited `0`. Result path: `E:\IntRuoyi\doc\tasks\20260802-dcc-controlled-file-obsolete-e2e\full-rerun-e2e-result-20260802201023.json`; chain result path: `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\chain-result.json`; status `PASS`; target errors all empty.
- 2026-08-02 21:28:23 +08:00: reran the full chain with password injected by `DCC_E2E_PASSWORD` PowerShell expression and without `DCC_E2E_USE_EXISTING_CHAIN`; command exited `1`. Result path: `E:\IntRuoyi\doc\tasks\20260802-dcc-controlled-file-obsolete-e2e\full-rerun-e2e-result-20260802212823.json`; chain result path: `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\chain-result.json`; status `BLOCKED`; blocker `approve publish zhaojie HTTP failed, status=500`.
- 2026-08-02 22:27:23 +08:00: reran the full chain with password injected by `DCC_E2E_PASSWORD` PowerShell expression and without `DCC_E2E_USE_EXISTING_CHAIN`; command exited `0`. Result path: `E:\IntRuoyi\doc\tasks\20260802-dcc-controlled-file-obsolete-e2e\full-rerun-e2e-result-20260802222723.json`; chain result path: `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\chain-result.json`; status `PASS`; target errors all empty.
- Latest status: PASS for the latest requested fresh rerun `20260802222723`. The historical `20260802212823` blocker remains recorded for traceability but is superseded by the backend-recovered full rerun. Per user instruction, no API-only, SQL status update, admin account, or delete workaround was used.

## Manual Obsolete Blocker Record

- BLOCKED but not current acceptance path: manual “作废当前版本” requires a published `bpm_business_approval_policy` for `DCC / DCC / CONTROLLED_FILE / OBSOLETE` in tenant `122`; current runtime has none.
- Data safety: no manual obsolete request was submitted, no obsolete approval/signature was fabricated, no DCC file was deleted, and no SQL/API status mutation was used.

## Long-Term Memory Update

- 2026-08-02：按用户要求将 DCC “作废/废止”口径写入长期经验：后续遇到 DCC 作废/废止必须先区分“手动作废审批 `OBSOLETE`”与“升版后旧版自动失效 `SUPERSEDED`”；用户明确说“不走审批、升版本老版本自动作废/失效”时，验收口径应为 V1 `SUPERSEDED`、V2 `ACTIVE`、master 指向 V2、受控浏览不再返回 V1 当前有效行。
- Updated: `docs/e2e-rules.md#dcc-文控审批处理入口门禁`。
- Updated: `docs/experience-index.md` DCC 文控审批关键词路由。
- 2026-08-02 20:15 +08:00：按 `project-experience-consolidation` 技能复查长期经验归宿；`docs/e2e-rules.md#dcc-文控审批处理入口门禁` 与 `docs/experience-index.md` 已覆盖“DCC 作废/废止按升版自动失效验收”的可复用规则，本次无需新建长期经验文档。
