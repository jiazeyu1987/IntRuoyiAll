# Execution Log

## User Intent

- 用户要求在 `E:\IntRuoyi` 对 DCC 文控“文件作废/废止”进行真实 Playwright E2E 验证。
- 初始要求覆盖手动作废申请、审批/签名、作废生效、受控浏览最终效果和只读 API/DB 核验。
- 用户后续明确变更验收口径：`作废先不走审批, 文件升版本的时候老的版本自动作废, 走这条链路`。
- 当前任务按最新口径验证“升版发布后旧版本自动失效/SUPERSEDED”，不再把手动作废审批作为完成门禁。
- 2026-08-02 19:30:25 +08:00：用户要求“进行一次完整的修改之后的作废链路 E2E 验证”，本轮将跑全新任务自有文件的原版上传、四级审批/签名、升版发布、旧版自动失效、受控浏览和只读 DB 核验。

## Preconditions Read

- 2026-08-02：已读取 `AGENTS.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/frontend-development.md`、`docs/task-closeout-rules.md`。
- 2026-08-02：已补充读取 `docs/database-rules.md`、`docs/local-runtime.md`、`docs/powershell-encoding.md`。
- 2026-08-02：已读取 Playwright 技能 `C:\Users\BJB110\.codex\skills\playwright\SKILL.md`。
- 2026-08-02：`npx --version` 返回 `11.6.2`，满足 Playwright CLI 技能前置。

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

## Milestone Updates

- 2026-08-02：手动作废链路曾通过真实 Playwright 进入受控浏览、目标详情和“作废当前版本”弹窗，但 `/form-center/actions/resolve` 返回 `No published business approval policy matched action OBSOLETE`；该阻塞按原始口径记录，未用 API/SQL/admin 绕过。
- 2026-08-02：用户确认“作废先不走审批，文件升版本时老版本自动作废”，当前任务切换到升版自动失效链路。
- 2026-08-02：复用任务自有、已完整发布的升版链路数据 `CODX-DCC-REV-FULL-20260802-20260802091213`，不新建额外业务文件，不改状态。
- 2026-08-02：修正验证脚本对当前页面的等待锚点：受控浏览详情入口当前是 `traceability=1&from=browser`，详情页使用内嵌“版本历史”表，而非 viewer 弹窗。
- 2026-08-02：真实 Playwright 复验 PASS：非 admin `wangsiyu` 登录，受控浏览 `status=ACTIVE` 当前行是 V2，V1 不作为当前有效行返回；从受控浏览打开 V2 追溯详情，页面可见版本历史和升版原因。
- 2026-08-02：只读 DB 复验 PASS：V1 `SUPERSEDED`、V2 `ACTIVE`、master 当前有效版本 `2054545668044070272`、发布 BPM 和 DCC 电子签名证据完整。

## Commands / Evidence

- `node --check E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\dcc-revision-publish-real-e2e.cjs` -> PASS.
- Real Playwright command with password injected via `DCC_E2E_PASSWORD` PowerShell expression and `DCC_E2E_USE_EXISTING_CHAIN=1` -> PASS.
- Result JSON: `E:\IntRuoyi\doc\tasks\20260802-dcc-controlled-file-obsolete-e2e\revision-auto-obsolete-e2e-result.json`.
- Screenshots: `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\browser-current-v2.png`, `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\detail-version-history.png`.
- Source PASS evidence reused and revalidated: `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\e2e-result.json`.
- Secret scan: `rg -n "111111|admin123|password\s*=\s*['\"]|DCC_E2E_PASSWORD\s*=\s*['\"]" ...` -> PASS, no matches.

## Full Fresh Rerun 2026-08-02

- Intent: run full real Playwright chain without `DCC_E2E_USE_EXISTING_CHAIN`, creating a fresh task-owned DCC file and validating old-version automatic invalidation through revision publish.
- Planned result path: `E:\IntRuoyi\doc\tasks\20260802-dcc-controlled-file-obsolete-e2e\full-rerun-e2e-result.json`.

## Manual Obsolete Blocker Record

- BLOCKED but not current acceptance path: manual “作废当前版本” requires a published `bpm_business_approval_policy` for `DCC / DCC / CONTROLLED_FILE / OBSOLETE` in tenant `122`; current runtime has none.
- Data safety: no manual obsolete request was submitted, no obsolete approval/signature was fabricated, no DCC file was deleted, and no SQL/API status mutation was used.

## Long-Term Memory Update

- 2026-08-02：按用户要求将 DCC “作废/废止”口径写入长期经验：后续遇到 DCC 作废/废止必须先区分“手动作废审批 `OBSOLETE`”与“升版后旧版自动失效 `SUPERSEDED`”；用户明确说“不走审批、升版本老版本自动作废/失效”时，验收口径应为 V1 `SUPERSEDED`、V2 `ACTIVE`、master 指向 V2、受控浏览不再返回 V1 当前有效行。
- Updated: `docs/e2e-rules.md#dcc-文控审批处理入口门禁`。
- Updated: `docs/experience-index.md` DCC 文控审批关键词路由。
