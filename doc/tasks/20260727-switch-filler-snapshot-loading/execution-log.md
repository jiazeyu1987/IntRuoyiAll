# Execution Log

## User Intent

- 用户反馈“切换填写人”加载时间太久，询问是否可以使用快照；业务口径是一个批次执行创建之后填写人已固定。

## BDD

- BDD: 使用批次执行创建快照加载填写人 -> Given 批次执行已创建且当前工序存在多个填写人候选快照 When 用户打开“切换填写人”弹窗 Then 前端应直接使用执行详情返回的快照渲染候选人，不应重新调用全量批次详情接口。

## RED/GREEN

- RED: `node yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> FAIL，执行详情前端类型未复用 `EdhrBatchExecutionTaskRespVO`，证明当前实现没有快照字段且弹窗仍依赖全量批次详情。
- GREEN: `node yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> PASS，执行详情 VO/TS 类型包含 `assistSwitchTasks`，前端 `loadAssistFillerSwitchItems` 读取 `execution.value?.assistSwitchTasks` 且不再调用 `getEdhrBatchExecution`。

## Milestone Updates

- 建立任务证据：completed。
- 根因定位：当前 `ExecutionPage.vue` 的 `loadAssistFillerSwitchItems` 每次打开弹窗都会调用 `getEdhrBatchExecution(batchExecutionId)` 拉全量批次详情；执行详情 VO 尚未提供弹窗可用的轻量任务/填写人快照。
- 经验门禁：命中 `docs/backend-development.md#edhr-详情回填门禁`；本任务必须从可追溯任务/填写人快照补齐链路，不得只改前端展示或推断填写人。
- 修复实现：后端执行详情新增 `assistSwitchTasks`，由同批次当前工序任务和活动工作任务 `candidateUserSnapshot` 生成；前端切换填写人弹窗直接读取该快照。

## Verification Evidence

- PASS: `node yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs`
- PASS: `pnpm exec eslint src\api\mes\pro\feedback\index.ts src\views\mes\pro\edhr\ExecutionPage.vue --format stylish`
- PASS: `pnpm ts:check`
- PASS: `git diff --check` / `git diff --cached --check` scoped to current task files.
- BLOCKED: `mvn -pl yudao-module-mes -am "-DskipTests" compile` fails in pre-existing parallel file `MesProEdhrBatchExecutionServiceImpl.java` because `validateCurrentUserIsSpecialNodeFiller(...)` is referenced but not defined. This prevents full backend module compile verification for this task without modifying unrelated work.

## Blockers

- 工作区开始时已有未提交改动、本地分支领先 origin，以及旧任务目录 `doc/tasks/20260727-switch-filler-selection/`；本任务使用独立目录，避免修改无关任务证据。提交/推送阶段需按项目规则单独处理。
- 后端模块编译被非本任务文件 `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java` 阻塞；该文件已有并行 staged/unstaged 改动，本任务未修改也不回滚。
