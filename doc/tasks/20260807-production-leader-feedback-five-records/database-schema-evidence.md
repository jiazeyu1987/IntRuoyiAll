# Database Schema Evidence

## Data Change Goal And Affected Entities

- 目标：在本机 `int_main` 测试环境新增 5 条生产组长报工管理可见的一线生产正式提交数据。
- 受影响实体：`mes_pro_feedback`、`mes_pro_edhr_recordbook_entry`、`mes_pro_edhr_recordbook_event`、`mes_pro_process_pool_event`、`mes_pro_process_pool_quantity_fragment`、`mes_pro_process_pool`。

## Database Engine And Migration Tool

- 数据库引擎：本机 Docker MySQL，数据库 `ruoyi-vue-pro`；写入前需重新确认容器和 schema 可用。
- 变更类型：一次性、任务自有测试 fixture 数据；不创建迁移，不修改表结构。

## Fixture Change

- 任务标识：`CODX-RPT-20260807-%`。
- 计划新增：5 条正式报工及对应 5 条记录本 entry、5 条记录本 event、5 条 `PRODUCTION_SUBMIT` 工序池事件、5 条数量片段，并更新 1 条工序池汇总。
- 数据对象与字段值以写入前真实 schema、正式样本和责任范围核对结果为准。

## Data Safety Analysis

- 写入限定在本机测试数据库、单一租户和精确任务标识。
- 所有写入置于单一事务中，任务标识预存在、正式链路对象缺失、责任范围缺失或行数不符合预期时立即回滚。
- 不修改远程环境，不改 schema，不删除或覆盖既有业务数据，不用默认值掩盖缺失前置。

## Rollback Or Recovery Plan

- 写入前记录目标工序池汇总快照。
- 如需恢复，按任务标识精确删除数量片段、工序池事件、记录本 event、记录本 entry 和正式报工，再恢复工序池汇总快照。
- 本任务目标是保留用户要求的 5 条数据，验证通过后不自动清理。

## BDD Scenarios

- BDD: 生产组长查看新增一线提交数据 -> Given 正式业务对象和责任范围完整 / When 新增 5 条完整正式链路数据 / Then 生产组长今日报工管理命中 5 条且姓名可解析。
- BDD: 缺正式提交前置时停止 -> Given 任一正式对象或责任范围缺失 / When 尝试执行 fixture / Then 事务回滚且不残留孤立数据。
- BDD: 重复任务标识不得再次写入 -> Given 任务标识已存在 / When 再次执行 / Then 事务 fail fast。

## RED Command And Expected Failure

- 待写入前执行；预期任务标识在正式报工和工序池事件中的计数均为 `0`，以证明目标数据尚不存在。

## GREEN Command And Passing Result

- 待写入后执行；预期五类明细表各命中 `5`，工序池汇总指向最后事件，生产组长登录态分页接口命中 5 条且员工姓名非空。

## Migration Verification

- 不涉及 schema migration；使用 `DESCRIBE`、正式样本只读查询、事务写入行数和跨表一致性查询验证 fixture。

## Blockers

- 暂无；缺数据库、正式 schema、测试租户、生产组长、责任员工、完整工单任务或工序池链路时立即阻塞。
