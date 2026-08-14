# Task: eDHR 批记录测试页签与生产组长代码分析

## Task Goal

在 eDHR 菜单中新增类似 “PQC组长” 的独立“批记录测试”入口，提供“生产组长”“一线PQC”和“一线生产”内部 tab；每行可自动创建/更新 Codex 测试项，并通过受控 Runner 调用 Codex CLI 进行只读代码分析。

## Milestones

- [x] M1：记录 BDD、适用经验门禁和 RED 失败证据。
- [x] M2：扩展 Codex 测试项数据契约，支持 `analysisMode=CODE_READONLY`。
- [x] M3：扩展 Runner，只读代码分析模式不走 Playwright E2E prompt。
- [x] M4：新增 eDHR 批记录测试页面、独立路由和生产组长标准列表。
- [x] M5：完成定向静态、后端和类型验证，记录剩余阻塞。
- [x] M6：修复芋道源码/admin 看不到“批记录测试”的可见菜单迁移。
- [x] M7：按用户修正将“批记录测试”从“批次执行”内部页签调整为类似“PQC组长”的独立菜单页，并锁定菜单排序。
- [x] M8：新增并校正“一线PQC”内部 tab，将一线PQC检验任务拆解为标准列表测试项。
- [x] M9：保留“一线PQC”内部 tab，并在其后新增“一线生产”内部 tab，将一线生产任务拆解为标准列表测试项。

## Expected Verification

- `node IntRuoyiFronted/tests/e2e/edhr-batch-record-test-tab-static.spec.cjs`
- `node IntRuoyiFronted/tests/e2e/codex-runner-code-readonly-static.spec.cjs`
- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_mes_edhr_batch_record_test_menu_sql.py`
- `pnpm --dir IntRuoyiFronted ts:check`
- `node --max-old-space-size=12288 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json --pretty false`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260808-edhr-batch-record-test-tab\frontend-feature-evidence.md`
- `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestCaseServiceImplTest,CodexTestExecutionServiceImplTest,CodexTestRunnerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 如本机运行态前置齐全，再通过真实页面点击第一行“测试”验证 executionId 创建；前置不齐全时记录 BLOCKED，不用 API-only 冒充通过。

## Applicable Gates

- 统一列表复合工具栏布局门禁：新页面使用 `UnifiedListTemplate` 时必须显式接入 definitions/state/events、稳定 table key、列配置和分页，不用隐藏旧筛选或页面级特例冒充标准列表。
- Codex Runner 自动测试门禁：不得浏览器直接裸调 CLI；执行必须走后端测试执行、Runner 注册/领取/心跳/结构化回写链路，失败检查点必须有差异描述。
- 测试管理测试节点闭环门禁：自然语言测试项必须业务可读、固定样本/范围清晰、可重复运行，不能返回默认成功或缺少检查点。
- 测试管理 schema 迁移门禁：修改 `system_codex_test_case` 必须同步 DO、VO、迁移、测试 fixture 和服务校验。
- eDHR 菜单可见性门禁：admin 可见入口必须通过正式 `system_menu`、`system_tenant_package`、`system_role_menu` 迁移交付，不能只依赖前端隐藏路由。
- 前端多布局模式真实页面门禁：静态合同必须锁定真实路由布局的可见入口；不得用批次执行内部页签或源码存在冒充独立菜单页。
- 前端角色内容页签拆分口径门禁：新增“一线PQC”和“一线生产”都是批记录测试页内部测试分类 tab，不得替代正式一线PQC或正式一线生产业务页。
- 一线生产任务拆解门禁：一线生产测试描述必须覆盖生产组长账号进入、负责工序卡片、负责员工卡片、工序上下文数据联动、设备可选性、设备参数可选性、设备参数限制规则、所选员工电子密码提交和进入报工管理页签等待分配。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否；代码分析模式不可用时显示真实错误并标记 BLOCKED/FAIL。
- `是否从根因和长期维护角度解决`：是；新增显式 `analysisMode` 契约，避免把代码分析伪装成 Playwright E2E。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed：已按用户最新需求保留“批记录测试”独立页的一线PQC内部 tab，并在其后新增“一线生产”内部 tab；一线生产标准列表固定展示 8 条任务拆解，目标静态合同、高内存 TypeScript 检查、前端证据校验和 diff 检查通过。

## Cleanup Keep

- `doc/tasks/20260808-edhr-batch-record-test-tab/bug-regression-evidence.md`
- `doc/tasks/20260808-edhr-batch-record-test-tab/verify-batch-record-test-visible.cjs`
