# Execution Log

## User Intent

- 2026-08-04：用户要求审批中心从其它顶部页签切换回来后不要每次重新加载。
- 验收解释：首次进入正常加载；切走再切回保留页面实例及已有状态；主动查询、刷新和有效筛选变化仍正常重新加载。

## Baseline

- 分支：`int_main`。
- 初始状态：分支领先 `origin/int_main` 1 个提交，工作区存在其它任务改动。
- 分支端口门禁：`scripts/preflight/branch-runtime-port-guard.ps1` -> PASS，`int_main` 使用前端 `8081`、后端 `48081`。
- 既有脏改动基线提交：`0dcee54f8 Baseline: preserve existing worktree changes before approval center cache fix`。
- 基线提交包含既有 13 个文件，本任务尚未修改审批中心源码或测试。

## BDD / TDD

- BDD: 审批中心页签切回保留页面 -> Given 用户已进入审批中心并完成列表加载、筛选或分页操作，When 用户切换到其它顶部页签后再切回审批中心，Then 页面沿用原实例和已有状态且不重复执行初始化请求。
- BDD: 审批中心主动操作仍可刷新 -> Given 审批中心页面处于缓存状态，When 用户主动查询、刷新或改变有效路由筛选，Then 页面按正式请求链路重新加载并明确暴露失败。

## Milestone Updates

### M1 根因与 RED

- 状态：进行中。
- 待记录：受影响组件、路由缓存身份、失败测试命令和预期失败原因。

### M2 最小修复

- 状态：未开始。

### M3 验证与收尾

- 状态：未开始。

## Blockers

- 当前无本任务 blocker。

