# Execution Log

## User Intent

- 用户要求新增一个 QA 页签，用于设置 PQC 执行的 QA 规则，并结合最开始的压力泵 PDF 内容。
- 用户明确纠正：QA 是给 PQC 制定规则的，与 DCC 没有任何关系。
- 用户进一步要求：QA 用一个单独的页签显示，不放在生产/PQC 工作台内部 tab 里。
- 用户基于截图进一步要求：在 eDHR 左侧菜单 `批记录表单` 和 `批次执行` 之间增加独立 `QA` 菜单项，点击进入 QA 设置，并且本机 `芋道源码/admin` 可以看到该菜单。

## BDD Scenarios

- BDD: QA 配置过程检验规程 -> Given QA 打开 `/mes/pro/process-pool/qa-regulation` When 独立 QA 页面加载 Then 页面展示规程元数据、适用范围、首检/巡检/末检规则和检验项目配置能力。
- BDD: 压力泵 PDF 初始化 -> Given QA 查看压力泵过程检验规程示例 When 页面加载 Then 能看到 `PQC-IDI-001`、`B/0`、`2026-01-04`、`按压式球囊扩充压力泵组装过程检验规程` 等 PDF 来源信息。
- BDD: QA/PQC 边界 -> Given PQC 只执行 QA 发布规则 When QA 独立页面展示配置能力 Then 页面不出现 DCC 文件分类、受控文件上传或文控审批语义。
- BDD: 发布完整性检查 -> Given QA 规程尚未正式接入发布接口 When 查看独立页面 Then 页面提示发布前必须完成范围、项目、抽样规则、判定标准和版本冻结检查，不伪造保存成功。
- BDD: 检验项目原文依据 -> Given QA 查看某条解析后的检验项目 When QA 对照判定标准 Then 页面展示该项目相关的 PDF 页码、项目名、接受标准原文摘录和检验方法原文摘录，而不是整页 OCR 或无来源说明。
- BDD: QA 独立页面入口 -> Given QA 需要单独工作入口 When QA 打开 `QA 规程配置` Then 页面通过独立路由展示 QA 规则配置，生产/PQC 工作台内部不再存在 `QA 规程` tab。
- BDD: eDHR 左侧菜单显示 QA -> Given `芋道源码/admin` 打开 eDHR 左侧菜单 When `批记录表单` 和 `批次执行` 已显示 Then `QA` 位于二者之间，点击后进入 `/mes/pro/process-pool/qa-regulation`。

## TDD Sequence

- TDD-01 RED: Add `role-matrix-qa-regulation-tab-static.spec.cjs` before implementation. Expected failure: workbench lacks `QA 规程` tab and QA selectors.
- TDD-02 GREEN: Add the QA tab, pressure-pump source metadata, editable scope/rule/item sections, completeness checks, and PQC task preview in `TeamLeaderWorkbenchPage.vue`.
- TDD-03 REGRESSION: Run existing QA regulation static contract to ensure backend schema/dynamic item contract remains intact.
- TDD-04 REGRESSION: Run existing PQC dynamic form static contract to ensure PQC still renders from regulation/task data instead of fixed demo items.
- TDD-05 REVIEW: Validate frontend evidence and document review checklist before closeout.
- TDD-06 RED: Extend `role-matrix-qa-regulation-tab-static.spec.cjs` to require item-level original-source fields and visible excerpt UI.
- TDD-07 GREEN: Add source page/item/excerpt/method fields to QA draft items and render them in the inspection-item table.
- TDD-08 RED: Update `role-matrix-qa-regulation-tab-static.spec.cjs` to require standalone `QaRegulationPage.vue` and route `/mes/pro/process-pool/qa-regulation`, and to forbid `QA 规程` in `TeamLeaderWorkbenchPage.vue` internal tabs.
- TDD-09 GREEN: Move QA UI/state to the standalone page, remove QA tab residue from the workbench, and update real E2E target path.
- TDD-10 RED: Add `test_mes_edhr_qa_menu_sql.py` before the menu migration. Expected failure: `20260804_mes_edhr_qa_menu.sql` is missing, so no dynamic `QA` eDHR menu exists.
- TDD-11 GREEN: Add the QA dynamic menu SQL, package/role binding, and static frontend/backend contract so `QA` appears between `批记录表单` and `批次执行`.
- TDD-12 E2E: Apply the menu migration to local runtime data, login as `芋道源码/admin`, click the eDHR `QA` menu item, and verify the standalone QA page opens.

