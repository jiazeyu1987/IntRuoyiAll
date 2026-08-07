# Bug Regression Evidence

## Bug Summary And Expected Behavior

- 用户证据：生产组长“工序配置”页面中，`球囊扩张导管` 多个工序仍显示“暂无损耗原因”。
- 已知偏差：首次报告声称 `66` 个工序均已新增，但截图中的实际列表不满足“每个工序随机新增 1~6 个”的要求。
- 期望行为：当前登录租户与生产组长页面实际返回的每个工序，均至少存在本任务新增的 `1~6` 个可追溯损耗原因。

## Reproduction

- Path: `测试租户/admin -> 生产组长 -> 工序配置`。
- RED command: Playwright CLI `run-code --filename output/playwright/20260807-production-leader-process-loss-reasons-random-fix/red-current-page.js`。
- RED result: FAIL（符合预期）；当前真实页面 `rowCount=105`、`emptyRowCount=104`、`taskReasonCount=0`。

## Root Cause

- 首次执行登录并写入了 `测试租户/admin`，其正式列表有 `66` 个工序；用户截图和当前默认页面属于 `芋道源码/admin`，正式列表有另一套 `105` 个工序。首次验证只证明了错误租户内的数据完整性，没有覆盖用户实际查看的租户。

## Regression Test

- 使用 Playwright CLI 重新加载真实页面，逐行断言损耗原因列不存在“暂无损耗原因”，并核对每个当前页面工序的任务原因数为 `1..6`。

## GREEN Evidence

- pending。

## Risk And Regression Scope

- 只补齐当前页面确认遗漏的工序；不删除、不修改任何已有损耗原因，不对已有 `RLR0807` 工序重复新增。
- 写入失败立即停止并记录工序，不切换租户、账号、端口或数据源。

## Blockers And Follow-up

- 当前无复现前置阻塞；本机前端 `8081`、后端 `48081` 已确认可用。
