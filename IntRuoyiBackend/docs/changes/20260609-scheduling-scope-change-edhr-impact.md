# 排产需求范围变更与 eDHR 影响评估

## Request Summary and Source

用户确认排产需求范围扩大：ERP 生产订单按工单编码同步到生产工单；同步范围为当前日期往前一年期间的 ERP 生产订单，每天晚上 2 点执行；排产员从生产工单生成不可拆分的排产工单并填写承诺交期，排产数量必须等于生产工单数量；每晚重排，已报工任务和已有 eDHR 执行上下文的任务不动；外部 MES Excel 报工导入后由班组长选择未开始排产工单的工序；同时需要评估当前系统可复用能力、与 eDHR 的冲突以及开发实现路径。

## Current Baseline Reviewed

- `doc/tasks/20260609-next-scheduling-requirements`
- `doc/tasks/20260609-scheduling-order-mvp-design`
- `MesKingdeeProductionOrderSyncServiceImpl`：现有金蝶生产订单同步入口。
- `MesProWorkOrderDO`、`MesProTaskScheduleExtDO`、`MesProFeedbackDO`、`MesProFeedbackImportRecordDO`。
- `MesProAutoScheduleServiceImpl`、`MesProScheduleCalendarServiceImpl`。
- 工艺路线、资源大表、工作站、设备、维修、人工产能相关代码。
- eDHR 批记录执行、审批、签名、归档、追踪、字段审计相关代码和 SQL。

## Classification

Decision type: requirement change + system design change + release planning change.

## Impact

Product impact:

- 需要新增排产工单池，生产工单不再直接作为排产决策对象。
- 排产资源、路线配置、报工归属、夜间重排需要形成一个闭环。
- 排产员需要能解释产能、瓶颈、延迟风险和报工偏差。

Design impact:

- 需要新增排产工单、排产工序快照、排产差异、报工归属、临时资源调整等模型。
- 自动排程服务需要新增排产工单输入路径。
- 外部 MES 报工导入必须从自动建报工改为待归属。
- eDHR 已执行/已归档内容必须作为不可移动边界。

Data impact:

- `mes_pro_work_order` 继续作为 ERP 镜像和生产工单。
- 新增 `mes_pro_schedule_order` 等排产侧表。
- 已归属报工与 eDHR 执行需要可追溯，不得伪造历史归属。

API impact:

- 新增排产工单池 API、排产工单差异 API、报工归属 API、排产资源快照/调整 API、夜间任务 API。
- 现有生产工单、工艺路线、资源、排程日历、任务、报工、eDHR API 需复用并在边界处补字段。

Test impact:

- 必须增加后端单元/集成测试、前端静态契约测试和真实 E2E。
- eDHR 回归必须覆盖已打开批记录执行后排产重排不改动执行快照、审批、归档。

Release impact:

- 不能一次性上线全部需求，建议按排产工单池、资源快照、排程接入、报工归属、夜间重排、看板工作台分阶段发布。
- 每阶段都要保留现有生产工单、生产任务、生产报工、eDHR 可用性。

Operations impact:

- 需要新增或配置 Quartz 定时任务：ERP 同步、差异生成、夜间重排、看板统计刷新。
- 需要失败告警和重跑入口，不允许静默跳过。

## Decision

Accept and split.

接受排产闭环方向，但必须拆成多个受控里程碑实现。当前系统可复用大量 MES 基础能力，但缺排产工单业务边界；如果直接改生产工单或自动排程入口，会与 eDHR、报工、批记录快照发生冲突。

## Required Approvals

- 产品确认排产工单编码格式与状态。
- 业务确认外部 MES Excel 字段样例及产品型号/规格字段可靠性。
- 技术确认 ERP 生产订单接口的最近一年查询字段和已完成/未完成状态字段。

## Downstream Skill Reruns

- system-design-docs：排产工单池、资源快照、报工归属、eDHR 边界设计。
- bdd-tdd-acceptance-planner：每个里程碑拆 BDD/TDD/E2E。
- development-plan-delivery：按里程碑逐个执行。

## Blockers and Next Action

Blockers:

- 当前没有正式排产工单表，不能保证不拆分、不重复、承诺交期和排产快照。
- 当前第三方报工导入会自动创建并提交报工，与用户确认流程冲突。
- 当前 eDHR 执行以生产工单和任务为上下文，重排必须识别并保护这些执行上下文。
- 当前金蝶同步是已同步或已存在即跳过，需改为按 ERP 工单编码幂等更新。

Next action:

- 先实现“排产工单池 + ERP 同步幂等更新 + 排产工单唯一约束”，再接排程引擎和报工归属。
