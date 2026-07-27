# Execution Log

## User Intent

- 用户要求表单模板三个按钮按批记录管理的行为执行。
- 用户明确指出实际表单与批记录表单没有直接关系。
- 用户反馈上一轮只移除了错误提示，当前页面交互逻辑仍与批记录管理不同。

## BDD

BDD: 打开当前表单模板 -> Given 用户在表单模板预览区选中模板，When 点击“打开”，Then 当前页面切换到只读模板工作区，且只携带表单模板上下文。

BDD: 编辑当前表单模板 -> Given 用户在表单模板预览区选中模板，When 点击“编辑”，Then 当前页面切换到模板编辑工作区，且不要求批记录绑定。

BDD: 模拟填写当前表单模板 -> Given 用户在表单模板预览区选中模板，When 点击“填写”，Then 跳转到独立的表单模板模拟填写页面，页面从当前模板版本的 `jimuSchemaJson` 加载内容。

BDD: 保持领域独立 -> Given 任意普通表单模板未绑定批记录表单，When 用户执行三个按钮中的任意一个，Then 不读取或校验 `batchRecordReportId`、`batchRecordBindingStatus` 或批记录 `reportId`。

BDD: 页面级交互壳层一致 -> Given 批记录管理通过同页 `DesignerWrapper` 处理打开/编辑并通过独立页面处理填写，When 用户在表单模板执行同名操作，Then 表单模板也使用同样的页面级切换结构，但组件只加载 FormCenter 模板上下文。

## Command Intent

- 只读检查批记录管理与表单模板三个按钮的源码流转。
- 新增聚焦静态合同锁定同页工作区、独立填写页和领域边界。
- 执行 RED/GREEN、类型检查和真实页面验证。

## Milestone Status

- Milestone 1: `completed`
- Milestone 2: `completed`
- Milestone 3: `completed`
- Milestone 4: `completed`
- Milestone 5: `completed`

## Evidence

