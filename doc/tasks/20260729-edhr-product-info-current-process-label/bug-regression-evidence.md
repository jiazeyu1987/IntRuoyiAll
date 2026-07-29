# Bug Regression Evidence

## Bug Summary

选择产品信息虚拟工序后，填写页顶部“工序”仍显示产品信息任务的追溯来源“粗洗工序”。

## Expected Behavior

顶部工序标签必须根据当前 `batchTaskId` 对应任务解析显示工序名称；产品信息显示“产品信息”，普通任务显示正式工序名称。

## Reproduction

- 真实页面：打开 eDHR 填写页，切换到“产品信息”，观察顶部“工序”标签。
- 聚焦静态合同：待新增。

## Root Cause

`assistProcessSwitchLabel` 直接读取 `execution.value.processName/processCode`；产品信息任务的这些字段按正式追溯保留粗洗来源，因此显示错误。

## Regression Test

- 待新增聚焦静态合同，锁定顶部标签通过当前批次任务和虚拟工序名称解析。

## RED

- RED: 待执行。

## GREEN

- GREEN: 待执行。

## Verification

- 待执行。

## Risk And Regression Scope

- 风险集中在辅助填写页顶部当前工序标签及切换后的当前任务解析。
- 不修改后端任务来源、批记录绑定、打开权限、填写人候选和表单槽位链路。

## Blockers And Follow-up

- 当前无阻塞。