## Command Log

- Read `docs/task-closeout-rules.md`, `docs/frontend-development.md`, `docs/powershell-encoding.md`, frontend feature skill and PDF skill.
- Read `docs/experience-index.md`; applied static-contract isolation and evidence-retention gates.
- Rendered the pressure-pump PDF first page from an ASCII temp copy for visual inspection; text extraction was empty, consistent with scanned PDF content.
- Added `IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`.
- Updated `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`.
- Reworked task documents into BDD/TDD structure with scenario matrix, strict TDD sequence, test data, user path plan, and document review checklist.
- Continued work in `D:\IntRuoyiWorktree\2020804_qa` on branch `codex/2020804_qa`.
- Removed task-owned temporary patch file `qa-tab-source.patch` after applying the QA tab change in the worktree.
- Ran `pnpm --dir IntRuoyiFronted install --frozen-lockfile` to satisfy the worktree-local `node_modules` prerequisite before type checking.
- Re-ran worktree runtime slot precheck; slot reservation is blocked because all `int_main` slots `1..19` are occupied.
- Ran `project-experience-consolidation`; merged the worktree dependency lesson into the existing `docs/worktree-memory.md` section `Worktree 前端依赖启动门禁`.
- Ran commit/push preflight `scripts\preflight\branch-runtime-port-guard.ps1`; it is blocked because this worktree has no registered port slot.
- Synchronized the latest QA task documents and worktree-memory evidence into the `E:\IntRuoyi` `int_main` working tree. Source comparison showed the QA page file matched the worktree ignoring EOL-only differences.
- Read E2E, login, local runtime, worktree, PowerShell encoding, task closeout, Playwright, and QA evidence gates before local browser validation.
- Ran local runtime prechecks: `8081` listening, `48081` listening, `http://127.0.0.1:8081/` returned HTTP 200, `http://127.0.0.1:48081/actuator/health` returned `UP`, and `require('playwright')` loaded Chromium.
- Ran local Chromium E2E against `http://127.0.0.1:8081/mes/pro/process-pool/team-leader` before the standalone split; first attempt reached the former QA tab but used a text locator for the newly added item even though the value is inside an input, so the script was corrected to assert input value.
- Captured local E2E screenshot at `doc/tasks/20260804-qa-regulation-tab/qa-regulation-live-e2e.png`.
- For the source-excerpt request, read frontend/PDF/task/encoding gates, copied the scanned PDF to `tmp/pdfs/qa-pressure-pump/pressure-pump.pdf`, rendered pages with Poppler `pdftoppm.exe`, and visually inspected pages 2-8 because direct PDF text extraction returned zero text.
- Identified item-specific excerpts from rendered pages: page 3 cleaning/assembly/no-pressure-drop; page 4 light-cured cover appearance/firmness; page 5 piston assembly; page 6 pump exterior/fit/no-card/firmness; page 7 air tightness negative/high/low pressure and sampling notes; page 8 judgment rule and record reference.
- Added `原文依据` column to the QA inspection item table with item-specific PDF page, original item name, acceptance-standard excerpt, and inspection-method excerpt.
- Re-ran `E:\IntRuoyi` local startup with `IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full`; Maven package succeeded, frontend `8081` returned HTTP 200, but backend `48081` failed before health.
- Read latest backend startup logs from `output\runtime\int_main\backend-runtime-control-20260804-153949.out.log`; startup failed in non-QA MES route flow config code: `@Resource annotation requires a single-arg method` on `MesProRouteFlowConfigServiceImpl.getRouteFlowProcessConfigList(Long,String)`.
- Did not run browser login/path assertions after the backend health gate failed; no mock login, API-only substitute, random port, or frontend-only success was used.
- Rechecked local runtime on request to perform E2E again; `http://127.0.0.1:48081/actuator/health` returned `UP` and `http://127.0.0.1:8081/` returned HTTP 200.
- Added task-owned real E2E script `IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-original-excerpt-real.e2e.cjs` to verify the `原文依据` column through a real login and page path.
- Ran the script against local `8081/48081` before the standalone split; it logged in with the local default `芋道源码/admin` identity, opened `/mes/pro/process-pool/team-leader`, selected the former `QA 规程` tab, verified item-level source excerpts, asserted no DCC terms in the QA panel, and asserted no backend write requests.
- Updated the QA implementation to a standalone route page: `QaRegulationPage.vue` owns `data-qa-regulation-page`, `remaining.ts` registers `/mes/pro/process-pool/qa-regulation`, `TeamLeaderWorkbenchPage.vue` keeps only `生产组长` and `PQC 组长` internal tabs, and the real E2E script now opens the standalone route directly.
- Started the follow-up dynamic menu slice after the screenshot clarification: re-read frontend/database/login/E2E/local runtime/task rules, confirmed the current QA page already exists as a standalone route, and added the focused RED SQL contract `IntRuoyiBackend/script/tests/test_mes_edhr_qa_menu_sql.py`.
- Re-read frontend/database/QA evidence skill contracts and local runtime/login/worktree/branch port rules for the eDHR dynamic menu follow-up.
- Inspected `20260804_mes_edhr_qa_menu.sql`, the SQL contract, route-menu static contract, real menu-click E2E script, and `remaining.ts` route wiring.
- Checked local runtime before real menu-click E2E: frontend `http://127.0.0.1:8081/` returned HTTP `200`; backend `http://127.0.0.1:48081/actuator/health` refused connection and no process was listening on `48081`.
- Identified unrelated active `E:\IntRuoyi` backend restart/Maven processes for `doc\tasks\20260804-dcc-approval-upload-view` and an MES route test; did not stop them because they are not task-owned.
- Re-ran focused menu verification contracts and local DB menu-order query after the runtime preflight.

