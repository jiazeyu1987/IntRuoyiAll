# 排产工单池 MVP 系统设计

## 任务目标

基于已确认的下一轮排产需求，完成“排产工单池 MVP”的工程设计与验收计划。重点明确生产工单到排产工单的边界、排产数量规则、ERP 每日同步边界、夜间重排边界、数据模型、后端接口、前端页面入口、权限、失败行为和 BDD/TDD 验证路径，为后续实现提供可执行输入。

## Previous Task Check

- 前序同仓库相关任务：`doc/tasks/20260609-next-scheduling-requirements/task.md`。
- 检查结果：该任务已标记 `completed`，并已提交。
- 本任务会补充前序需求中的最新业务决策，并在新的系统设计文档中引用这些决策。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺 ERP 必要字段、缺路线、缺产能、缺承诺交期、重复生成排产工单等情况必须显式阻塞或提示，不允许静默成功。
- `是否从根因和长期维护角度解决`：是。以排产工单作为排产业务边界，生产工单只承接 ERP 源数据，排产工单承接排产员决策和排程计算。
- `是否存在临时补丁或绕过`：否。本任务只做正式系统设计和验收计划，不引入临时绕过。

## BDD 场景

- BDD: 生产工单生成唯一排产工单 -> Given ERP 已同步未完成生产工单 / When 排产员选择生产工单、填写承诺交期并加入排产 / Then 系统生成一张有效排产工单，并拒绝同一生产工单再次生成第二张有效排产工单。
- BDD: 排产工单承接排产决策 -> Given 生产工单数量、需求日期和状态来自 ERP / When 排产员维护排产工单 / Then 排产数量、优先级、承诺交期、路线快照和风险状态均以排产工单为准，不被 ERP 自动覆盖。
- BDD: ERP 夜间同步只更新生产工单 -> Given 每天晚上 2 点触发 ERP 同步 / When ERP 返回当前日期往前一年期间的生产订单 / Then 系统按 ERP 工单编码幂等新增或更新生产工单，排产工单只生成差异提示并等待排产员手动处理。
- BDD: 夜间重排保护已报工任务 -> Given 已存在排程任务且部分任务已有报工 / When 每天晚上重排执行 / Then 系统重新计算未报工任务的排程，已报工任务不被移动或覆盖。

## 里程碑

- [x] M1：补充前序需求文档中的最新业务决策。
- [x] M2：检查现有 MES 生产工单、排程、任务、报工、ERP 同步相关代码和表结构。
- [x] M3：形成排产工单池 MVP 系统设计文档。
- [x] M4：形成 BDD/TDD 验收计划。
- [x] M5：运行文档校验、记录证据并提交。

## 预期验证

- `python C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root doc\tasks\20260609-scheduling-order-mvp-design`
- `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root doc\tasks\20260609-scheduling-order-mvp-design`
- `rg --no-ignore -n "排产工单|工单编码|承诺交期|晚上 2 点|已报工|不允许拆分" doc\tasks\20260609-scheduling-order-mvp-design`

## 当前状态

completed

## 完成记录

- 已补充前序需求文档，写入用户确认的排产数量、ERP 同步时间、同步范围、ERP 状态边界、夜间重排、已报工任务保护和直接修改不走审批规则。
- 已检查现有 MES 生产工单、金蝶同步、自动排程、生产任务、任务扩展和报工数据对象，确认当前系统已有部分基础能力，但缺排产工单池数据边界。
- 已新增系统设计文档：前端页面、后端 API、数据模型、配置安全部署。
- 已新增验收计划文档：BDD 场景、TDD 计划、E2E 计划、测试数据要求。
- 已明确下一步实现重点：新增排产工单表和页面、按 ERP 工单编码幂等更新生产工单、排产工单差异提示、排产工单进入排程、夜间重排保护已报工任务。

## 最终验证

- GREEN: `python C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root ruoyi-vue-pro\doc\tasks\20260609-scheduling-order-mvp-design` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root ruoyi-vue-pro\doc\tasks\20260609-scheduling-order-mvp-design` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root ruoyi-vue-pro\doc\tasks\20260609-next-scheduling-requirements` -> PASS。

## Cleanup Keep

- `doc/tasks/20260609-scheduling-order-mvp-design/docs/system/frontend-design.md`
- `doc/tasks/20260609-scheduling-order-mvp-design/docs/system/backend-api-design.md`
- `doc/tasks/20260609-scheduling-order-mvp-design/docs/system/data-model.md`
- `doc/tasks/20260609-scheduling-order-mvp-design/docs/system/config-security-deployment.md`
- `doc/tasks/20260609-scheduling-order-mvp-design/docs/acceptance/bdd-scenarios.md`
- `doc/tasks/20260609-scheduling-order-mvp-design/docs/acceptance/tdd-plan.md`
- `doc/tasks/20260609-scheduling-order-mvp-design/docs/acceptance/e2e-plan.md`
- `doc/tasks/20260609-scheduling-order-mvp-design/docs/acceptance/test-data.md`
