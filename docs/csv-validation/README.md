# IntRuoyi CSV 验证最小可交付包

| 项目 | 内容 |
| --- | --- |
| Package ID | CSV-INT-20260908-DRAFT |
| 系统名称 | IntRuoyi GxP 生产质量与文控系统 |
| 版本状态 | Draft / Gap Closure Package |
| 编制日期 | 2026-09-08 |
| 覆盖范围 | eDHR/MES 批记录、DCC 文控、电子签名、GxP 审计追踪、注册证、PQC/放行、ERP 同步、备份恢复、表单中心 |
| 质量结论 | 本包可作为 CSV 差距关闭和审计准备输入；不能单独替代 QA 批准、正式执行记录、培训记录或生产运行证据。 |

## 使用边界

本目录是计算机化系统验证（CSV, Computerized System Validation）最小可交付包，用于响应“第五章 基于风险评估的 CSV 验证策略”5.1 至 5.16 检查项。本文档基于当前仓库内产品、系统、安全、变更、测试和源码证据编制，不声称已经完成生产质量批准。

若用于正式外部审计，必须由质量负责人补齐以下受控证据：

- QA 批准的 CSV 总计划、风险评估和验证报告。
- 已执行并签署的 IQ/OQ/PQ 测试记录。
- 真实或代表性生产数据的 PQ/性能验证记录。
- 培训记录、供应商评估、变更单、偏差/CAPA、周期回顾和退役归档记录。
- 生产环境配置、备份恢复、WORM/留存、可信时间和特权审计运行证据。

## 文档清单

| 序号 | 文件 | 用途 | 对应检查项 |
| --- | --- | --- | --- |
| 01 | `01-validation-strategy.md` | CSV 总策略、风险分级、V 模型、放行门禁 | 5.1, 5.2, 5.3, 5.4, 5.5, 5.10 |
| 02 | `02-system-inventory-impact-assessment.md` | 系统清单、GxP 影响性评估、接口和数据分类 | 5.1, 5.5, 5.8, 5.14 |
| 03 | `03-gamp-risk-assessment.md` | GAMP 分类、功能风险评估、验证深度 | 5.1, 5.5, 5.12 |
| 04 | `04-urs-int-ruoyi-gxp.md` | 用户需求说明书最小版 | 5.2, 5.3, 5.7 |
| 05 | `05-functional-design-summary.md` | 功能/配置/设计摘要和证据引用 | 5.2, 5.7, 5.14 |
| 06 | `06-iq-oq-pq-test-plan.md` | IQ/OQ/PQ 测试方案与脚本草案 | 5.2, 5.3, 5.6, 5.12 |
| 07 | `07-traceability-matrix.md` | 需求-风险-设计-测试-结果追溯矩阵 | 5.6, 5.7 |
| 08 | `08-deviation-capa-log.md` | 偏差与 CAPA 管理模板及当前差距 | 5.13 |
| 09 | `09-supplier-assessment.md` | 供应商/第三方组件/外部服务评估 | 5.8, 5.14 |
| 10 | `10-validation-summary-report.md` | 验证总结报告草案与 5.1-5.16 判定 | 5.1-5.16 |
| 11 | `11-periodic-review-change-revalidation-retirement.md` | 周期回顾、变更再验证、退役归档方案 | 5.4, 5.9, 5.15 |
| 12 | `12-qms-training-approval.md` | QMS 整合、职责、培训和审批门禁 | 5.10, 5.11, 5.16 |

## 当前总体判定

| 检查项 | 当前判定 | 摘要 |
| --- | --- | --- |
| 5.1 风险评估 | 发现问题 / 本包已补草案 | 当前已生成风险评估草案，但缺 QA 批准版。 |
| 5.2 V 模型 | 发现问题 / 部分证据存在 | 仓库已有 PRD、系统设计、测试和变更资料，但缺正式 V 模型追溯批准。 |
| 5.3 首次使用前确认 | 发现问题 | 不能仅以工程测试替代生产首次使用前 CSV 确认。 |
| 5.4 更改后再确认 | 发现问题 / 部分机制存在 | 项目任务和变更记录丰富，但缺正式 QMS 变更影响评估与再验证签署。 |
| 5.5 软件类别匹配 | 发现问题 / 本包已分类 | IntRuoyi GxP 功能按 GAMP Category 5 处理，需完整验证。 |
| 5.6 测试脚本 | 发现问题 / 本包已补草案 | 已生成 IQ/OQ/PQ 脚本草案，尚未执行。 |
| 5.7 文档和追溯 | 发现问题 / 本包已补矩阵 | 已生成 RTM 草案，缺批准与执行结果。 |
| 5.8 供应商评估 | 发现问题 | 缺供应商/开源组件/外部系统正式评估和质量协议。 |
| 5.9 周期回顾 | 发现问题 | 电子签名已有 SOP 草案，CSV 总体周期回顾尚未执行。 |
| 5.10 纳入 QMS | 发现问题 | 需要绑定企业 QMS 的变更、偏差、CAPA、培训和文件控制流程。 |
| 5.11 人员资质 | 发现问题 | 仓库无验证人员培训和授权记录。 |
| 5.12 性能数据 | 发现问题 | 需要真实或代表性生产数据量的 PQ/性能证据。 |
| 5.13 偏差处理 | 发现问题 / 本包已补模板 | 已生成偏差/CAPA 记录模板，尚无正式偏差关闭证据。 |
| 5.14 接口验证 | 发现问题 | ERP、BPM、对象存储、备份等接口需专项验证报告。 |
| 5.15 退役归档 | 不适用待确认 / 本包已补方案 | 当前未识别已退役 GxP 系统，仍需业务和 QA 确认。 |
| 5.16 报告审批 | 发现问题 | 当前验证报告为草案，缺授权人员签字批准。 |

## 主要仓库证据

- `docs/product/production-role-system-operations.md`
- `docs/product/frontline-process-material-batch-record-mvp-prd.md`
- `docs/product/dcc-windchill-version-phase1-prd.md`
- `docs/product/edhr-urs-template.md`
- `docs/adr/ADR-0001-dcc-windchill-version-model.md`
- `docs/adr/ADR-0002-unified-gxp-audit-trail.md`
- `docs/adr/ADR-0003-unified-electronic-signature-kernel.md`
- `docs/system/gxp-audit-trail-data-model.md`
- `docs/system/gxp-audit-trail-backend-api-design.md`
- `docs/system/gxp-audit-trail-config-security-deployment.md`
- `docs/system/shared-word-template-parser-design.md`
- `docs/security/electronic-signature-periodic-compliance-review-sop.md`
- `docs/security/security-privacy-compliance-review.md`
- `docs/qa/test-suite-evidence.md`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/signature/service/csv/SignatureGovernanceCsvServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrOqPqController.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrValidationPackageController.java`
- `IntRuoyiBackend/sql/mysql/20260908_gxp_audit_trail_core.sql`

## 法规和行业参考

- 中国 GMP 2010 修订版及确认与验证、质量风险管理、供应商管理、年度质量回顾要求。
- EU GMP Annex 11 Computerised Systems。
- FDA Computer Software Assurance for Production and Quality Management System Software。
- PIC/S PI 011 Good Practices for Computerised Systems in Regulated GxP Environments。
- ISPE GAMP 5 风险基础 CSV 方法论。
