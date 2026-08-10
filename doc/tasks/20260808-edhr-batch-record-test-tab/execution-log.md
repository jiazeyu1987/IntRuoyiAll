# Execution Log: eDHR 批记录测试页签与生产组长代码分析

## User Intent

用户要求按既定计划实现：在 eDHR 批记录能力中新增“批记录测试”入口，内含“生产组长”tab 和 5 条职责描述，行级“测试”按钮自动创建/更新 Codex 测试项，并通过 Codex CLI 做只读代码分析。用户随后明确修正：当前不能做成“批次执行”里的一个 tab，必须是类似“PQC组长”的独立页签/菜单页。

用户新增要求：将一线PQC任务拆解后加入“批记录测试”独立页，在内部新增“一线PQC”tab，使用标准列表模板逐行展示职责描述，每行继续提供“测试”按钮触发只读代码分析。任务范围仅新增批记录测试页的测试分类，不替代正式一线PQC业务页。

用户最新纠正：保留“一线PQC”内部 tab，并在它后面新增“一线生产”内部 tab；一线生产 tab 承载生产组长登录一线生产后的工序、员工、不良、设备、设备参数、电子密码和待分配报工任务拆解。

## BDD

- BDD: 批记录测试页签展示生产组长职责 -> Given 用户进入 eDHR 批记录页签，When 点击“批记录测试”，Then 能看到“生产组长”内部 tab 和 5 条职责列表。
- BDD: 缺失测试项时自动创建并执行 -> Given 用户点击某行“测试”且对应测试项不存在，When 页面提交测试请求，Then 系统创建 `批记录测试-生产组长-xx-*` 测试项并启动执行批次。
- BDD: 已存在测试项时更新并执行 -> Given 对应测试项已存在，When 用户再次点击“测试”，Then 系统按最新职责描述更新测试项后启动执行，且不产生重复项。
- BDD: Runner 执行只读代码分析 -> Given Runner 领取 `CODE_READONLY` 测试项，When Codex CLI 运行，Then 只读分析代码是否满足职责描述，并把 PASS/FAIL/BLOCKED 写回检查点。
- BDD: admin 可见批记录测试入口 -> Given 用户使用芋道源码/admin 进入 eDHR 菜单，When eDHR 可见菜单加载完成，Then 能看到“批记录测试”入口并打开生产组长测试页。
- BDD: 批记录测试为独立菜单页 -> Given 用户进入 eDHR 菜单，When 查看“批记录测试”和“批次执行”，Then “批记录测试”像“PQC组长”一样作为独立菜单入口存在，不出现在“批次执行”顶部页签内。
- BDD: 一线PQC任务测试列表 -> Given 用户打开“批记录测试”页面，When 点击内部“一线PQC”tab，Then 标准列表模板展示活跃订单池选择、按产品读取工艺路线、按工序加载QA检验项、检验项名称与方法、首检数量、巡检抽样数量、电子密码提交和提交进入PQC组长管理列表 8 条测试描述。
- BDD: 一线PQC行级代码分析 -> Given 用户在“一线PQC”tab 点击某行“测试”，When 对应测试项不存在或已存在，Then 系统按 `项目=批记录` + 精确名称自动创建/更新 `CODE_READONLY` 测试项并启动受控 Runner 执行。
- BDD: 一线生产任务测试列表 -> Given 用户打开“批记录测试”页面，When 点击“一线PQC”后面的“一线生产”tab，Then 标准列表模板展示一线生产 8 条任务描述。
- BDD: 一线生产行级代码分析 -> Given 用户在“一线生产”tab 点击某行“测试”，When 对应测试项不存在或已存在，Then 系统按 `项目=批记录` + 精确名称自动创建/更新 `CODE_READONLY` 测试项并启动受控 Runner 执行。

## Evidence

- Skills loaded: `frontend-feature-delivery`, `backend-api-delivery`, `database-schema-delivery`.
- Trigger rules loaded: `docs/frontend-development.md`, `docs/backend-development.md`, `docs/database-rules.md`, `docs/powershell-encoding.md`, `docs/task-closeout-rules.md`.
- Experience gates applied: 统一列表复合工具栏布局门禁、Codex Runner 自动测试门禁、测试管理测试节点闭环门禁、测试管理 schema 迁移门禁、前端多布局模式真实页面门禁、用户可见描述与内部编码隔离门禁。

## Commands

