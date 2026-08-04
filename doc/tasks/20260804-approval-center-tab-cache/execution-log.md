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

- 状态：已完成。
- 根因 1：`ApprovalCenterTodo/Done/MyInitiated/Cc` 均设置 `noCache: true`，切走时页面实例被卸载。
- 根因 2：四个路由名与共享 SFC 组件名 `ApprovalCenterWorkbench` 不一致，直接改为 `noCache: false` 仍无法命中 `keep-alive include`。
- 根因 3：共享页面 route watcher 在缓存实例失活期间仍会观察全局路由；若不限定实例路由并比较成功加载状态，切回或切换审批子路由仍会重复请求。
- RED: `pnpm e2e:approval-center:tab-return-no-reload:static` -> FAIL，预期原因：`ApprovalCenterTodo must enable keep-alive caching`。

### M2 最小修复

- 状态：进行中。
- 已为审批中心四个路由启用缓存并声明共享 `keepAliveName: 'ApprovalCenterWorkbench'`。
- 已让 `AppView` 和 `TagsView` 按显式组件缓存身份加入、保留和主动刷新删除缓存。
- 已让审批中心缓存实例只响应自己的 route name，并仅在模块与列表按同一 route state 成功加载后跳过同状态切回加载。

### M3 验证与收尾

- 状态：未开始。

## Blockers

- 当前无本任务 blocker。
