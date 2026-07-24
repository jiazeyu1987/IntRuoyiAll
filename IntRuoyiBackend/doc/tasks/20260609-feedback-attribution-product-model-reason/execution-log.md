# 报工归属必须按产品型号选择的需求补充执行日志

## 2026-06-09

- BDD: 同工艺流程同工序多产品型号必须人工选择 -> Given 待归属报工只包含工艺流程和工序线索 When 系统找到多个不同产品型号的候选排产工单 Then 系统展示候选产品型号并要求班组长选择，不自动归属。
- CHANGE: 补充前序 PRD、用户流程和验收标准，明确报工归属人工选择原因是同工艺流程同工序可能涉及多个产品型号。
- CHANGE: 补充排产工单池前端、后端、数据模型和验收计划，要求候选列表展示产品型号/规格，禁止仅凭工艺流程和工序自动归属。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root ruoyi-vue-pro\doc\tasks\20260609-next-scheduling-requirements` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root ruoyi-vue-pro\doc\tasks\20260609-scheduling-order-mvp-design` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root ruoyi-vue-pro\doc\tasks\20260609-scheduling-order-mvp-design` -> PASS。
