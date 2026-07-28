# 20260728 批记录表单列表产品名称下拉筛选执行日志

## User Intent

用户要求将截图红框中的“产品名称”输入下拉框改为：点击显示候选产品名称，点击候选后直接过滤无需查询按钮；也支持手动输入或复制产品名称后点击查询按钮过滤。

## Command / Evidence Log

- BDD: 点击产品名称输入框展示候选 -> Given 批记录表单目录存在多个产品名称 / When 用户点击产品名称筛选输入框 / Then 下拉展示当前批记录表单目录实际存在的产品名称候选。
- BDD: 点击候选立即过滤 -> Given 候选下拉中存在目标产品名称 / When 用户点击该候选 / Then 快速筛选写入 `productName` 并立即请求列表过滤，无需点击查询按钮。
- BDD: 手动输入查询过滤 -> Given 用户手动输入或复制产品名称 / When 用户点击查询按钮 / Then 列表按输入文本作为 `productName` 过滤。
- 2026-07-28: 任务启动。已加载 frontend-feature-delivery、backend-api-delivery、frontend/backend 规则、task-closeout、PowerShell 编码规则。
- 2026-07-28: `git status --short --branch --untracked-files=all` 显示 `int_main` 分支已有既有脏改动且本地 ahead 4；本任务实现前按规则做脏工作区基线提交。

## Baseline Dirty Worktree

- Baseline commit: `dfc71011 chore: baseline dirty worktree before product name dropdown`。
- Baseline scope: 保存实施前已存在的 22 个脏改动文件，包括批记录报表后端测试、产品名称 autocomplete 前端静态合同、其它并行任务文档/脚本与 `docs/e2e-rules.md`。
- Baseline verification: 基线提交后 `git status --short --branch --untracked-files=all` 显示工作区无未提交文件，分支为 `int_main...origin/int_main [ahead 5]`。
- 2026-07-28: 本次接续开始时发现 `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue` 已有任务前残留“批量删除”按钮改动；按脏工作区基线门禁单独提交。
- Baseline commit: `1ffed41c chore: baseline residual batch record form list edits`。
- Baseline scope: 仅 `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue`，保留任务前残留改动，避免混入本任务证据提交。

## RED

- RED: `node -e "<dfc71011^ snapshot assertions>"` -> FAIL, expected reason: task-start parent source lacked batch-record `product-name-options` API wrapper, `triggerOnFocus`, and productName autocomplete quick-filter contract.

## GREEN

- GREEN: `node tests/e2e/edhr-batch-record-form-list-product-filter-autocomplete-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-record-form-list-static.spec.js` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，119 tests，0 failures，0 errors。
- GREEN: `pnpm ts:check` -> PASS。

## REGRESSION

- 2026-07-28: 代码检查确认后端接口、Service、前端 API、`TableQuickFilter` autocomplete、产品名称候选函数和 quick-filter 定义均已落地。
- 2026-07-28: 本机运行态检查：`8081` 前端 HTTP 200，`48081/actuator/health` status `UP`。
- 2026-07-28: 真实页面只读验证登录 `芋道源码/admin` 后打开 `/mes/pro/batch-record-form-list`；页面列表 `/page` 返回业务码 `0`、total `320`、首屏 `20` 行且 `20` 行均有非空产品名称。
- 2026-07-28: 真实页面点击产品名称输入框触发 `/product-name-options?keyword=&latestVersionOnly=false`，HTTP 200 但业务码 `404`；根据本地运行态门禁，当前 `48081` 后端未加载新增 Controller 路由，真实 E2E 不能用 API-only 或旧运行态冒充通过。
- 2026-07-28: 运行 project-experience-consolidation 检查；本次经验已由 `docs/local-runtime.md` 的“隔离构建 Jar 加载门禁 / 新 Controller 运行态加载”覆盖，不新增长期经验文档。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260728-batch-record-product-name-dropdown/frontend-feature-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260728-batch-record-product-name-dropdown/backend-api-evidence.md` -> PASS。

## Blockers

- 本机真实 E2E blocked：需将 `48081` 后端重启/替换为包含本次新增 endpoint 的 verified Jar 后复跑。
- Git closeout blocked：当前 `int_main` 本地 ahead 4 / behind 6；且任务外 `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceTest.java` 出现未提交并行改动，本任务未触碰或提交该文件。
