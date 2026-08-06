# Execution Log

## Intent

用户要求在截图红框位置新增“历史表单”tab，展示审核通过的 PQC 表单历史；内容与“PQC管理”基本一致，并增加谁审核通过、什么时间审核。

## BDD

- `BDD: PQC历史表单只展示审核通过记录 -> Given PQC组长打开工作台 / When 切换到“历史表单”tab / Then 页面必须使用正式 PQC 管理列表接口并携带 submissionReviewStatus=APPROVED，只展示审核通过记录。`
- `BDD: PQC历史表单展示审核上下文 -> Given 一条 PQC 表单已审核通过 / When 历史表单列表渲染该记录 / Then 列表显示 PQC管理基本字段，并显示审核通过人姓名与审核通过时间。`
- `BDD: PQC历史表单保持只读 -> Given 用户查看历史表单 / When 行记录已审核通过 / Then 行操作只允许查看详情，不得出现复核或复核修改入口。`
- `BDD: PQC管理保留复核能力 -> Given 用户停留在“PQC管理”tab / When 列表包含待复核或退回记录 / Then 原有详情、复核、复核修改能力保持不变。`

## RED/GREEN

- 待记录。

## Milestone Updates

- 2026-08-07：已完成脏工作区基线保全；创建任务目录与 BDD 记录；已读取前端规则、任务收尾规则、PowerShell/编码规则、经验索引和 frontend-feature-delivery 技能。

## Blockers

- 暂无。
