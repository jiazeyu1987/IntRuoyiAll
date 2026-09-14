# Execution Log

## Intent

用户要求在截图红框位置新增“历史表单”tab，展示审核通过的 PQC 表单历史；内容与“PQC管理”基本一致，并增加谁审核通过、什么时间审核。

## BDD

- `BDD: PQC历史表单只展示审核通过记录 -> Given PQC组长打开工作台 / When 切换到“历史表单”tab / Then 页面必须使用正式 PQC 管理列表接口并携带 submissionReviewStatus=APPROVED，只展示审核通过记录。`
- `BDD: PQC历史表单展示审核上下文 -> Given 一条 PQC 表单已审核通过 / When 历史表单列表渲染该记录 / Then 列表显示 PQC管理基本字段，并显示审核通过人姓名与审核通过时间。`
- `BDD: PQC历史表单保持只读 -> Given 用户查看历史表单 / When 行记录已审核通过 / Then 行操作只允许查看详情，不得出现复核或复核修改入口。`
- `BDD: PQC管理保留复核能力 -> Given 用户停留在“PQC管理”tab / When 列表包含待复核或退回记录 / Then 原有详情、复核、复核修改能力保持不变。`

## RED/GREEN

- RED: `node IntRuoyiFronted\tests\e2e\pqc-leader-form-history-tab-static.spec.cjs` -> FAIL，预期原因：PQC 模块页签中 `data-pqc-leader-module-tab-history` 数量为 `0`，尚未新增“历史表单”。
- GREEN: `node IntRuoyiFronted\tests\e2e\pqc-leader-form-history-tab-static.spec.cjs` -> PASS，历史表单页签、独立列池、`APPROVED` 查询、审核人/审核时间列、只读操作边界均通过静态合同。
- GREEN: `node IntRuoyiFronted\tests\e2e\team-leader-production-report-history-tab-static.spec.cjs` -> PASS，生产报工历史页签合同已适配共享只读/APPROVED 逻辑。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> PASS，后端审核人姓名字段链路仍满足静态合同。
- GREEN: `pnpm ts:check`（workdir `IntRuoyiFronted`）-> PASS，`vue-tsc --noEmit -p tsconfig.relaxed.json` 退出码 `0`。
- GREEN: `git diff --check` -> PASS，仅有 LF/CRLF 工作区提示，无 whitespace error。
- REGRESSION BLOCKER: `node IntRuoyiFronted\tests\e2e\team-leader-production-report-payload-columns-static.spec.cjs` -> FAIL，既存断言要求 `productionSubmissionDefaultColumns` 移除“生产工单”列；当前失败点不由本次 PQC 历史表单改动引入，未在本任务中扩大修改生产报工列池。

## Milestone Updates

- 2026-08-07：已完成脏工作区基线保全；创建任务目录与 BDD 记录；已读取前端规则、任务收尾规则、PowerShell/编码规则、经验索引和 frontend-feature-delivery 技能。
- 2026-08-07：脏工作区隔离基线提交：`5718320e5`、`f876ee280`、`dbd117815`、`b5ec093c8`、`3a20010a2`，均为本任务开始/执行期间出现的非本任务改动保全。
- 2026-08-07：并发基线提交 `6fc534b35` 已包含 `TeamLeaderWorkbenchPage.vue` 的 PQC 历史表单实现，同时混入其它任务经验索引/日志补充；后续本任务仅提交静态合同适配和任务 evidence。
- 2026-08-07：已核对后端 `submissionReviewLeaderUserName` 字段在 mapper、ReadDO、RespVO、service 和前端 VO 中存在；本任务无需新增后端字段。
- 2026-08-07：已在 `TeamLeaderWorkbenchPage.vue` 新增 PQC “历史表单”tab、`history` tab key、`showPqcFormHistoryModule`、`isPqcFormHistoryTab`、`PQC_FORM_HISTORY_TABLE_KEY`、历史列池和审核人/审核时间列。
- 2026-08-07：历史页签查询强制 `submissionReviewStatus=APPROVED`，切换/重置时保持 APPROVED；历史页签屏蔽复核与修改入口，仅保留详情。
- 2026-08-07：生产报工历史静态合同更新为兼容 PQC 历史页签共享的 `APPROVED` 查询和只读操作边界。
- 2026-08-07：项目经验沉淀完成，已更新 `docs/frontend-development.md#前端角色内容页签拆分口径门禁` 和 `docs/experience-index.md`，记录重复 module tabs、独立历史状态、列池隔离和相邻合同要求。
- 2026-08-07：`task-closeout-cleanup` preview/apply -> PASS，仅删除已归档的 `frontend-feature-evidence.md`，保留 `task.md`、`execution-log.md`、`verification-report.md`；任务状态更新为 `completed`。

## Blockers

- `team-leader-production-report-payload-columns-static.spec.cjs` 仍失败在既存生产报工默认列池断言，失败内容为 `productionSubmissionDefaultColumns` 仍含 `label: '生产工单'`。本任务未修改生产报工列池，避免将 PQC 历史表单需求扩大成生产报工列池重构。
