# Execution Log

## Intent

用户反馈：工艺流程页面、批记录表单页面与 DCC 受控浏览相同，切换到其它顶部页签后再切回来时不应重新刷新页面。

## BDD

- BDD: MES route-flow/batch-record tabs keep cached -> Given 用户已打开“工艺流程”和“批记录表单”两个顶部页签 / When 用户切到其它页签后再切回 / Then 已打开页签保留在 `keep-alive` 缓存中，不重新执行首屏加载。
- BDD: MES route-flow/batch-record tabs avoid same-state route watcher reload -> Given 目标页面已完成首屏加载 / When 用户切走再切回且有效路由状态没有变化 / Then 页面保留当前内容，不因 `route.fullPath` watcher 或 query 同步重复刷新。

## Command Log

- Read rules -> PASS: `bug-regression-fix-loop`、`frontend-feature-delivery`、`docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`。
- Read experience index -> PASS: matched frontend tab cache gate and hidden-route tab state gate.
- Inspect git status -> BLOCKED-FOR-CLOSEOUT: workspace has unrelated dirty files and current branch is ahead of origin.

## Milestone Updates

- Task documentation -> PASS: created task goal, BDD, expected verification and design constraint check.

## Verification Evidence

- PENDING: RED contract not yet written.

## Remaining Blockers

- 提交/推送前必须隔离或处理本任务外的脏改动与本地 ahead 状态。
