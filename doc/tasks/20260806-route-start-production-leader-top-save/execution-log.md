# Execution Log

## User Intent

用户要求：点击工艺路线流转关系图顶部“保存”按钮时，也要保存右侧“工序开始生产组长”的账号变动。

## BDD

- BDD: 顶部保存覆盖生产组长变动 -> Given 用户在流转关系图右侧“工序开始生产组长”字段明细修改账号，When 点击顶部“保存”，Then 前端必须先通过正式 `route-start-production-leaders/save` 保存生产组长配置，再完成通用关系图保存结果；失败时不能显示通用成功。

## RED / GREEN

- RED: `workdir=IntRuoyiFronted; node tests/e2e/mes-route-start-production-leaders-static.spec.js` -> FAIL, expected reason: 顶部保存链路缺少 `saveRouteStartProductionLeadersIfChanged`，没有调用生产组长专用保存。
- GREEN: `workdir=IntRuoyiFronted; node tests/e2e/mes-route-start-production-leaders-static.spec.js` -> PASS。
- REGRESSION: `workdir=E:\IntRuoyi; git diff --check -- IntRuoyiFronted\src\views\mes\pro\route\RouteFlowGraphDesigner.vue IntRuoyiFronted\tests\e2e\mes-route-start-production-leaders-static.spec.js doc\tasks\20260806-route-start-production-leader-top-save\task.md doc\tasks\20260806-route-start-production-leader-top-save\execution-log.md` -> PASS。
- REGRESSION: `workdir=IntRuoyiFronted; pnpm ts:check` -> PASS。

## Root Cause

- 当前顶部“保存”通过 `handleRequestSubmit -> RouteFormContent.submitForm -> saveFromParent` 保存关系图和选中工序属性。
- 生产组长字段只有右侧明细小“保存”会调用 `saveRouteStartProductionLeaders`；顶部保存不会调用该专用接口，导致用户看到“保存成功”但该字段仍未落库。

## Verification Evidence

- 顶部保存链路：`saveFromParent` 调用 `saveRouteStartProductionLeadersIfChanged()`，该函数仅在生产组长明细已加载且草稿内容相对基线变化时调用正式保存。
- 正式保存 API：`saveRouteStartProductionLeaders` 复用 `/mes/pro/route/flow-config/route-start-production-leaders/save`，不引入备用数据源、表单槽位或批记录表单替代。
- 成功提示边界：顶部保存联动不额外弹出“生产组长配置已保存”，由外层统一显示通用保存结果；右侧小保存仍保留局部成功提示。
- 项目经验沉淀：已检查 `project-experience-consolidation`；既有 `docs/frontend-development.md#前端按钮文案与行为一致性门禁` 和 `docs/frontend-development.md#前端静态契约隔离门禁` 已覆盖本次经验，无需新增长期经验文档。
- CLEANUP PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260806-route-start-production-leader-top-save --mode preview` -> PASS，keep `task.md` / `execution-log.md` / `verification-report.md`，delete `<none>`。
- CLEANUP APPLY: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260806-route-start-production-leader-top-save --mode apply` -> PASS，deleted_paths `<none>`。

## Blockers

- 当前分支已有非本任务基线提交且仍领先 `origin/int_main`，另有非本任务工作区改动；本任务不改写历史、不强推、不回滚并行改动。
- 未执行 `git push origin int_main`：当前领先提交包含大量非本任务基线内容，直接推送会超出本任务边界。
