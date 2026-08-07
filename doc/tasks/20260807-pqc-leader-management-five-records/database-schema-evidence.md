# Database Schema Evidence

## Data Change Goal And Affected Entities

- 目标：在本机 `int_main` 测试环境通过真实一线 PQC 路径新增 5 条 PQC 组长管理可见数据。
- 预计受影响实体：PQC 检验任务、PQC 正式事件、PQC 记录、结构化检验项目/逐件明细，以及一线正式提交链路自动维护的关联实体；最终以真实 schema 和运行结果为准。

## Database Engine And Migration Tool

- 数据库引擎：本机 Docker MySQL `8.0.39`，数据库 `ruoyi-vue-pro`。
- 变更类型：真实页面产生的一次性、任务自有测试 fixture 数据；不创建迁移，不修改表结构。

## Fixture Change

- 任务标识：`CODX-PQC-20260807`。
- 计划新增：5 个任务自有 `PENDING` 检验轮次；随后由真实一线页面生成 5 条正式 PQC 事件、PQC 记录及逐件项目明细。
- 正式来源：tenant 1 工单 `980008`、活跃订单 `12`、路线 `922119/V448`、路线工序 `928609/922985`、发布 QA 规程版本 `16`、生产来源事件 `131`。
- 任务唯一身份：`business_date=2026-08-07 / inspection_type=PATROL / shift_code=CODX5 / round_no=80701..80705`，creator/updater 为任务标识。

## Data Safety Analysis

- 写入限定在本机测试数据库、单一测试租户、目标 PQC 人员和精确任务标识。
- SQL 仅创建正式提交所需的任务自有待检轮次；使用真实前端路径触发 PQC 事件、记录、逐件明细和任务状态更新事务，不直接拼装提交结果。
- 写入前确认任务标识不存在，并排除同一目标对象的并发写入。
- 不修改远程环境，不改 schema，不扩大权限，不覆盖或删除既有业务数据。

## Rollback Or Recovery Plan

- 写入前记录选定活跃订单、工序和关联主键基线。
- 如本任务部分失败，先停止继续提交，再按实际生成的 5 组 task/event/record 主键和任务标识核对依赖关系后精确回滚。
- 本任务目标是保留用户要求的 5 条数据，验证通过后不自动清理。

## BDD Scenarios

- BDD: 一线 PQC 提交进入组长管理 -> Given 正式人员、规程和订单前置完整 / When 真实页面提交 5 次 / Then PQC 组长管理命中 5 条且关联数据完整。
- BDD: 缺正式提交前置时停止 -> Given 任一正式前置缺失 / When 尝试提交 / Then 业务链路 fail fast 且不创建伪造数据。
- BDD: 任务标识防止重复写入 -> Given 标识已存在 / When 再次执行 / Then 新增前停止。

## RED Command And Expected Failure

- marker read-only SQL -> `0`，符合预期 RED，证明 5 条目标正式提交尚不存在。
- 可提交来源只读查询 -> 仅现有 `task=163` 具备完整生产来源，不能满足 5 条任务自有数据，符合预期 RED。

## GREEN Command And Passing Result

- 待写入后执行；预期 5 条正式提交均具有 task/event/record 和结构化项目明细，PQC 组长真实列表全部可见。

## Migration Verification

- 不涉及 schema migration；已通过 `SHOW COLUMNS` / `SHOW INDEX` 核对任务、事件、PQC 记录、逐件明细和 QA 规程真实结构；后续使用真实前端提交结果和跨表一致性查询验证 fixture。

## Blockers

- 暂无；缺数据库、正式 schema、测试账号、发布 QA 规程、活跃订单、PQC 人员范围或无冲突提交对象时立即阻塞。
