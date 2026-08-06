# Frontend Feature Evidence

## Feature Goal

生产组长模块新增“报工历史”页签，只展示已审核通过的报工历史，并新增审核通过人、审核通过时间列。

## Acceptance

- 生产组长每组模块页签都包含独立“报工历史”tab。
- 报工历史使用正式报工分页接口，强制 `submissionReviewStatus=APPROVED`。
- 报工历史表格显示审核通过人和审核通过时间。
- 报工历史行只允许查看详情，不显示复核或修改入口。

## UI Entry Points

- `TeamLeaderWorkbenchPage.vue` 的生产组长模块页签组新增 `reportHistory`。
- 报工历史复用正式报工表格区域，使用独立列配置 key：`mes.processPool.teamLeader.submissions.productionHistory`。

## API Contract

- 仍使用正式班组长提交分页接口。
- 报工历史请求强制 `submissionReviewStatus=APPROVED`。
- 前端 VO 增加 `submissionReviewLeaderUserName?: string`。

## BDD

- `BDD: 报工历史只展示审核通过记录 -> Given 生产组长打开工作台 / When 切换到“报工历史”tab / Then 页面必须使用正式报工分页接口并携带 submissionReviewStatus=APPROVED，只展示已审核通过的记录。`
- `BDD: 报工历史展示审核上下文 -> Given 一条报工记录已被组长审核通过 / When 历史列表渲染该记录 / Then 列表显示审核通过人姓名与审核通过时间。`
- `BDD: 报工历史保持只读 -> Given 用户查看报工历史 / When 行记录已审核通过 / Then 行操作只允许查看详情。`

## RED

- `RED: node IntRuoyiFronted/tests/e2e/team-leader-production-report-history-tab-static.spec.cjs -> FAIL, 报工历史 tab、APPROVED 查询、审核列与只读边界不存在。`

## GREEN

- `GREEN: node IntRuoyiFronted/tests/e2e/team-leader-production-report-history-tab-static.spec.cjs -> PASS。`
- `GREEN: pnpm ts:check -> PASS。`

## Blockers

- 当前工作区存在并行任务改动和未推送提交，本任务未单独 commit/push。