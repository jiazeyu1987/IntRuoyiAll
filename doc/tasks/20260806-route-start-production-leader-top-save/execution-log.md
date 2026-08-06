# Execution Log

## User Intent

用户要求：点击工艺路线流转关系图顶部“保存”按钮时，也要保存右侧“工序开始生产组长”的账号变动。

## BDD

- BDD: 顶部保存覆盖生产组长变动 -> Given 用户在流转关系图右侧“工序开始生产组长”字段明细修改账号，When 点击顶部“保存”，Then 前端必须先通过正式 `route-start-production-leaders/save` 保存生产组长配置，再完成通用关系图保存结果；失败时不能显示通用成功。

## RED / GREEN

- RED: pending
- GREEN: pending

## Root Cause

- 当前顶部“保存”通过 `handleRequestSubmit -> RouteFormContent.submitForm -> saveFromParent` 保存关系图和选中工序属性。
- 生产组长字段只有右侧明细小“保存”会调用 `saveRouteStartProductionLeaders`；顶部保存不会调用该专用接口，导致用户看到“保存成功”但该字段仍未落库。

## Verification Evidence

- pending

## Blockers

- 工作区已有大量非本任务脏改动；本任务只修改目标前端组件、目标静态合同和本任务文档，不回滚或纳入其它任务文件。
