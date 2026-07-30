# Dev Plan

## Task Graph

### task_id: T1

- title: F5 审核副本上下限修正
- objective: 实现审核副本 schema、服务规则、锁定边界、签名提交和只读追溯所需合同。
- dependency_ids: []
- affected_paths:
  - `IntRuoyiBackend/yudao-module-mes`
  - `IntRuoyiFronted/src/api/mes`
  - `IntRuoyiFronted/src/views/mes`
- write_scope:
  - MES 工序池审核副本相关 Java、mapper、SQL、测试和必要前端入口。
  - 本任务 `execution-log.md` 中的 F5 执行证据。
- acceptance_ids: [AC-01, AC-02, AC-03, AC-04, AC-05, AC-10]
- validation_steps:
  - 先写 F5 RED 测试并记录失败原因。
  - 实现最小正式模型和服务逻辑。
  - 运行 F5 GREEN 命令和静态合同。
  - 自查 21 条需求门禁中 F5 覆盖项。
- done_definition: F5 所有 mapped acceptance ids 通过定向测试，且不改写原始 payload、不绕过 FIFO 锁定。

### task_id: T2

- title: F6 原始记录 revision 与重新电子签名
- objective: 实现原始记录修改 revision、字段级 diff、重新签名、修改原因和 FIFO 锁定拒绝。
- dependency_ids: []
- affected_paths:
  - `IntRuoyiBackend/yudao-module-mes`
  - `IntRuoyiFronted/src/api/mes`
  - `IntRuoyiFronted/src/views/mes`
- write_scope:
  - MES 工序池原始记录 revision 相关 Java、mapper、SQL、测试和必要前端入口。
  - 本任务 `execution-log.md` 中的 F6 执行证据。
- acceptance_ids: [AC-06, AC-07, AC-08, AC-09, AC-10]
- validation_steps:
  - 先写 F6 RED 测试并记录失败原因。
  - 实现最小正式 revision 和字段 diff 链路。
  - 运行 F6 GREEN 命令和静态合同。
  - 自查 21 条需求门禁中 F6 覆盖项。
- done_definition: F6 所有 mapped acceptance ids 通过定向测试，且已分配或无法确认锁定状态时拒绝修改。

## Conflict Analysis

- T1/T2 可能共同修改工序池事件 VO、时间轴详情、测试建表 SQL、前端详情页和静态合同；合并时由主 agent 统一审查并解决冲突。
- 若 F5/F6 都需要补齐同一基础工序池模型，主 agent 以 `int_main` 当前正式实现为准，禁止两个 worktree 各自建立不兼容模型。