## Verification Evidence

- RED: `node IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL, expected reason: existing page did not expose `QA 规程` tab.
- GREEN: `node IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted run e2e:role-matrix-qa-regulation:static` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted run e2e:role-matrix-pqc-dynamic-form:static` -> PASS.
- RED: `pnpm --dir IntRuoyiFronted ts:check` -> FAIL, expected reason: fresh worktree lacked `node_modules`, so `cross-env` was not found.
- GREEN: `pnpm --dir IntRuoyiFronted install --frozen-lockfile` -> PASS, dependencies installed from the worktree lock file.
- GREEN: `pnpm --dir IntRuoyiFronted ts:check` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-qa-regulation-tab/frontend-feature-evidence.md` -> PASS, `Frontend feature evidence is valid.`
- BLOCKED: `powershell -ExecutionPolicy Bypass -File scripts\runtime\reserve-worktree-slot.ps1 -Name 2020804_qa -Path D:\IntRuoyiWorktree\2020804_qa -Branch codex/2020804_qa -Profile int_main -AsJson` -> FAIL, `No available runtime slot for profile 'int_main' in range 1..19.`
- BLOCKED: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> FAIL, `No worktree port registry entry is registered for 'D:\IntRuoyiWorktree\2020804_qa'.`
- REVIEW: `python -X utf8 -c "<BDD/TDD document structure check>"` -> PASS, `BDD/TDD document review OK`.
- REVIEW: `git diff --check -- doc\tasks\20260804-qa-regulation-tab\*.md` -> PASS.
- REVIEW: UTF-8 read check for all task Markdown files -> PASS.
- REVIEW: project experience consolidation -> PASS, updated existing `docs/worktree-memory.md`; no new long-term document required.
- GREEN: `E:\IntRuoyi` `node IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS.
- GREEN: `E:\IntRuoyi` `pnpm --dir IntRuoyiFronted run e2e:role-matrix-qa-regulation:static` -> PASS.
- GREEN: `E:\IntRuoyi` `pnpm --dir IntRuoyiFronted run e2e:role-matrix-pqc-dynamic-form:static` -> PASS.
- GREEN: `E:\IntRuoyi` `pnpm --dir IntRuoyiFronted run ts:check` -> PASS.
- GREEN: `E:\IntRuoyi` `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-qa-regulation-tab/frontend-feature-evidence.md` -> PASS.
- RETRY: local Chromium E2E first run -> FAIL, expected reason: validation script looked for `QA-ITEM-06` as visible text, but the UI renders item codes as input values.
- GREEN: local Chromium E2E on `http://127.0.0.1:8081/mes/pro/process-pool/team-leader` before the standalone split -> PASS, assertions covered source/scope/rules/items/completeness/PQC preview, `PQC-IDI-001`, `B/0`, `2026-01-04`, pressure-pump title, `过程检验规程`, `首检/上午巡检/下午巡检/末检`, API-not-wired blocker, local draft item value `QA-ITEM-06`, draft preview message, publish precheck message, no DCC/file-classification/controlled-file/document-control terms in the QA panel, no backend write requests, `consoleErrorCount=0`, `pageErrorCount=0`.
- GREEN: `node IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS after local browser E2E.
- GREEN: `pnpm --dir IntRuoyiFronted run e2e:role-matrix-qa-regulation:static` -> PASS after local browser E2E.
- RED: `node IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL after extending the contract for original-source fields/UI, expected reason: QA item rows did not yet expose `data-qa-regulation-original-excerpt`, `sourceOriginalPage`, `sourceOriginalItem`, `sourceOriginalExcerpt`, and `sourceOriginalMethod`.
- GREEN: `E:\IntRuoyi\IntRuoyiFronted` `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS after adding original-source excerpts.
- GREEN: `E:\IntRuoyi\IntRuoyiFronted` `pnpm run e2e:role-matrix-qa-regulation:static` -> PASS after adding original-source excerpts.
- GREEN: `E:\IntRuoyi\IntRuoyiFronted` `pnpm run e2e:role-matrix-pqc-dynamic-form:static` -> PASS after adding original-source excerpts.
- GREEN: `E:\IntRuoyi\IntRuoyiFronted` `pnpm run ts:check` -> PASS after adding original-source excerpts.
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-qa-regulation-tab/frontend-feature-evidence.md` -> PASS after adding original-source excerpts.
- REVIEW: `git diff --check -- IntRuoyiFronted\src\views\mes\pro\processpool\TeamLeaderWorkbenchPage.vue IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs doc\tasks\20260804-qa-regulation-tab` -> PASS.
- REVIEW: UTF-8 read check for task Markdown files -> PASS.
- GREEN: `E:\IntRuoyi\IntRuoyiFronted` `node --check tests\e2e\role-matrix-qa-regulation-original-excerpt-real.e2e.cjs` -> PASS.
- GREEN: `E:\IntRuoyi\IntRuoyiFronted` `node tests\e2e\role-matrix-qa-regulation-original-excerpt-real.e2e.cjs` -> PASS; result `sourceExcerptCount=5`, `writeRequests=[]`, `consoleErrors=[]`, `pageErrors=[]`.
- GREEN: 2026-08-04 follow-up E2E rerun `E:\IntRuoyi\IntRuoyiFronted` `node tests\e2e\role-matrix-qa-regulation-original-excerpt-real.e2e.cjs` -> PASS; result `sourceExcerptCount=5`, `writeRequests=[]`, `consoleErrors=[]`, `pageErrors=[]`.
- GREEN: standalone route static contract -> PASS; route `/mes/pro/process-pool/qa-regulation` loads `QaRegulationPage.vue`, and the workbench no longer contains a `QA 规程` internal tab.
- RED: `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_mes_edhr_qa_menu_sql.py -q` -> FAIL, expected reason: `missing eDHR QA menu SQL migration` because `20260804_mes_edhr_qa_menu.sql` is not present yet.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py -q` -> PASS, `3 passed`.
- GREEN: `node IntRuoyiFronted\tests\e2e\mes-edhr-qa-menu-static.spec.js` -> PASS, `PASS eDHR QA dynamic menu static contract`.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\mes-edhr-qa-menu-real.e2e.js` -> PASS.
- GREEN: `node IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS, `PASS role-matrix QA regulation standalone page static contract`.
- GREEN: local Docker MySQL query -> `900365 批记录表单 sort=0`, `900434 QA sort=1`, `900033 批次执行 sort=2`, `admin_role_bindings=3`, `tenant_package_bindings=2`.
- GREEN: database schema evidence validator -> PASS, `Database schema evidence is valid.`
- GREEN: frontend feature evidence validator -> PASS, `Frontend feature evidence is valid.`
- REVIEW: latest UTF-8 read check for task Markdown files -> PASS.
- REVIEW: latest `git diff --check` for QA menu source/test/task docs -> PASS.
- RETRY: real local menu-click E2E preflight initially found backend `48081` refused connection while frontend `8081` was healthy; no frontend-only or API-only substitute was used.
- RETRY: `node IntRuoyiFronted\tests\e2e\mes-edhr-qa-menu-real.e2e.js` first exposed hidden/collapsed eDHR menu timing and expansion behavior; the E2E script was corrected to wait for menu mount, expand the visible eDHR parent with browser interactions, and verify child order before clicking `QA`.
- GREEN: `node IntRuoyiFronted\tests\e2e\mes-edhr-qa-menu-real.e2e.js` -> PASS; actor `芋道源码/admin`, menu order `批记录表单 -> QA -> 批次执行`, target path `/mes/pro/process-pool/qa-regulation`, `writeRequests=[]`, `consoleErrors=[]`, `pageErrors=[]`.
- EVIDENCE: `output/playwright/20260804-qa-regulation-tab/edhr-qa-menu-real-e2e.json`.
- EVIDENCE: `output/playwright/20260804-qa-regulation-tab/edhr-qa-menu-real-e2e.png`.
- GREEN: latest focused standalone contract rerun `E:\IntRuoyi\IntRuoyiFronted` `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS.
- GREEN: latest adjacent PQC static regression `E:\IntRuoyi\IntRuoyiFronted` `pnpm run e2e:role-matrix-pqc-dynamic-form:static` -> PASS.
- GREEN: latest Vue type check `E:\IntRuoyi\IntRuoyiFronted` `pnpm run ts:check` -> PASS.
- BLOCKED: latest broader QA regulation static regression `E:\IntRuoyi\IntRuoyiFronted` `pnpm run e2e:role-matrix-qa-regulation:static` -> FAIL in pre-existing M6 SQL fixture assertion: `M6 QA/PQC formal fixture must freeze the task-owned PQC task ids before resetting them to PENDING`.
- RETRY: latest standalone real E2E first run `E:\IntRuoyi\IntRuoyiFronted` `node tests\e2e\role-matrix-qa-regulation-original-excerpt-real.e2e.cjs` -> FAIL on login `domcontentloaded` timeout during Vite warm-up; direct HTTP and Playwright route probes then confirmed the login route loaded.
- GREEN: latest standalone real E2E rerun `E:\IntRuoyi\IntRuoyiFronted` `node tests\e2e\role-matrix-qa-regulation-original-excerpt-real.e2e.cjs` -> PASS; opened `/mes/pro/process-pool/qa-regulation`, verified `sourceExcerptCount=5`, `writeRequests=[]`, `consoleErrors=[]`, `pageErrors=[]`.
- EVIDENCE: `output/playwright/20260804-qa-regulation-tab/qa-regulation-original-excerpt-real-e2e.json`.
- EVIDENCE: `output/playwright/20260804-qa-regulation-tab/qa-regulation-original-excerpt-real-e2e.png`.

