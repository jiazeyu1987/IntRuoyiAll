# 报工归属必须按产品型号选择的需求补充

## 任务目标

补充用户确认的报工归属选择原因：同一个工艺流程里的同一道工序可能涉及不同产品型号，仅凭工艺流程和工序不能唯一确定排产工单，因此班组长导入外部 MES Excel 报工后必须选择具体排产工单和工序，候选列表必须展示产品型号/规格等区分信息。

## Previous Task Check

- 前序同仓库相关任务：`doc/tasks/20260609-feedback-excel-attribution-design/task.md`。
- 检查结果：该任务已标记 `completed`，并已提交。
- 本任务只补充需求与设计原因，不实现代码，不改生产或测试数据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。系统不得在多产品型号候选时自动选择某个排产工单。
- `是否从根因和长期维护角度解决`：是。明确报工归属歧义来自工艺流程/工序与产品型号之间不是一对一关系。
- `是否存在临时补丁或绕过`：否。本任务只补充正式需求和验收口径。

## BDD 场景

- BDD: 同工艺流程同工序多产品型号必须人工选择 -> Given 待归属报工只包含工艺流程和工序线索 / When 系统找到多个不同产品型号的候选排产工单 / Then 系统展示候选产品型号并要求班组长选择，不自动归属。

## 里程碑

- [x] M1：补充前序需求和验收标准。
- [x] M2：补充系统设计和 BDD/TDD/E2E 计划。
- [x] M3：运行文档校验、记录证据并提交。

## 预期验证

- `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root ruoyi-vue-pro\doc\tasks\20260609-next-scheduling-requirements`
- `python C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root ruoyi-vue-pro\doc\tasks\20260609-scheduling-order-mvp-design`
- `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root ruoyi-vue-pro\doc\tasks\20260609-scheduling-order-mvp-design`

## 当前状态

completed

## 完成记录

- 已补充 PRD、用户流程和验收标准，明确同一工艺流程同一工序可能对应多个产品型号，系统不能仅凭工序自动归属。
- 已补充前端设计、后端 API、数据模型和 BDD/TDD/E2E 计划，要求候选列表展示产品型号/规格、产品编码、排产工单编号和工序。
- 已明确后续实现规则：同工艺流程同工序多产品型号候选时，必须由班组长人工选择，不得默认选中第一条。

## 最终验证

- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root ruoyi-vue-pro\doc\tasks\20260609-next-scheduling-requirements` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root ruoyi-vue-pro\doc\tasks\20260609-scheduling-order-mvp-design` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root ruoyi-vue-pro\doc\tasks\20260609-scheduling-order-mvp-design` -> PASS。
