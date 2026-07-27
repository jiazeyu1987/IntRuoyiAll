# Execution Log

## User Intent

- 用户反馈“切换填写人”加载时间太久，询问是否可以使用快照；业务口径是一个批次执行创建之后填写人已固定。

## BDD

- BDD: 使用批次执行创建快照加载填写人 -> Given 批次执行已创建且当前工序存在多个填写人候选快照 When 用户打开“切换填写人”弹窗 Then 前端应直接使用执行详情返回的快照渲染候选人，不应重新调用全量批次详情接口。

## RED/GREEN

- RED: pending -> 先新增定向静态契约后运行。
- GREEN: pending -> 等待修复后记录通过命令。

## Milestone Updates

- 建立任务证据：completed。
- 根因定位：当前 `ExecutionPage.vue` 的 `loadAssistFillerSwitchItems` 每次打开弹窗都会调用 `getEdhrBatchExecution(batchExecutionId)` 拉全量批次详情；执行详情 VO 尚未提供弹窗可用的轻量任务/填写人快照。
- 经验门禁：命中 `docs/backend-development.md#edhr-详情回填门禁`；本任务必须从可追溯任务/填写人快照补齐链路，不得只改前端展示或推断填写人。

## Verification Evidence

- pending

## Blockers

- 工作区开始时已有未提交改动、本地分支领先 origin，以及旧任务目录 `doc/tasks/20260727-switch-filler-selection/`；本任务使用独立目录，避免修改无关任务证据。提交/推送阶段需按项目规则单独处理。