- 当前批记录管理“打开/编辑”切换同页 `DesignerWrapper`，“填写”跳转独立 `template-simulate` 页面。
- 当前表单模板“打开/编辑/填写”分别使用三个弹窗，未完成交互行为对齐。
- RED: `node tests\e2e\form-template-button-interaction-parity-static.spec.js` -> FAIL，缺少同页工作区路由模式。
- RED: `mvn -pl yudao-module-bpm -am "-Dtest=FormCenterTemplateVersionQueryTest,FormCenterRuntimeContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，缺少 `getTemplateVersion` 精确查询方法。
- E2E DIAGNOSTIC: `/approval-center/manager/form-center/template` -> 404；真实动态菜单入口为 `/mdm/form-center/template`，独立填写页必须注册在同一真实路径域。
- ROOT CAUSE: 上一轮只把三个弹窗改成 `index.vue` 内部的三个条件工作区，并使用 `mode=workspace`；批记录管理实际采用 `DesignerWrapper v-if / 列表 v-else` 和独立模拟填写页面组件，因此页面级切换结构仍不一致。
- RED: `node tests\e2e\form-template-button-interaction-parity-static.spec.js` -> FAIL，缺少 `FormTemplateDesignerWrapper.vue` 和 `FormTemplateSimulatePage.vue`。
- RED: 同一聚焦合同 -> FAIL，DesignerWrapper/模拟填写仍会先执行模板池查询，未按页面级动作精确读取模板版本。
- RED: 真实 Playwright 请求审计 -> FAIL，独立填写路由切换时旧列表实例与新页面实例各请求一次精确模板版本。
- GREEN: `node tests\e2e\form-template-button-interaction-parity-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\form-template-independent-button-actions-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\form-batch-template-tabs-singleton-static.spec.js` -> PASS。
- GREEN: `node node_modules\eslint\bin\eslint.js src\views\form-center\template\index.vue src\views\form-center\template\FormTemplateSimulatePage.vue src\views\form-center\template\components\FormTemplateDesignerWrapper.vue src\router\modules\remaining.ts` -> PASS。
- GREEN: Vue SFC parse + `compileScript` -> PASS，`index.vue`、`FormTemplateSimulatePage.vue`、`FormTemplateDesignerWrapper.vue` 均通过。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check`（任务前端文件）-> PASS。
- GREEN: BPM 定向 Maven 回归 -> PASS，13 tests（此前本任务已完成，当前纠偏未修改后端）。
- REGRESSION: `node tests\e2e\form-center-static.spec.js` -> FAIL，仅剩既有无关断言 `activeMenu: '/mdm/form-center/policy'`；本任务相关 DesignerWrapper、模拟填写页面和旧弹窗移除断言均已通过。
- REAL E2E: 本机 `http://127.0.0.1:8081/mdm/form-center/template`，身份标签 `芋道源码/admin`，模板 `templateId=28`、`versionNo=V3.0`。
- REAL E2E OPEN: 点击“打开”进入 `/mdm/form-center/template?...&mode=designer&templateMode=preview`，显示 FormCenter DesignerWrapper 和只读模板；仅请求 `GET /form-center/templates/28/versions/V3.0`。
- REAL E2E EDIT: 点击“编辑”进入 `/mdm/form-center/template?...&mode=designer&templateMode=edit`，显示规则编辑工作区；仅请求同一精确模板版本接口。
- REAL E2E FILL: 点击“填写”进入 `/mdm/form-center/template/simulate?...`，显示独立模拟填写工作区；仅请求一次精确模板版本接口。
- REAL E2E SAFETY: 三个动作可见弹窗均为 0，未出现“当前模板未绑定批记录表单”，FormCenter 写请求为 0，console error 和 page error 均为 0。
- RUNTIME NOTE: 首次真实验证期间共享 `48081` 后端停止监听，前端认证接口返回 500；未擅自重启。共享运行任务恢复 `48081` health `UP` 后，真实 E2E 完整通过。
- GREEN: `project-experience-consolidation` -> PASS，已将“复用页面组件必须用显式属性隔离路由实例，避免 watcher 重复请求”合并到既有 `docs/frontend-development.md`，无需新建长期经验文档。
- GREEN: `task-closeout-cleanup preview/apply` -> PASS，仅清理本任务临时产物，保留 `task.md`、`execution-log.md`、`verification-report.md` 和 `bug-regression-evidence.md`。
- GIT BASELINE: `219169b70a17461d160d4aa47cd9295f604a4ed6`（`chore: checkpoint concurrent workspace changes`）保存基线时的 44 个并行工作区文件；本任务实现文件未进入该提交，`docs/experience-index.md` 中本任务经验索引 hunk 已随基线提交保存。
- GIT BASELINE FILES: `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesBatchRecordBaseSchemaTest.java`；`IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProBatchRecordVersionPhaseTwoMigrationContractTest.java`；`IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProEdhrFormFillLogMenuContractTest.java`；`IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProEdhrTemplateConfigMenuRemovalContractTest.java`；`IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/md/workstation/importer/BalloonProcessDeviceMappingImportServiceImplTest.java`；`IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionFieldAuditQueryExportServiceTest.java`；`IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportRenameServiceImplDbTest.java`；`IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/importer/ThirdPartyFeedbackImportServiceImplDbTest.java`；`IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/importer/IntGyRouteMarkdownImportServiceImplDbTest.java`；`IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/importer/Sheet1RouteExcelImportServiceImplDbTest.java`；`IntRuoyiFronted/scripts/codex-test-runner.mjs`；`IntRuoyiFronted/tests/e2e/codex-test-runner-child-settlement-static.spec.js`；`doc/tasks/20260727-batch-record-list-detail-500/bug-regression-evidence.md`；`doc/tasks/20260727-batch-record-list-detail-500/execution-log.md`；`doc/tasks/20260727-batch-record-list-detail-500/task.md`；`doc/tasks/20260727-batch-record-list-detail-500/verification-report.md`；`doc/tasks/20260727-batch-record-node-parse-e2e/execution-log.md`；`doc/tasks/20260727-batch-record-node-parse-e2e/task.md`；`doc/tasks/20260727-batch-record-node-parse-e2e/verification-report.md`；`doc/tasks/20260727-codex-runner-token-invalid/bug-regression-evidence.md`；`doc/tasks/20260727-codex-runner-token-invalid/execution-log.md`；`doc/tasks/20260727-codex-runner-token-invalid/task.md`；`doc/tasks/20260727-codex-runner-token-invalid/verification-report.md`；`doc/tasks/20260727-codex-test-node-chain/node-chain-real-e2e-summary.json`；`doc/tasks/20260727-codex-test-node-chain/start-node-chain-isolated-runtime.ps1`；`doc/tasks/20260727-edhr-notify-all-valid-candidates/backend-api-evidence.md`；`doc/tasks/20260727-edhr-notify-all-valid-candidates/bug-regression-evidence.md`；`doc/tasks/20260727-edhr-notify-all-valid-candidates/dev-plan.md`；`doc/tasks/20260727-edhr-notify-all-valid-candidates/execution-log.md`；`doc/tasks/20260727-edhr-notify-all-valid-candidates/failure-inventory.md`；`doc/tasks/20260727-edhr-notify-all-valid-candidates/prd.md`；`doc/tasks/20260727-edhr-notify-all-valid-candidates/request-analysis.md`；`doc/tasks/20260727-edhr-notify-all-valid-candidates/task-state.json`；`doc/tasks/20260727-edhr-notify-all-valid-candidates/task.md`；`doc/tasks/20260727-edhr-notify-all-valid-candidates/test-plan.md`；`doc/tasks/20260727-edhr-notify-all-valid-candidates/test-report.md`；`doc/tasks/20260727-edhr-notify-all-valid-candidates/verification-report.md`；`doc/tasks/20260727-start-int-main-backend/execution-log.md`；`doc/tasks/20260727-start-int-main-backend/task.md`；`doc/tasks/20260727-start-int-main-backend/verification-report.md`；`docs/changes/20260727-mes-full-regression-green.md`；`docs/e2e-rules.md`；`docs/experience-index.md`；`docs/local-runtime.md`。
- GIT BASELINE WARNING: 基线提交前的 `git diff --cached --check` 报告了其它任务 Markdown 空行/尾随空格；当时命令使用分号继续执行并成功提交。该警告属于基线提交中的并行任务内容，本任务不回退、不改写，也不将其误记为本任务门禁通过。
- GIT IMPLEMENTATION: `c30856e5340a8a32715c98551d6862670add0324`（`fix: align form template button interactions`），13 个本任务文件，500 insertions、379 deletions。
- GIT IMPLEMENTATION FILES: `IntRuoyiFronted/src/router/modules/remaining.ts`；`IntRuoyiFronted/src/views/form-center/template/FormTemplateSimulatePage.vue`；`IntRuoyiFronted/src/views/form-center/template/components/FormTemplateDesignerWrapper.vue`；`IntRuoyiFronted/src/views/form-center/template/components/TemplateViewDialog.vue`（删除）；`IntRuoyiFronted/src/views/form-center/template/index.vue`；`IntRuoyiFronted/tests/e2e/form-center-static.spec.js`；`IntRuoyiFronted/tests/e2e/form-template-button-interaction-parity-static.spec.js`；`IntRuoyiFronted/tests/e2e/form-template-independent-button-actions-static.spec.js`；`doc/tasks/20260727-form-template-button-interaction-parity/bug-regression-evidence.md`；`doc/tasks/20260727-form-template-button-interaction-parity/execution-log.md`；`doc/tasks/20260727-form-template-button-interaction-parity/task.md`；`doc/tasks/20260727-form-template-button-interaction-parity/verification-report.md`；`docs/frontend-development.md`。
- GREEN: pending history object scan -> PASS，`origin/int_main..c30856e5` 共 109 个对象，无大于等于 100 MB 的 blob。
- GIT PUSH: `git push origin int_main` -> PASS，远端从 `ac028943` 前进到 `c30856e5`；推送后本地分支不再 ahead。
- FINAL STATUS: 所有里程碑、验证、经验沉淀、cleanup、实现提交和首次推送均完成，任务状态更新为 `completed`。

## Blockers

- 当前工作区存在其他任务的未提交改动；本任务不得提交、覆盖或清理这些文件。
- 既有宽合同 `form-center-static.spec.js` 仅失败于无关策略路由 `activeMenu` 断言，本任务未修改无关策略路由。
