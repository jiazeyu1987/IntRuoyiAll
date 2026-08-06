# Database Schema Evidence

## Data Change Goal

新增 5 条符合一线生产提交格式的正式报工数据，用于本机 `int_main` 生产组长报工管理展示。任务标识为 `CODX-RPT-20260806`。

## Affected Entities

- `mes_pro_feedback`
- `mes_pro_edhr_recordbook_entry`
- `mes_pro_edhr_recordbook_event`
- `mes_pro_process_pool_event`
- `mes_pro_process_pool_quantity_fragment`
- `mes_pro_process_pool`
- `mes_pro_process_pool_team_leader_scope`，只读用于核对生产组长责任员工集合

## Database Engine And Migration Tool

- 数据库引擎：本机 Docker MySQL，容器 `int-ruoyi-mysql`，数据库 `ruoyi-vue-pro`。
- 迁移工具：不新增迁移。本任务是本机任务自有测试数据写入，不改变 schema、索引或约束。

## Schema Or Seed Changes

- Schema changes：无。
- Data seed / fixture changes：新增 5 条正式报工主表记录、5 条记录本 entry、5 条记录本 event、5 条工序池生产提交事件、5 条数量片段，并更新对应工序池汇总状态。
- 写入上下文：tenant `1`，生产组长 `1520/lvyujie`，员工 `964`，工单 `980008`，任务 `981941`，路线 `922119`，路线工序 `928611`，工序 `922987`，工作站 `980009`，设备 `41`，记录本 `980011`，模板 `980010`。
- 报工编码：`CODX-RPT-20260806-001` 到 `CODX-RPT-20260806-005`。

## Data Safety Analysis

- 仅允许本机任务自有测试数据。
- 若缺正式 schema、目标租户、报工人、工序或写入链路，任务阻塞。
- 不操作远端测试服、正式服、备用服，不修改生产租户或无关业务数据。
- 使用任务标识和幂等键限定可追踪范围，不写默认成功、空结果或前端假数据。
- 写入后按生产组长正式时间线 Mapper 口径复验，避免只写 `mes_pro_feedback` 导致页面不可见。

## Rollback Or Recovery Plan

- 如需撤回，仅清理 `CODX-RPT-20260806-%` 标识数据。
- 建议清理顺序：`mes_pro_process_pool_quantity_fragment` -> `mes_pro_process_pool_event` -> `mes_pro_edhr_recordbook_event` -> `mes_pro_edhr_recordbook_entry` -> `mes_pro_feedback`，随后按正式口径重算或回退 `mes_pro_process_pool` 的最新事件和计数。
- 本轮不执行回滚，因为用户目标是保留新增随机数据。

## BDD Scenarios

- BDD: 生产组长查看一线报工随机数据 -> Given 本机环境存在可用生产组长报工管理入口和正式报工数据源 / When 新增 5 条任务自有一线生产格式报工记录 / Then 生产组长报工管理按正式字段展示 5 条新增记录。
- BDD: 缺正式报工前置时停止 -> Given 缺少目标租户、报工人、工序或正式写入链路 / When 尝试新增随机报工数据 / Then 任务必须阻塞并记录缺失前置，不写默认成功或假数据。

## RED Command And Expected Failure

- RED: 写入前执行 `SELECT COUNT(*) FROM mes_pro_feedback WHERE code LIKE 'CODX-RPT-20260806-%'` 与 `SELECT COUNT(*) FROM mes_pro_process_pool_event WHERE event_idempotency_key LIKE 'CODX-RPT-20260806-%'`，均返回 `0`。预期失败原因：目标随机报工数据尚不存在。

## GREEN Command And Passing Result

- GREEN: 通过本机 Docker MySQL 执行新增后只读复验 SQL，结果为 `feedback_count=5`、`pool_event_count=5`、`recordbook_entry_count=5`、`recordbook_event_count=5`、`quantity_fragment_count=5`、`timeline_mapper_visible_count=5`。
- 登录态接口验证：生产组长 `1520/lvyujie` 调用 `/admin-api/mes/pro/process-pool/team-leader/submission/page?leaderType=PRODUCTION&submitDate=2026-08-06&pageNo=1&pageSize=50`，业务码 `0`，任务事件 ID `161-165` 命中 `5`。

## Migration Verification

- 不涉及迁移 up/down。
- Schema verification：已执行 `DESCRIBE` 核对受影响表字段；发现记录本表不存在 `source_biz_type` 后，按真实 `idempotency_key` 字段修正验证 SQL。
- Runtime verification：后端 health `UP`，前端 HTTP `200`，生产组长登录态 API 命中 5 条任务数据。

## Blockers

- 数据新增与验证无阻塞。
- 项目级 cleanup/commit/push 尚未执行，因为当前 `int_main` 工作区存在大量本任务外既有脏改动，需单独确认收尾策略。
