# IntRuoyi CSV 验证最小可交付包

| 项目 | 内容 |
| --- | --- |
| Package ID | CSV-INT-20260908-DRAFT |
| 系统名称 | IntRuoyi GxP 生产质量与文控系统 |
| 版本状态 | Draft / Gap Closure Package |
| 编制日期 | 2026-09-08 |
| 覆盖范围 | eDHR/MES 批记录、DCC 文控、电子签名、GxP 审计追踪、注册证、PQC/放行、ERP 同步、备份恢复、表单中心 |
| 质量结论 | 本包可作为 CSV 差距关闭和审计准备输入；不能单独替代 QA 批准、正式执行记录、培训记录或生产运行证据。 |

## 正式送审分册包

按用户要求，已另建中文 Word 分册目录：`docs/csv-validation/official-submission-package/`。

该目录将原单文件汇总包拆分为 16 份可流转 Word 文件，覆盖送审导读、正式递交说明与签字责任矩阵、验证总计划、系统清单、GAMP 风险评估、URS、FS、IQ/OQ/PQ、RTM、偏差/CAPA、供应商评估、验证总结、持续验证和 QMS 培训审批。对外提交前仍必须补齐企业真实签章、执行记录、培训记录、供应商资料和生产运行证据。

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
| 13 | `13-training-manual-version-evidence.md` | 培训、操作手册、文档版本和岗位匹配现场证据，配套 `IntRuoyi-eDHR-CSV现场证据包.xlsx` | 7.1, 7.2, 7.3, 7.4 |

## 7.1-7.4 审查交付包

以下文件是面向审查人员的正式交付结构。文件中的“现场填写”“待补证”“待签署”必须由责任部门用真实记录完成，不能把模板状态作为已完成结论。

| 文件 | 用途 |
| --- | --- |
| `CSV-INT-20260908-7.1-7.4现场证据包-正式交付版.docx` | 主文档：检查项结论、系统依据、证据目录、签字矩阵、现场记录页、最终批准与归档页 |
| `CSV-INT-20260908-7.1-7.4签字与审批汇总表.docx` | 集中收集编制、业务、IT、QA、质量负责人和审查人员签字 |
| `CSV-INT-20260908-7.1-7.4现场记录表单册.docx` | F-01 至 F-06：培训、权限授权前确认、手册审批、文件发放回收、现场抽查、CAPA有效性 |
| `SOP-EDHR-090-文档培训版本控制-受控草案.docx` | 文档编号、版本状态、修订、发放回收、培训触发、权限前置和有效性检查 |
| `TRN-EDHR-培训课程大纲与考核标准-受控草案.docx` | 7.1/7.4 培训课程、岗位组合、实操任务、考核标准和批准页 |
| `SOP-EDHR-系统操作手册编写与批准模板.docx` | 7.2 操作手册的详细步骤、异常处理、版本匹配检查和批准页模板 |
| `IntRuoyi-eDHR-CSV现场证据包.xlsx` | 15 个页签：矩阵、台账、签字批准矩阵、现场填报索引、发放回收、培训人员和附件索引 |

## 药监审核老师递交口径

药监现场核查或审核沟通时，建议递交 `docs/csv-validation/official-submission-package/` 下的分册包，优先给出 `00-送审目录与审查导读.docx`、`00A-正式递交说明与签字责任矩阵.docx`、`12-验证总结报告VSR.docx`、`09-需求风险设计测试追溯矩阵RTM.docx`、`03-GAMP分类与风险评估报告.docx` 和 `01-CSV验证总计划.docx`。审核老师负责审查和提出意见，不替代企业 QA、业务、IT、系统所有者和验证负责人对 CSV 文件的内部批准签字。

### 审查使用顺序

1. 先看主文档的“证据目录与交叉引用”和“现场补证与缺项清单”。
2. 用 Excel 台账登记唯一证据编号，并将真实原始记录挂接到对应编号。
3. 用表单册逐人、逐文件、逐权限填写；汇总表不能替代原始签到、考试、实操和发放回收记录。
4. 完成真实记录和 QA 审核后，再使用签字汇总表和主文档最终批准页。

### 审查证据组织规则

- 模板只能证明“已建立控制框架”，不能证明真实培训、考核、批准或授权已经完成；所有空白字段必须明确标记为现场填写、待签署或待确认。
- 每个检查项至少拆成四层证据：矩阵/计划、真实原始记录、批准与版本控制、现场抽查或有效性检查；汇总台账不能替代签到、考试、实操、发放回收和授权审批原件。
- 主文档应提供唯一证据编号，并把签字角色、日期、意见、原始记录位置和最终处置关联起来；审查人员应能从结论回到原始记录。
- 关键权限授权应核对岗位职责、培训合格日期和授权日期，授权日期不得早于培训合格日期；不合格或缺证时应暂停关键权限并进入偏差/CAPA评估。
- 文件版本控制必须同时证明当前有效版本唯一、旧版已回收/作废或有受控归档、培训材料与系统发布版本及 SOP 版本匹配。

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
- `docs/csv-validation/13-training-manual-version-evidence.md`
- `docs/csv-validation/IntRuoyi-eDHR-CSV现场证据包.xlsx`
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
