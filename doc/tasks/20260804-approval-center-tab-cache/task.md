# 审批中心页签切回缓存

## Task Goal

审批中心首次进入时正常加载；切换到其它顶部页签后再切回审批中心时，保留已有列表、筛选和分页状态，不重复执行页面初始化请求。用户主动查询、刷新或改变有效筛选条件时仍按正式链路重新加载。

## Milestones

- [x] M1：定位审批中心路由缓存和页面生命周期的根因，建立失败回归测试。
- [x] M2：实现最小正式修复，使审批中心页签切回不重复初始化。
- [x] M3：完成定向回归、类型检查、证据校验并进入任务收尾。

## Expected Verification

- 审批中心首次激活执行一次初始化加载。
- 顶部页签切走再切回时不重复执行初始化加载。
- 已有列表、快速筛选和分页状态保持不变。
- 用户主动查询、刷新或有效路由筛选变化仍可重新请求。
- 运行审批中心目标静态合同、相邻审批中心合同和 `pnpm ts:check`。
- 运行 `bug-regression-fix-loop` 与 `frontend-feature-delivery` evidence validator。

## Applicable Experience Gates

- 审批中心的生效 route query、缓存状态和快速筛选必须保持用户可见且一致，不能通过清空筛选或隐藏状态规避重复加载。
- 前端行为修复必须使用任务专用最小静态合同完成 RED/GREEN；无关历史失败不得替代本任务证据。
- 不吞请求错误，不增加 fallback，不把用户主动刷新能力一并禁用。
- Git 提交前运行分支端口 guard，任务实现、收尾记录分别提交并推送 `origin/int_main`。

## Design Constraint Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；从路由缓存身份与 Vue 激活生命周期解决，不使用延时、请求去重窗口或静默跳过。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

- 实现和定向静态/类型验证已完成：审批中心页签切回缓存合同、相邻审批中心合同和 `pnpm ts:check` 均通过。
- 真实 Playwright 用户路径已通过：从 `/approval-center/todo` 点击真实侧边菜单进入 `/user/profile`，再点击审批中心顶部页签返回；首次列表请求 2 次，返回阶段目标列表请求 0 次，`pageErrors=[]`。
- `task-closeout-cleanup` preview/apply 已通过并清理临时失败截图及临时 evidence 文件。
- 收尾提交 `4c83728ef docs: close approval center tab cache task` 已通过一次性 GitHub proxy `127.0.0.1:8902` 推送到 `origin/int_main`。

## Cleanup Keep

- doc/tasks/20260804-approval-center-tab-cache/approval-center-tab-return-no-reload-real.e2e.cjs
- doc/tasks/20260804-approval-center-tab-cache/approval-center-tab-return-no-reload-result.json
- doc/tasks/20260804-approval-center-tab-cache/approval-center-tab-return-no-reload.png

## Cleanup Candidates

- doc/tasks/20260804-approval-center-tab-cache/approval-center-tab-return-no-reload-failed.png
