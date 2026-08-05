# Execution Log

## User Intent

- 用户报告 `TeamLeaderWorkbenchPage.vue` 首屏渲染读取 `submissionMultiFilter.state` 时发生 TypeError。
- 期望页面正常进入，报工列表多维筛选可用，不通过可选链或默认空对象掩盖初始化错误。

## Rule Reads

- 已读取 `bug-regression-fix-loop` 及 `references/bug-contract.md`。
- 已读取 `frontend-feature-delivery` 及 `references/frontend-contract.md`。
- 已读取 `docs/task-closeout-rules.md`。
- 已读取 `docs/frontend-development.md`。
- 已读取 `docs/local-runtime.md`、`docs/powershell-encoding.md`。
- 已读取 `docs/experience-index.md`，命中 `docs/frontend-development.md#统一列表复合工具栏布局门禁`。

## BDD Scenarios

- BDD: 班组长工作台首屏可渲染多维筛选 -> Given 用户进入生产组长或 PQC 组长工作台, When 报工列表首屏渲染, Then `submissionMultiFilter` 已完成正式初始化且页面不会因读取 `state` 崩溃。

## TDD Evidence

- RED: `node tests/e2e/team-leader-multifilter-render-state-static.spec.js` -> FAIL，现有源码未把 hook state/events 暴露为顶层绑定，模板仍直接解引用包装对象。
- GREEN: 待执行。

## Root Cause

- 当前 Vite 编译产物已经包含 `submissionMultiFilter`，正常完整 setup 初始化时 hook 返回契约有效。
- 页面通过 `submissionMultiFilter.state/updateState/removeCondition` 在渲染阶段直接解引用 hook 包装对象；模板热更新先应用到仍持有旧 setup state 的组件实例时，旧实例没有新增包装对象，因而在父组件 render 阶段抛出 TypeError。
- 正式修复方向是将模板需要的 hook 成员作为顶层 setup binding 暴露，避免 render 阶段再解引用包装对象；不增加可选链、空对象或静默降级。

## Blockers

- 当前工作区存在多个并行任务改动，目标文件也有未提交改动；本任务只修改可明确归属的最小 hunk。
