# 排产工单池 MVP 系统设计执行日志

## 2026-06-09

- BDD: 生产工单生成唯一排产工单 -> Given ERP 已同步未完成生产工单 When 排产员选择生产工单、填写承诺交期并加入排产 Then 系统生成一张有效排产工单，并拒绝同一生产工单再次生成第二张有效排产工单。
- BDD: 排产工单承接排产决策 -> Given 生产工单数量、需求日期和状态来自 ERP When 排产员维护排产工单 Then 排产数量、优先级、承诺交期、路线快照和风险状态均以排产工单为准，不被 ERP 自动覆盖。
- BDD: ERP 夜间同步只更新生产工单 -> Given 每天晚上 2 点触发 ERP 同步 When ERP 返回当前日期往前一年期间的生产订单 Then 系统按 ERP 工单编码幂等新增或更新生产工单，排产工单只生成差异提示并等待排产员手动处理。
- BDD: 夜间重排保护已报工任务 -> Given 已存在排程任务且部分任务已有报工 When 每天晚上重排执行 Then 系统重新计算未报工任务的排程，已报工任务不被移动或覆盖。
- READONLY: 检查 `MesProWorkOrderDO`，现有生产工单已有工单编码、ERP 来源编码、数量、已排数量、需求日期、状态和临时冻结字段，可承接 ERP 源数据。
- READONLY: 检查 `MesKingdeeProductionOrderSyncServiceImpl`，当前逻辑已有金蝶同步入口，但后续实现需要从“已有记录跳过”调整为按 ERP 工单编码幂等更新并生成排产工单差异提示。
- READONLY: 检查 `MesProAutoScheduleController` 和 `MesProAutoScheduleServiceImpl`，现有自动排程已有预览、应用、重排预览、重排应用、问题和依赖接口，但主要以生产工单输入，后续需要新增排产工单入口。
- READONLY: 检查 `MesProTaskDO`、`MesProFeedbackDO` 和 `MesProTaskScheduleExtDO`，任务和报工已有关系字段，后续夜间重排必须基于报工存在性保护任务。
- CHANGE: 新增排产工单池 MVP 系统设计文档，覆盖前端页面、后端 API、数据模型、配置安全部署。
- CHANGE: 新增排产工单池 MVP 验收计划，覆盖 BDD 场景、TDD 计划、E2E 路径和测试数据。
- GREEN: `python C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root ruoyi-vue-pro\doc\tasks\20260609-scheduling-order-mvp-design` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root ruoyi-vue-pro\doc\tasks\20260609-scheduling-order-mvp-design` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root ruoyi-vue-pro\doc\tasks\20260609-next-scheduling-requirements` -> PASS，前序需求文档补充口径后结构有效。
