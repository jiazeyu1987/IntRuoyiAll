# Execution Log

## User Intent

- 用户要求：PQC 管理下的列表也要像提交页面一样展示实际提交参数，不能再统一塞到“提交内容”列；参数超出上下限允许提交，但异常数值要标红；损耗数量必须等于各损耗原因数量之和。

## BDD Scenarios

- BDD: 提交列表删除红框列 -> Given 组长打开报工管理或 PQC 管理提交列表 / When 列表渲染提交记录 / Then 主表不再显示“生产工单”“PQC”“提交内容”三列，操作和复核列继续保留。
- BDD: 报工/PQC 主列表展示结构化参数 -> Given 员工提交完成数量、损耗数量、损耗原因、设备和参数 / When 组长查看主列表 / Then 列表以完成/检验数量、损耗数量、损耗明细、设备、参数明细等结构化列展示，不能只展示汇总文本。
- BDD: PQC 超限值红色提示且不阻止提交 -> Given PQC 样本值来自冻结项目明细且超出标准上下限 / When 组长查看 PQC 管理列表 / Then 超限样本值在参数明细列标红显示，且该展示逻辑不改变提交接口或提交校验。

## Milestone Updates

- in_progress: 已创建任务目录，读取前端功能交付、前端开发、PowerShell/编码、任务收尾规则和 MES PQC 项目级检验快照门禁。
- completed: 新增 `pqc-submission-structured-columns-static.spec.js`，RED 先失败于旧红框列。
- completed: `TeamLeaderWorkbenchPage.vue` 主列表删除“生产工单/PQC/提交内容”列，新增完成/检验数量、损耗数量、损耗明细、设备、参数明细结构化列。
- completed: 已核对 `FrontlineFixedTemplatePanel.vue`，生产报工 rawPayload 已具备 `lossReasonDetails` 与 `equipmentParameterRules` 快照，列表可展示每个损耗原因数量和参数上下限。
- completed: PQC 参数明细按 `pqcItemDetails/itemResults` 展示设备、设备编号、接收标准、检验方法、样本值、判定，并通过冻结上下限给超限数值加红色样式。

## TDD Evidence

- RED: `node tests\e2e\pqc-submission-structured-columns-static.spec.js` -> FAIL, expected because the old submission main table still renders the red-box `label="生产工单"` column and unified `提交内容` column.
- GREEN: `node tests\e2e\pqc-submission-structured-columns-static.spec.js` -> PASS.
- REGRESSION: `node tests\e2e\pqc-leader-standard-list-template-static.spec.js` -> PASS.
- REGRESSION: `node tests\e2e\pqc-leader-item-snapshot-static.spec.js` -> PASS.
- REGRESSION: `node tests\e2e\mes-frontline-pqc-submit-to-leader-chain-static.spec.js` -> PASS.
- REGRESSION: `pnpm ts:check` -> PASS.
- REGRESSION: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-submission-structured-columns-static.spec.js doc/tasks/20260806-pqc-submission-structured-columns` -> PASS.
- REGRESSION: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-pqc-submission-structured-columns/frontend-feature-evidence.md` -> PASS.
- DIAGNOSTIC: `node tests\e2e\team-leader-workbench-static.spec.cjs` -> FAIL, existing unrelated dirty change removed `data-team-leader-defect-reason-select` and abnormal reason binding before this task.
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260806-pqc-submission-structured-columns --mode preview` -> PASS, keep `task.md`/`execution-log.md`/`verification-report.md`, delete temporary `frontend-feature-evidence.md`.
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260806-pqc-submission-structured-columns --mode apply` -> PASS, deleted `frontend-feature-evidence.md`.
- RECHECK: `node tests\e2e\pqc-submission-structured-columns-static.spec.js` -> PASS.
- RECHECK: `node tests\e2e\pqc-leader-standard-list-template-static.spec.js` -> PASS.
- RECHECK: `node tests\e2e\pqc-leader-item-snapshot-static.spec.js` -> PASS.
- RECHECK: `node tests\e2e\mes-frontline-pqc-submit-to-leader-chain-static.spec.js` -> PASS.
- RECHECK: `pnpm ts:check` -> PASS.
- RECHECK: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-submission-structured-columns-static.spec.js doc/tasks/20260806-pqc-submission-structured-columns` -> PASS.
- RECHECK: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260806-pqc-submission-structured-columns --mode preview` -> PASS, no delete/blocked/warnings.
- EXPERIENCE: 已按 `project-experience-consolidation` 检索现有 `docs/*memory*.md`、`docs/experience-index.md` 和班组长产品文档；当前教训属于本任务业务实现和既有 PQC 快照门禁范围，且相关长期经验文档存在并行脏改动，本轮不新增长期经验文件，避免混入其它任务改动。

## Blockers

- Git closeout blocker: 工作区在本任务开始前已有多个未提交/未跟踪改动，当前分支 `int_main` 与 `origin/int_main` 已对齐，但 `TeamLeaderWorkbenchPage.vue` 包含其它任务的活跃订单/异常原因改动；不能安全提交并推送本任务独立变更。
