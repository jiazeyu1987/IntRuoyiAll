# Bug Regression Evidence

## Bug Summary And Expected Behavior

- 现象：审批中心从其它顶部页签切回时重复初始化并重新加载列表。
- 期望：首次进入加载一次；页签切回保留页面实例和状态；主动查询、刷新和有效筛选变化仍重新请求。

## Reproduction

- 路径：打开审批中心，等待列表加载，切换到其它顶部页签，再切回审批中心。
- 自动化命令：`pnpm e2e:approval-center:tab-return-no-reload:static`。

## Root Cause

- 审批中心四个子路由显式禁用缓存。
- 路由名与共享页面组件名不同，现有 `keep-alive include` 无法用路由名命中组件。
- 缓存页面的全局 route watcher 未区分当前缓存实例与其它路由，也未记录成功加载的语义 route state。

## Regression Test

- `tests/e2e/approval-center-tab-return-no-reload-static.spec.js`。
- 覆盖路由缓存元数据、共享组件缓存身份、TagsView 加入/刷新删除、缓存实例路由边界和同状态切回 guard。

## RED

- `pnpm e2e:approval-center:tab-return-no-reload:static` -> FAIL。
- 预期失败：`ApprovalCenterTodo must enable keep-alive caching`。

## GREEN

- 待执行。

## Risk And Regression Scope

- 风险集中在审批中心路由缓存身份、页面激活生命周期和主动刷新入口。
- 不修改后端合同、权限、错误处理或审批业务动作。

## Blockers And Follow-Up

- 当前无 blocker。
