# Execution Log

## User Intent

用户确认审核人只需要关心“审核内容是什么”和“当前在哪一步”，不需要默认看到 DCC 项目代码联动、受控浏览落位、投影缺失、内部接口错误等管理员排障信息。

## Preflight

- 已读取 `frontend-feature-delivery` 技能和 `references/frontend-contract.md`。
- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/powershell-encoding.md`。
- 已读取 `docs/experience-index.md`，命中前端同路由多入口分面门禁。
- 发现工作区已有大量既有未提交改动；本任务只修改 BPM DCC 审批摘要相关文件，不覆盖或回滚无关改动。

## BDD

- BDD: DCC 审批人默认只看审核摘要和流程位置 -> Given 审核人打开 BPM 流程详情且流程业务表单是 DCC 受控文件详情, When 页面展示“审批详情”页签, Then 页面默认展示审核内容、当前步骤和当前处理人，并提供进入文控审批处理页的正式入口，不挂载完整 DCC 详情页的项目代码联动、受控浏览落位或管理员排障区块。

## TDD Evidence

- RED: `node scripts/bpm-dcc-approval-compact-detail.test.mjs` -> FAIL, 旧 BPM 详情页没有 `data-testid="bpm-dcc-approval-compact-summary"`，并且自定义业务表单无条件挂载 `<BusinessFormComponent :id="processInstance.businessKey" />`。
- GREEN: `node scripts/bpm-dcc-approval-compact-detail.test.mjs` -> PASS。
- REGRESSION: `node tests/e2e/dcc-original-release-ux-improvements-static.spec.js` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-bpm-dcc-approval-compact-detail/frontend-feature-evidence.md` -> PASS。
- REGRESSION: `pnpm ts:check` -> FAIL, 首个失败在既有无关文件 `src/views/approval-center/index.vue`，缺少 `resolveSourceTaskTypeLabel` / `resolveBusinessTitleLabel` / `resolveBusinessIdentifierLabel` / `resolveBusinessContextTagLabel` / `resolveNodeNameLabel` 等 helper；本任务未修改该文件。
- REGRESSION: `git diff --check -- IntRuoyiFronted/src/views/bpm/processInstance/detail/index.vue IntRuoyiFronted/scripts/bpm-dcc-approval-compact-detail.test.mjs doc/tasks/20260804-bpm-dcc-approval-compact-detail/task.md doc/tasks/20260804-bpm-dcc-approval-compact-detail/execution-log.md` -> PASS，仅提示 Git 工作区 LF 将来会按配置替换为 CRLF。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260804-bpm-dcc-approval-compact-detail --mode preview` -> PASS，keep `task.md` / `execution-log.md` / `verification-report.md`，delete `frontend-feature-evidence.md`。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260804-bpm-dcc-approval-compact-detail --mode apply` -> PASS，已删除临时 `frontend-feature-evidence.md`。
- GREEN: `rg -n "DCC 审批摘要|BusinessFormComponent|BPM 自定义业务表单" docs/experience-index.md docs/frontend-development.md` -> PASS，长期经验已合并到既有前端分面门禁。

## Milestone Updates

- completed: 建立任务记录和 BDD。
- completed: 新增 `IntRuoyiFronted/scripts/bpm-dcc-approval-compact-detail.test.mjs`，先 RED 锁定旧实现无审核摘要且无条件挂载完整 DCC 详情。
- completed: 修改 `IntRuoyiFronted/src/views/bpm/processInstance/detail/index.vue`，DCC 受控文件自定义业务表单在 BPM 审批页默认显示精简审核摘要，包含审核内容、当前步骤、当前处理人和进入文控审批处理页入口；非 DCC 自定义业务表单继续走原 `BusinessFormComponent`。
- completed: 聚焦静态契约、相邻 DCC UX 契约和前端技能证据校验已通过；全量 `pnpm ts:check` 失败归因到既有无关 `approval-center/index.vue` helper 缺失。
- completed: cleanup preview/apply 已执行，仅删除本任务临时 `frontend-feature-evidence.md`，保留核心任务记录和验证报告。
- completed: 按项目经验沉淀要求更新 `docs/frontend-development.md#前端同路由多入口分面门禁` 和 `docs/experience-index.md`，记录 BPM 自定义业务表单无条件嵌入完整业务详情的前置门禁。
- ready_for_closeout: 实现、验证、cleanup 和经验沉淀已完成；提交/推送仍受既有无关脏工作区与分支 ahead 状态影响。