## Document Review

- PASS: Task document includes goal, milestones, expected verification, current status, and design constraint checks.
- PASS: BDD scenarios use observable Given / When / Then behavior and cover QA rule editing, PDF source metadata, QA/PQC boundary, and publish precheck behavior.
- PASS: TDD sequence records RED expected failure, GREEN implementation target, regression commands, and evidence validation.
- PASS: No document states that QA is related to DCC; DCC is mentioned only as an explicit non-goal/boundary.
- PASS: Missing formal save/publish API is documented as a visible UI blocker, not a mock success or fallback.
- PASS: Document review command confirmed required sections across `task.md`, `execution-log.md`, `frontend-feature-evidence.md`, and `verification-report.md`.

## Blockers

- `pnpm --dir IntRuoyiFronted test <target>` wrapper does not register the two role-matrix targets; package scripts were verified with `pnpm run`.
- Worktree runtime/browser E2E was not started in `D:\IntRuoyiWorktree\2020804_qa` because `reserve-worktree-slot.ps1` reported no available `int_main` slot in range `1..19`; no random port or fallback runtime was used. Local `E:\IntRuoyi` browser E2E on fixed `int_main` ports `8081/48081` passed.
- Commit/push closeout was not performed because the mandatory branch runtime port guard failed without a worktree registry entry. The registry entry could not be created because all `int_main` slots `1..19` are occupied.
- `E:\IntRuoyi` remains dirty with unrelated DCC/NAS changes and branch divergence `int_main...origin/int_main [ahead 3, behind 2]`; QA changes are present in the int_main working tree, but a formal commit/push still requires resolving that broader repository state.
