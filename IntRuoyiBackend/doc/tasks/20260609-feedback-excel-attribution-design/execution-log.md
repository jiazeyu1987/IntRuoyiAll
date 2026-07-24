# MES Excel 报工归属排产工单设计补充执行日志

## 2026-06-09

- BDD: 外部 MES Excel 报工导入后待归属 -> Given 班组长从外部 MES 导出报工 Excel When 本系统加载 Excel Then 系统生成待归属报工，不直接更新排产工单进度。
- BDD: 班组长确认报工归属 -> Given 存在待归属报工 When 班组长选择未完成排产工单及其工序并确认 Then 系统将报工关联到排产工单工序，更新进度并记录归属人和归属时间。
- BDD: 未归属报工不参与重排保护 -> Given 报工 Excel 已导入但未确认归属 When 夜间重排执行 Then 系统不把该导入记录当作已报工任务保护依据，并在看板提示待归属。
- READONLY: 检查 `ThirdPartyFeedbackImportForm.vue`，现有前端入口为“导入第三方报工”，当前文案描述导入成功后自动提交到审批中。
- READONLY: 检查 `ProFeedbackApi.importThirdPartyXlsx`，现有 API 为 `/mes/pro/feedback/import-third-party-xlsx`。
- READONLY: 检查 `ThirdPartyFeedbackImportServiceImpl`，当前导入会按 Excel 任务编码解析任务、创建生产报工并调用 `submitFeedback` 自动提交。
- READONLY: 检查 `MesProFeedbackDO` 和 `MesProFeedbackImportRecordDO`，现有报工已有关联工单、任务、工序字段，导入记录已有来源文件、指纹、工作表、行号和 feedbackId。
- CHANGE: 补充前序 PRD、用户流程和验收标准，明确外部 MES Excel 导入后必须待归属，不直接更新排产工单进度。
- CHANGE: 补充排产工单池前端、后端、数据模型、配置安全和 BDD/TDD/E2E 计划，明确报工归属确认接口和测试要求。
- GREEN: `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root ruoyi-vue-pro\doc\tasks\20260609-next-scheduling-requirements` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root ruoyi-vue-pro\doc\tasks\20260609-scheduling-order-mvp-design` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root ruoyi-vue-pro\doc\tasks\20260609-scheduling-order-mvp-design` -> PASS。