- RED: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> FAIL, expected reason: 批记录测试页面、路由和生产组长职责列表尚不存在。
- RED: `node IntRuoyiFronted\tests\e2e\codex-runner-code-readonly-static.spec.cjs` -> FAIL, expected reason: Runner 尚无显式 `CODE_READONLY` 模式和只读代码分析 prompt。
- GREEN: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> PASS.
- GREEN: `node IntRuoyiFronted\tests\e2e\codex-runner-code-readonly-static.spec.cjs` -> PASS.
- GREEN: `node IntRuoyiFronted\tests\e2e\system-codex-test-management-static.spec.js` -> PASS.
- GREEN: `node IntRuoyiFronted\tests\e2e\edhr-batch-history-static.spec.js` -> PASS.
- GREEN: `node IntRuoyiFronted\tests\e2e\edhr-trace-visual-record-detail-static.spec.js` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted ts:check` -> PASS.
- GREEN: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestCaseServiceImplTest,CodexTestExecutionServiceImplTest,CodexTestRunnerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 37 tests, BUILD SUCCESS.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_codex_test_analysis_mode_migration.py` -> PASS, 2 tests.
- GREEN: `git diff --check` -> PASS, exit code 0；仅输出工作区既有 CRLF 警告。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-edhr-batch-record-test-tab --mode preview` -> PASS, no delete, no blocked, no warnings.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-edhr-batch-record-test-tab --mode apply` -> PASS, no deleted paths.
- GREEN: `rg "analysisMode|CODE_READONLY|代码只读分析|不要打开浏览器作为优先路径" docs\experience-index.md docs\e2e-rules.md -n` -> PASS, experience index routes new keywords to the Codex Runner gate.
- RED: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> FAIL, expected reason: 缺少 admin 可见菜单迁移 `20260808_mes_edhr_batch_record_test_menu.sql`，芋道源码/admin 从菜单进入看不到“批记录测试”。
- GREEN: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> PASS after adding visible menu migration assertions and SQL migration.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_batch_record_test_menu_sql.py` -> PASS, 3 tests.
- GREEN: `node IntRuoyiFronted\tests\e2e\mes-edhr-qa-menu-static.spec.js` -> PASS.
- RED: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> FAIL, expected reason: “批记录测试”仍存在于 `EdhrBatchRecordTabs.vue` 的批次执行顶部页签内，不符合类似“PQC组长”的独立页签口径。
- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_batch_record_test_menu_sql.py` -> FAIL, expected reason: 菜单 SQL 合同仍要求 `批次执行` sort=6、`批记录测试` sort=7，未体现独立入口排在批次执行前。
- GREEN: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> PASS after removing the batch-execution internal tab contract and locking independent page anchors.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_batch_record_test_menu_sql.py` -> PASS, 3 tests after sort contract update.
- GREEN: `node IntRuoyiFronted\tests\e2e\mes-edhr-qa-menu-static.spec.js` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted ts:check` -> PASS, exit code 0.
- GREEN: `git diff --check -- <touched files>` -> PASS; only existing CRLF warning reported for the SQL file.
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260808-edhr-batch-record-test-tab/bug-regression-evidence.md` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-edhr-batch-record-test-tab --mode preview` -> PASS, no delete, no blocked, no warnings.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-edhr-batch-record-test-tab --mode apply` -> PASS, no deleted paths.
- Runtime diagnosis: `Invoke-WebRequest http://127.0.0.1:48081/actuator/health` initially -> FAIL, connection refused; frontend 8081 was running from `E:\IntRuoyi\IntRuoyiFronted`.
- Runtime recovery: confirmed Docker MySQL/Redis running, existing 48081 backend process `62116` from `E:\IntRuoyi\output\runtime\int_main\backend-latest-...` became healthy; task-owned failed startup process `45728` was stopped with Ctrl+C.
- Runtime permission proof: Node login probe for `芋道源码/admin` -> PASS, `get-permission-info` returned menu `900440 批记录测试`, path `/mes/pro/feedback/edhr-batch-test`, component `mes/pro/edhr-batch/BatchRecordTestPage`, chain `MES 系统 > eDHR批记录 > 批记录测试`.
- Real Path GREEN: `node doc\tasks\20260808-edhr-batch-record-test-tab\verify-batch-record-test-visible.cjs` -> PASS, URL `/mes/pro/feedback/edhr-batch-test`, title visible, `生产组长` visible, page anchor count `1`, internal batch-execution tab count `0`, side menu includes `批记录测试`, `consoleErrors=[]`, `pageErrors=[]`.
- Experience consolidation: added `docs/frontend-development.md#动态菜单真实可见性缓存门禁` and routed `roleRouters` / fresh 登录 / admin 看不到新菜单 keywords from `docs/experience-index.md`.
- RED: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> FAIL, expected reason: current page lacks the requested `一线PQC` internal tab, standard list table key, 8 frontline pqc rows, and row-level CODE_READONLY upsert/start execution contract.
- RED: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> FAIL, expected reason: static contract still used the old `一线PQC/frontlinePqc` scope; updated the contract to the user-confirmed `一线PQC/frontlinePqc` scope before GREEN.

