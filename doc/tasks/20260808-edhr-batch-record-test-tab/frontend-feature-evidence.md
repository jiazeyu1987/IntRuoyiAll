# Frontend Feature Evidence: 批记录测试一线PQC与一线生产 Tabs

## Feature Goal And Non-Goals

- Goal: 在独立“批记录测试”页面保留内部 tab “一线PQC”，并在其后新增内部 tab “一线生产”；两个 tab 均用标准列表模板展示固定任务拆解，并复用行级 CODE_READONLY Codex 测试链路。
- Non-goal: 不修改正式一线PQC业务页面、正式一线生产业务页面、后端写接口、QA检验项目接口或PQC组长/生产组长管理业务逻辑。

## Requirements And Acceptance IDs

- REQ-FPQC-01: 页面包含“生产组长”“一线PQC”和“一线生产”三个内部 tab，且“一线生产”位于“一线PQC”之后。
- REQ-FPQC-02: “一线PQC”tab 使用 `UnifiedListTemplate`，table-key 为 `mes.pro.edhrBatchRecordTest.frontlinePqc`。
- REQ-FPQC-03: 固定展示 8 条一线PQC职责测试项：活跃订单池选择、按产品读取工艺路线、按工序加载QA检验项、检验项名称与方法、首检数量、巡检抽样数量、电子密码提交、提交进入PQC组长管理列表。
- REQ-FPQC-04: 每行“测试”按钮继续按 `项目=批记录` + 精确名称 upsert `CODE_READONLY` 测试项并调用 `startCodexTestExecution`。
- REQ-FPROD-01: “一线生产”tab 使用 `UnifiedListTemplate`，table-key 为 `mes.pro.edhrBatchRecordTest.frontlineProduction`。
- REQ-FPROD-02: 固定展示 8 条一线生产职责测试项：一线生产入口与组长身份、负责工序卡片来源、负责员工卡片来源、工序上下文数据联动、设备可选性、设备参数可选性、设备参数限制规则、电子密码与待分配报工。

## UI Entry Points And Owned Files

- Route: `/mes/pro/feedback/edhr-batch-test`。
- Component: `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchRecordTestPage.vue`。
- Static contract: `IntRuoyiFronted/tests/e2e/edhr-batch-record-test-tab-static.spec.cjs`。
- Task docs: `doc/tasks/20260808-edhr-batch-record-test-tab/`。

## API Contracts And Data States

- Reused APIs: `getCodexTestCasePage`, `createCodexTestCase`, `updateCodexTestCase`, `startCodexTestExecution`, `getCodexTestRunnerStatus`, `getTenantList`。
- No new backend API contract was introduced.
- Error behavior remains fail-fast via existing `showRequestError`; no mock success, fallback, or browser-side CLI spawn was added.

## BDD Scenarios

- BDD: 一线PQC职责测试列表 -> Given 用户打开“批记录测试”页面，When 点击内部“一线PQC”tab，Then 标准列表模板展示一线PQC 8 条职责测试描述。
- BDD: 一线PQC行级代码分析 -> Given 用户在“一线PQC”tab 点击某行“测试”，When 测试项不存在或已存在，Then 系统创建/更新 `批记录测试-一线PQC-xx-*` CODE_READONLY 测试项并启动受控 Runner 执行。
- BDD: 一线生产职责测试列表 -> Given 用户打开“批记录测试”页面，When 点击“一线PQC”后面的“一线生产”tab，Then 标准列表模板展示一线生产 8 条职责测试描述。
- BDD: 一线生产行级代码分析 -> Given 用户在“一线生产”tab 点击某行“测试”，When 测试项不存在或已存在，Then 系统创建/更新 `批记录测试-一线生产-xx-*` CODE_READONLY 测试项并启动受控 Runner 执行。

## RED And GREEN Evidence

- RED: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> FAIL, expected reason: 页面已存在“一线PQC”，但缺少用户要求放在其后的“一线生产/frontlineProduction”tab、table-key 和职责列表。
- GREEN: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> PASS。
- GREEN: `node --max-old-space-size=12288 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json --pretty false` -> PASS。
- GREEN: `node IntRuoyiFronted\tests\e2e\codex-runner-code-readonly-static.spec.cjs` -> PASS。
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_batch_record_test_menu_sql.py` -> PASS, 3 tests。
- GREEN: `git diff --check -- IntRuoyiFronted\src\views\mes\pro\edhr-batch\BatchRecordTestPage.vue IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs doc\tasks\20260808-edhr-batch-record-test-tab` -> PASS with CRLF warnings only.

## UX State Checks

- Responsive: No layout-specific CSS was changed; new tab reuses existing `UnifiedListTemplate` layout and current page shell.
- Accessibility: Existing Element Plus `el-tabs`, `el-table`, and permission-controlled `el-button` patterns are preserved.
- Loading: Runner status and tenant loading behavior are unchanged.
- Empty: Static lists are fixed data, so empty-state behavior is not introduced.
- Error: Tenant/Runner/upsert/start execution errors continue to surface through existing message handling.
- Permission: Row test button remains guarded by `v-hasPermi="['system:codex-test:execute']"`。

## E2E Or Component Verification Path

- Static contract verifies the route, independent page anchors, internal tabs, 一线PQC后置一线生产的 tab 顺序, standard list templates, both 8-row lists, row test button, CODE_READONLY upsert/start execution, and no frontend direct Codex CLI spawn.
- Real click execution was not performed in this verification pass to avoid creating/updating live Codex test executions unnecessarily; the existing page visibility real-path evidence remains in `verification-report.md`。

## Blockers And Follow-Up Skills

- Blockers: None for the requested 一线PQC保留和一线生产新增 implementation.
- Follow-up: If a real execution click is required, run it with confirmed tenant/admin/Runner/Codex CLI prerequisites and record the executionId.
