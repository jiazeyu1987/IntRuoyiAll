# Execution Log: 修复审批中心已办页系统异常

## User Intent

- 用户截图显示进入“审批中心 > 已办”后，列表区域出现“系统异常”，且当前页面无审批任务数据。

## BDD

- BDD: 已办审批列表正常加载 -> Given 用户进入审批中心“已办”页 / When 前端以 `viewType=DONE` 请求统一审批任务分页 / Then 系统必须返回正式 DONE 视图结果或空态，不显示“系统异常”。

## TDD Evidence

- RED: pending.
- GREEN: pending.

## Work Log

- 创建任务目录和初始任务文档。
- 已确认工作树已有大量未提交改动，且 `IntRuoyiFronted/src/views/approval-center/index.vue` 已有未提交修改；后续只做最小差异并避免覆盖无关改动。

## Blockers

- 当前分支 `int_main` 已领先 `origin/int_main` 且工作区存在大量既有未提交改动；提交/推送收尾需单独处理基线或由用户确认边界。

