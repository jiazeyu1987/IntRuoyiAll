# 移除零排产活跃订单的 ERP 计划开工时间限制

## Request Summary And Source

- 来源：用户在当前任务中明确要求“ERP计划开工时间缺失 移除这个限制”，并进一步确认“不用管PQC业务日期，不要把这个作为限制”。
- 目标：无有效排产工单时，ERP 计划开工时间为空不得阻塞生产工单进入生产组长活跃订单。

## Current Baseline Reviewed

- 当前零排产链路通过产品唯一正式工艺路线绑定和唯一 ACTIVE 路线版本生成工序快照。
- `MesTeamLeaderActiveOrderServiceImpl` 当前在解析发布快照时要求 `MesProWorkOrderDO.plannedStartTime` 非空，并用其日期生成 PQC 任务业务日期。
- `mes_pqc_inspection_task.business_date` 为 `NOT NULL`，且参与任务身份唯一键，不能写入空值。

## Classification

- 类型：已接受的业务需求变更。

## Impact Analysis

- 产品：缺少 ERP 计划开工时间的零排产生产工单可成为候选并加入活跃订单。
- 设计：零排产 PQC 任务的记录日期改为活跃订单实际加入日期，不再读取 ERP 计划开工时间；有排产链路继续使用排产工序计划日期。
- 数据：不改 schema；`business_date` 继续保持非空和唯一身份约束。
- API：接口形状、权限和请求参数不变，仅候选资格及新增行为变化。
- 测试：更新零排产候选、新增和 PQC 日期断言，保留单排产与多排产回归。
- 发布：不涉及迁移；需要重新部署后端代码后生效。
- 运维：无新增配置、服务或凭据依赖。

## Decision

- 决策：接受。
- 批准依据：用户已明确要求移除该限制，并明确 PQC 业务日期不得作为限制。
- 实现边界：仅零排产模式使用活跃订单加入日期；不放宽产品路线、ACTIVE 版本、发布工序、ERP 数量或正式 QA/PQC 规程门禁。

## Downstream Skill Reruns

- `behavior-driven-development`：更新可观察场景。
- `backend-api-delivery`：严格 TDD 修改服务行为并执行聚焦回归。
- `project-experience-consolidation`：收尾时修订既有零排产经验门禁。

## Blockers And Next Action

- 当前无阻塞。
- 下一步：记录 BDD，运行新增 RED 测试，实现最小后端变更并验证。
