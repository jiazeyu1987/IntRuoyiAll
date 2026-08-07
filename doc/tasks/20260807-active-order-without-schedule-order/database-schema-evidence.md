# Database Schema Evidence

## Data Change Goal And Entities

- Goal: 为本机真实 Playwright 写入型 E2E 创建并清理 `AONS-20260807-*` 任务自有 fixture，证明已确认生产工单在零排产工单时可从正式产品路线绑定和 ACTIVE 发布快照加入活跃订单。
- Entities: MES 产品、工序、路线、路线工序、路线版本、路线产品绑定、生产工单、QA 规程/版本/项目，以及 E2E 产生的活跃订单、工序快照、PQC 任务和维护审计。
- 不修改 schema、migration、正式 seed 或现有业务记录。

## Engine And Tooling

- Database engine: 本机 MySQL，连接参数只从 `application-local.yaml` 读取且不输出密码。
- Fixture tool: `doc/tasks/20260807-active-order-without-schedule-order/e2e_fixture.py`，事务写入、精确 ID 反向清理、失败回滚。

## Fixture Contract

- Fixture 固定使用本机测试租户 `tenant_id=122` 和既有测试账号 `acd04lead1`，不使用默认 admin 执行业务写入。
- 新增数据全部使用 `AONS-20260807-*` 稳定前缀；写入前要求同前缀行数为 0。
- 生产工单 `status=CONFIRMED`、ERP 数量为 100、ERP 计划开工时间非空，且 `mes_pro_schedule_order` 行数严格为 0。
- 产品只绑定一条正式路线；路线只存在一个 ACTIVE 版本；发布快照含一个流程工序和一个 SCHEDULE 用途配置，数量系数为 1。
- QA 规程和当前版本均为 PUBLISHED，包含 FIRST 固定数量规则与 PATROL 比例规则，末检明确不适用并提供依据。

## Data Safety And Rollback

- 只操作本机测试租户和任务前缀数据，不访问远端环境。
- 所有 seed 写入在一个事务内完成；任一步失败即 rollback。
- Cleanup 先按生产工单解析精确 ID，再按审计/PQC/工序快照/活跃订单/QA 规程/工单/路线/产品/工序的反向依赖顺序硬删除任务自有行；影响行数不符合预期即 rollback。
- Cleanup 后 `verify-clean` 必须证明所有前缀主对象计数为 0。

## BDD Scenarios

- BDD: 任务 fixture 正式来源闭环 -> Given 测试租户和生产组长账号存在 When seed 任务数据 Then 已确认工单具有唯一产品路线、唯一 ACTIVE 快照、已发布 PQC 规程且有效排产工单数为 0。
- BDD: 无排产真实页面加入 -> Given fixture 已通过正式来源校验 When 生产组长通过页面搜索并加入订单 Then 落库一个活跃订单、一条数量快照和三条使用 ERP 计划开工日期的 PQC 任务。
- BDD: 任务数据可恢复 -> Given E2E 已完成或中途失败 When 执行 cleanup Then 仅删除任务精确 ID 数据且前缀计数归零。

## RED / GREEN / Migration Verification

- RED: pending。
- GREEN: pending。
- Migration verification: 无 migration；真实 schema 只读核对和 fixture seed/verify/cleanup 结果待记录。

## Blockers

- 当前无 schema 前置阻塞；seed 尚未执行。
