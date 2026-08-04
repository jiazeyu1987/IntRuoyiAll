# Bug Regression Evidence

## Bug Summary And Expected Behavior

- 现象：审批中心从其它顶部页签切回时重复初始化并重新加载列表。
- 期望：首次进入加载一次；页签切回保留页面实例和状态；主动查询、刷新和有效筛选变化仍重新请求。

## Reproduction

- 路径：打开审批中心，等待列表加载，切换到其它顶部页签，再切回审批中心。
- 自动化命令：待 M1 定位后补充。

## Root Cause

- 待 M1 定位后补充。

## Regression Test

- 待 M1 新增任务专用静态合同。

## RED

- 待执行。

## GREEN

- 待执行。

## Risk And Regression Scope

- 风险集中在审批中心路由缓存身份、页面激活生命周期和主动刷新入口。
- 不修改后端合同、权限、错误处理或审批业务动作。

## Blockers And Follow-Up

- 当前无 blocker。