- RED: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> FAIL, expected reason: 页面仍是旧 `一线PQC/frontlinePqc` 口径，缺少用户要求的 `一线PQC/frontlinePqc` tab、table-key 和职责列表。
- GREEN: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> PASS after replacing with 一线PQC tab, table-key and 8 fixed rows.
- SUPERSEDED: earlier negative scan for `frontlinePqc|一线PQC` was part of a transient misread and is no longer a completion criterion; the current accepted scope keeps 一线PQC and adds 一线生产 after it.
- GREEN: `pnpm --dir IntRuoyiFronted ts:check` -> PASS, exit code 0.
- GREEN: `node IntRuoyiFronted\tests\e2e\codex-runner-code-readonly-static.spec.cjs` -> PASS.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_batch_record_test_menu_sql.py` -> PASS, 3 tests.
- GREEN: `git diff --check -- <touched files>` -> PASS; only CRLF warnings for edited frontend files.
- RED: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> FAIL, expected reason: 页面仍保留旧 `一线PQC/frontlinePqc` 模板标识和旧 QA 检验任务内容，不符合用户最新“一线PQC”任务拆解。
- SUPERSEDED: earlier negative scan for `frontlinePqc|一线PQC` was corrected by the user; current implementation intentionally keeps 一线PQC and separately adds 一线生产.
- GREEN: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> PASS after locking 一线PQC tab、table-key、8 行职责、旧一线PQC负向断言和 CODE_READONLY upsert/start execution。
- GREEN: `node --max-old-space-size=12288 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json --pretty false` from `IntRuoyiFronted` -> PASS, exit code 0.
- GREEN: `git diff --check -- IntRuoyiFronted\src\views\mes\pro\edhr-batch\BatchRecordTestPage.vue IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs doc\tasks\20260808-edhr-batch-record-test-tab\task.md doc\tasks\20260808-edhr-batch-record-test-tab\execution-log.md doc\tasks\20260808-edhr-batch-record-test-tab\verification-report.md` -> PASS, only CRLF warnings for edited frontend files.

## Implementation Notes

- 新增 `analysisMode` / `analysisModeSnapshot` 契约，后端保存、默认值、非法值拒绝、执行快照和 Runner claim 均覆盖 `PLAYWRIGHT_E2E` 与 `CODE_READONLY`。
- `CODE_READONLY` Runner prompt 明确只读扫描代码、路由、API、测试，不允许修改文件、运行写入命令、写业务数据或返回默认成功，并继续要求 `checkpointResults` JSON 回写。
- `BatchRecordTestPage.vue` 作为独立菜单页展示页面标题和稳定 `data-edhr-batch-record-test-page` 锚点，不再渲染 `EdhrBatchRecordTabs active-tab="test"`；页面内部按顺序提供“生产组长”“一线PQC”“一线生产”tab，三者均使用 `UnifiedListTemplate`、固定职责/任务行和行级“测试”按钮；一线生产行覆盖生产组长账号进入、负责工序卡片、负责员工卡片、工序上下文数据联动、设备可选性、设备参数可选性、设备参数限制规则、所选员工电子密码和报工管理待分配，测试动作按 `项目=批记录` + 精确名称 upsert 后启动执行。
- `20260808_mes_edhr_batch_record_test_menu.sql` 新增 admin 可见菜单 900440，路径 `/mes/pro/feedback/edhr-batch-test`，组件 `mes/pro/edhr-batch/BatchRecordTestPage`，并同步租户套餐与 admin 角色菜单绑定；菜单排序锁定为 `批记录测试` sort=6、`批次执行` sort=7。

## Real Path Status

- PASS: 本机前端 `8081`、后端 `48081`、芋道源码/admin 权限、动态侧边栏菜单和目标页面 fresh 登录路径均已验证；`批记录测试` 是类似 `PQC组长` 的独立菜单入口。
- Note: 若既有浏览器会话仍看不到，根因是前端会话内菜单缓存未重建；刷新页面或退出后重新登录即可让 `roleRouters` 从最新权限响应重建。

## Closeout

- Cleanup preview/apply completed with no delete candidates, no blockers, and no warnings.
- Project experience consolidated into `docs\e2e-rules.md#codex-runner-自动测试门禁` and `docs\experience-index.md` for `analysisMode=CODE_READONLY` code-readonly Runner tests.
- Bug regression evidence recorded in `doc\tasks\20260808-edhr-batch-record-test-tab\bug-regression-evidence.md`.


## Final Correction Verification 2026-08-08

- GREEN: `rg -n "activeInnerTab|frontlinePqc|frontlineProduction|生产组长|一线PQC|一线生产|UnifiedListTemplate" IntRuoyiFronted\src\views\mes\pro\edhr-batch\BatchRecordTestPage.vue` -> PASS, confirms tab order `生产组长` -> `一线PQC` -> `一线生产`.
- GREEN: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> PASS after locking 一线PQC tab, 一线生产 tab, table keys, both fixed 8-row lists, and CODE_READONLY upsert/start execution.
- GREEN: `node --max-old-space-size=12288 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json --pretty false` from `IntRuoyiFronted` -> PASS, exit code 0.
- GREEN: `node IntRuoyiFronted\tests\e2e\codex-runner-code-readonly-static.spec.cjs` -> PASS.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_batch_record_test_menu_sql.py` -> PASS, 3 tests.
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260808-edhr-batch-record-test-tab\frontend-feature-evidence.md` -> PASS.
- GREEN: `git diff --check -- <touched files>` -> PASS, only CRLF warnings for edited frontend files.
- Experience consolidation check: existing `docs/frontend-development.md#前端静态契约隔离门禁` already covers adjacent template/tab additions and preservation; no new long-term experience document was created.
