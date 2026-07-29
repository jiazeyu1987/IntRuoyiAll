# Execution Log

## User Intent

- 用户反馈选择“产品信息”工序后，填写页顶部“工序”仍显示“粗洗工序”，要求继续修复。

## BDD

- BDD: 产品信息顶部标签使用虚拟工序名称 -> Given 当前页面 `batchTaskId` 指向保留粗洗来源 `routeProcessId/processName` 的产品信息任务, When 填写页加载或切换完成, Then 顶部“工序”必须显示“产品信息”。
- BDD: 普通工序顶部标签保持正式名称 -> Given 当前页面任务不是产品信息虚拟工序, When 填写页加载, Then 顶部“工序”仍显示该任务正式 `processName/processCode`。

## Investigation

- 用户截图证明工序卡片切换已经成功，但顶部标签仍显示“粗洗工序”。
- `assistProcessSwitchLabel` 当前直接读取 `execution.value.processName/processCode`。
- 产品信息预览详情在 `loadAssistBatchTaskPreviewExecution` 中仍按追溯来源赋值 `processName: task.processName`，因此顶部标签没有使用虚拟工序名称。

## Git Baseline

- 待记录当前脏工作区基线提交。

## Verification Evidence

- 待记录 RED/GREEN、相邻回归、类型检查和真实 E2E。

## Blockers

- 无。
