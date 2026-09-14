# Backend API Evidence

## Scope

后端范围为 MES 批记录执行打开/创建、执行详情填写人快照组装，以及 active 执行记录查询隔离。

## Contract

执行详情必须返回 `assistSwitchTasks`；传统批记录打开链路必须保存 `taskId`，active 查询必须在上下文中使用 `batchExecutionId + taskId`，避免新批次复用旧执行详情。

## Validation

无新增数据库迁移；权限和接口入口沿用现有执行详情链路；缺少快照时前端显式报错，不在后端或前端用默认成功、空列表、角色/部门或当前用户推断填写人。

## BDD

- BDD: 重新创建批次执行后切换填写人 -> Given 同批号存在旧 active 执行记录且新批次执行属于当前任务 When 用户打开当前执行详情并切换填写人 Then 后端返回当前 `batchExecutionId + taskId` 对应的填写人快照。

## RED/GREEN

- RED: `node yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> FAIL，执行创建和 active 查询缺少 `taskId` 契约。
- GREEN: `node yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> PASS，静态合同确认 `setTaskId(task.getId())`、`.taskId(reqVO.getTaskId())` 和 mapper `batchExecutionId + taskId` 条件。

## Verification

- BLOCKED: `mvn -pl yudao-module-mes -am "-DskipTests" compile` 最新运行被未跟踪并行 cell-link 源码阻断。
- PASS: mapper final source review changed chained query assignment to non-chained construction, then Maven compile passed.

## Blockers

切换填写人 backend 静态合同无 blocker；最终编译被非本任务 cell-link 工作区改动阻断。未引入 fallback、兼容 shim、mock success 或异常吞噬。
