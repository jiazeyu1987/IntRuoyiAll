# MES Excel 报工归属排产工单设计补充

## 任务目标

将用户确认的真实报工流程补充到下一轮排产需求和排产工单池 MVP 设计中：班组长先在外部 MES 填写报工，再从外部 MES 导出 Excel，本系统加载 Excel 后必须由班组长选择对应排产工单和工序，确认归属后才能更新排产工单进度和影响夜间重排。

## Previous Task Check

- 前序同仓库相关任务：`doc/tasks/20260609-scheduling-order-mvp-design/task.md`。
- 检查结果：该任务已标记 `completed`，并已提交。
- 本任务只补充报工导入归属口径，不实现代码，不改生产或测试数据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。Excel 导入后无法匹配或未确认归属时必须进入待归属/异常状态，不得自动假定归属成功。
- `是否从根因和长期维护角度解决`：是。明确外部 MES Excel 是历史流程约束，本系统通过待归属报工和人工确认建立排产工单进度关系。
- `是否存在临时补丁或绕过`：否。本任务只补充正式需求和设计，不引入临时接口或绕过。

## BDD 场景

- BDD: 外部 MES Excel 报工导入后待归属 -> Given 班组长从外部 MES 导出报工 Excel / When 本系统加载 Excel / Then 系统生成待归属报工，不直接更新排产工单进度。
- BDD: 班组长确认报工归属 -> Given 存在待归属报工 / When 班组长选择未完成排产工单及其工序并确认 / Then 系统将报工关联到排产工单工序，更新进度并记录归属人和归属时间。
- BDD: 未归属报工不参与重排保护 -> Given 报工 Excel 已导入但未确认归属 / When 夜间重排执行 / Then 系统不把该导入记录当作已报工任务保护依据，并在看板提示待归属。

## 里程碑

- [x] M1：检查当前第三方报工 Excel 导入代码和前端入口。
- [x] M2：补充前序 PRD、流程和验收标准。
- [x] M3：补充排产工单池系统设计和验收计划。
- [x] M4：运行文档校验、记录证据并提交。

## 预期验证

- `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root ruoyi-vue-pro\doc\tasks\20260609-next-scheduling-requirements`
- `python C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root ruoyi-vue-pro\doc\tasks\20260609-scheduling-order-mvp-design`
- `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root ruoyi-vue-pro\doc\tasks\20260609-scheduling-order-mvp-design`

## 当前状态

completed

## 完成记录

- 已检查现有 `ThirdPartyFeedbackImportForm.vue`、`ProFeedbackApi.importThirdPartyXlsx`、`ThirdPartyFeedbackImportServiceImpl`、`MesProFeedbackDO` 和 `MesProFeedbackImportRecordDO`。
- 已确认现有第三方报工导入会自动按任务编码创建并提交报工，后续实现必须调整为导入后待归属。
- 已补充 PRD、用户流程和验收标准：外部 MES Excel 导入后生成待归属报工，班组长确认排产工单和工序后才更新排产工单进度。
- 已补充系统设计和验收计划：新增报工归属页面/接口/状态、数据模型扩展、TDD/E2E 测试要求。

## 最终验证

- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root ruoyi-vue-pro\doc\tasks\20260609-next-scheduling-requirements` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root ruoyi-vue-pro\doc\tasks\20260609-scheduling-order-mvp-design` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root ruoyi-vue-pro\doc\tasks\20260609-scheduling-order-mvp-design` -> PASS。
- GREEN: `rg --no-ignore -n "外部 MES|Excel|待归属|归属|班组长|排产工单进度|自动提交" ...` -> PASS，需求、系统设计、验收计划均覆盖报工归属口径。
