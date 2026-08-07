# Execution Log

## User Intent

- 在上一轮设备台账比对结论基础上，用户要求新增 `C01017 / 撤压机`。
- 用户要求将当前光固机编码 `A05059` 改为 `A05075`。
- 用户要求将当前箱型干燥机编码 `B09041` 改为 `B04091`。
- 变更范围限定为本地 `int_main` 运行库的租户 `122`，不修改其它租户或其它设备。

## BDD Scenarios

BDD: 新增撤压机 -> Given 租户 122 不存在未删除编码 C01017 且正式设备类型和车间已核对, When 新增设备 C01017 / 撤压机, Then 台账和设备条码各存在唯一且一致的正式记录。

BDD: 光固机编码修正 -> Given 租户 122 的设备 A05059 唯一存在且 A05075 不存在, When 将该设备编码改为 A05075, Then 原设备 ID 和非编码业务字段不变，所有正式关联及条码不再残留 A05059。

BDD: 箱型干燥机编码修正 -> Given 租户 122 的设备 B09041 唯一存在且 B04091 不存在, When 将该设备编码改为 B04091, Then 原设备 ID 和非编码业务字段不变，所有正式关联及条码不再残留 B09041。

BDD: 任一前置条件失败时整体回滚 -> Given 三项变更在同一事务中执行, When 任一唯一性、关联、租户或影响行数断言失败, Then 三项变更均不提交且现有数据保持原状。

## Command Intent And Evidence

- READ: 已读取 `docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/database-rules.md` 和 `docs/experience-index.md` 适用门禁。
- GIT BASELINE: 任务开始时 `int_main` 比 `origin/int_main` 领先 1 个提交，且存在并发任务日志改动；并发提交 `9c7507e1d` 随后保存了该日志和另一并发任务的初始文档。该提交不作为本任务实现提交，本任务未修改其内容。
- SCHEMA: 上一轮只读核对已确认本地运行库为 Docker `int-ruoyi-mysql` 的 `ruoyi-vue-pro`，正式设备表为 `mes_dv_machinery`；本任务写入前将重新核对 schema、目标行和引用关系。

## Milestone Updates

- M1 completed：任务记录、BDD、预期验证、设计约束和并发 Git 基线已记录。
- M2 in_progress：正在只读核对目标设备、关联表、条码和新增设备必填元数据。

## Blockers

- 当前无已确认 blocker；业务数据尚未修改。
