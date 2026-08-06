# Execution Log

## Intent

用户要求在生产组长工作台新增“报工历史”tab，展示审核通过的报工历史；内容与报工管理基本一致，但增加审核通过人和审核通过时间。

## BDD

- `BDD: 报工历史只展示审核通过记录 -> Given 生产组长打开工作台 / When 切换到“报工历史”tab / Then 页面必须使用正式报工分页接口并携带 submissionReviewStatus=APPROVED，只展示已审核通过的记录。`
- `BDD: 报工历史展示审核上下文 -> Given 一条报工记录已被组长审核通过 / When 历史列表渲染该记录 / Then 列表显示报工管理基本字段，并显示审核通过人姓名与审核通过时间。`
- `BDD: 报工历史保持只读 -> Given 用户查看报工历史 / When 行记录已审核通过 / Then 行操作只允许查看详情，不得出现复核或修改入口。`
- `BDD: 报工管理保留待复核能力 -> Given 用户停留在“报工管理”tab / When 列表包含待复核或退回记录 / Then 原有复核、修改、详情能力保持不变。`

## RED/GREEN

- 待记录。

## Milestone Updates

- 2026-08-06：创建任务目录与 BDD 记录；已读取前端、后端、E2E、PowerShell、任务收尾规则及前后端交付技能。

## Blockers

- 当前工作区已有未提交/未推送改动，目标页面 `TeamLeaderWorkbenchPage.vue` 和时间轴 mapper 已存在并行差异；本任务仅在现有差异上追加，提交前需按脏工作区/同文件并行改动门禁处理。
