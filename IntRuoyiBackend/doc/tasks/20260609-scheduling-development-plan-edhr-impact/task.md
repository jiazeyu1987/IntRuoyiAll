# 排产需求开发计划与 eDHR 影响分析

## 任务目标

基于当前已确认的排产需求和现有 IntRuoyi MES/eDHR 代码，形成可执行开发计划：明确哪些系统能力可以复用，哪些现有实现与新需求冲突，哪些点会影响 eDHR，应该按什么顺序开发才能实现排产员闭环。

## Previous Task Check

- 前序同仓库相关任务：`doc/tasks/20260609-feedback-attribution-product-model-reason/task.md`。
- 检查结果：该任务已标记 `completed`，并已提交。
- 当前工作区仅有运行态文件变动，本任务只新增规划文档，不修改生产代码和业务数据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。计划明确缺排产工单表、缺 ERP 字段、缺外部 MES Excel 样例、缺 eDHR 保护边界时必须阻塞。
- `是否从根因和长期维护角度解决`：是。以新增排产工单业务边界和排产快照为核心，不把排产员决策硬塞进生产工单或 eDHR。
- `是否存在临时补丁或绕过`：否。本任务只输出正式开发计划，不提供临时绕过方案。

## BDD 场景

- BDD: 排产工单成为排产唯一入口 -> Given ERP 同步的生产工单存在 / When 排产员填写承诺交期并生成排产工单 / Then 系统使用排产工单进入排程，并保证同一生产工单只有一张有效排产工单。
- BDD: eDHR 执行保护排产重排 -> Given 某生产任务已经打开 eDHR 执行或已有归属报工 / When 夜间重排执行 / Then 系统不得移动、删除或覆盖该任务及其执行快照。
- BDD: 外部 MES 报工先归属再入账 -> Given 班组长导入外部 MES Excel / When 同工艺流程同工序存在多个产品型号候选 / Then 系统要求选择排产工单和工序后才创建正式报工。

## 里程碑

- [x] M1：检查当前需求文档和系统设计基线。
- [x] M2：检查现有 MES 排产、资源、报工、生产工单和 eDHR 代码能力。
- [x] M3：输出复用矩阵、冲突矩阵和分阶段开发计划。
- [ ] M4：等待用户确认后，拆分第一个实现里程碑。

## 预期验证

- `python C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence docs\changes\20260609-scheduling-scope-change-edhr-impact.md`
- `python -m json.tool doc\tasks\20260609-scheduling-development-plan-edhr-impact\task-state.json`
- `rg --no-ignore -n "可复用|eDHR|冲突|里程碑|排产工单|报工归属|夜间重排" doc\tasks\20260609-scheduling-development-plan-edhr-impact docs\changes\20260609-scheduling-scope-change-edhr-impact.md`

## 当前状态

completed

## 完成记录

- 已确认当前系统没有正式排产工单表，现有“待排产工单”仍围绕生产工单/任务。
- 已确认工艺路线资源、工作站人工产能、设备产能、排程日历、自动排程、生产任务、报工和 eDHR 执行均可复用，但需要新增排产侧边界和保护规则。
- 已形成开发计划、eDHR 冲突矩阵和测试计划。
- 已按最新口径更新计划：ERP 每晚 2 点同步最近一年订单；按工单编码幂等更新；排产数量必须等于生产工单数量；已报工和已有 eDHR 执行任务均为夜间重排保护边界；外部 MES Excel 导入后先由班组长选择排产工单和工序再入账。
- 已复查代码现状：当前金蝶同步会跳过已同步/已存在工单，第三方报工导入会直接创建并提交正式报工，自动重排保护尚未识别 eDHR 执行上下文，这三处需要作为实现优先改造点。

## 最终验证

- GREEN: `python C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence docs\changes\20260609-scheduling-scope-change-edhr-impact.md` -> PASS。
- GREEN: `python -m json.tool doc\tasks\20260609-scheduling-development-plan-edhr-impact\task-state.json` -> PASS。
- GREEN: `rg --no-ignore -n "最近一年|工单编码|eDHR 已打开|外部 MES Excel|排产工单不允许拆分|排产数量必须等于生产工单数量" doc\tasks\20260609-scheduling-development-plan-edhr-impact` -> PASS。
