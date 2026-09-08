# 07 需求-风险-设计-测试-结果追溯矩阵

## 1. 目的

建立 URS、风险、设计、测试、证据和 CSV 检查项之间的追溯关系。当前矩阵为草案，执行结果列需在 IQ/OQ/PQ 后补齐。

## 2. RTM

| URS ID | Risk ID | 设计证据 | 测试 ID | 预期证据 | 当前结果 | CSV 检查项 |
| --- | --- | --- | --- | --- | --- | --- |
| URS-VAL-001 | GAP-RA-001 | `01-validation-strategy.md`, `03-gamp-risk-assessment.md` | DR-001 | QA 批准风险评估 | Draft / 未批准 | 5.1 |
| URS-VAL-002 | RSK-001..018 | `05-functional-design-summary.md` | RTM-001 | 全链路 RTM 批准 | Draft / 未批准 | 5.2, 5.7 |
| URS-VAL-003 | RSK-001..014 | `06-iq-oq-pq-test-plan.md` | IQ-001..010, OQ-001..016, PQ-001..005 | 已执行脚本和验证总结 | 未执行 | 5.3, 5.6 |
| URS-VAL-004 | RSK-015 | `11-periodic-review-change-revalidation-retirement.md` | OQ-015 | 变更影响评估和再验证记录 | 未执行 | 5.4 |
| URS-VAL-005 | GAP-RA-002 | `03-gamp-risk-assessment.md` | DR-002 | GAMP 分类批准 | Draft / 未批准 | 5.5 |
| URS-ER-001 | RSK-001, RSK-006 | `docs/product/production-role-system-operations.md` | OQ-002, PQ-001 | 批记录可查、可读、可追溯 | 未执行 | 5.2, 5.7 |
| URS-ER-002 | RSK-002 | `docs/system/gxp-audit-trail-data-model.md` | OQ-004 | before/after、原因、hash 验证 | 未执行 | 5.7, 5.13 |
| URS-ES-001 | RSK-008 | `docs/adr/ADR-0003-unified-electronic-signature-kernel.md` | OQ-003 | 签名重新认证和内容绑定证据 | 未执行 | 5.2, 5.6 |
| URS-ES-002 | RSK-010 | `docs/changes/20260907-trusted-time-minimal-mvp.md` | OQ-005 | 服务器时间证据 | 未执行 | 5.6, 5.12 |
| URS-AT-001 | RSK-009 | `docs/adr/ADR-0002-unified-gxp-audit-trail.md` | OQ-004 | 审计同事务和失败关闭证据 | 未执行 | 5.7, 5.10 |
| URS-AT-002 | RSK-009, RSK-014 | `docs/system/gxp-audit-trail-config-security-deployment.md` | OQ-014, PQ-004 | 审计导出、归档、恢复证据 | 未执行 | 5.9, 5.15 |
| URS-DCC-001 | RSK-005, RSK-006 | `docs/product/dcc-windchill-version-phase1-prd.md`, `ADR-0001` | OQ-008, OQ-009, PQ-002 | DCC 版本、发布和历史证据 | 未执行 | 5.2, 5.5 |
| URS-DCC-002 | RSK-007 | 注册证 SQL/变更记录 | OQ-010, PQ-002 | 注册证生命周期证据 | 未执行 | 5.2, 5.6 |
| URS-MES-001 | RSK-001, RSK-002 | MES 批记录控制器和 PRD | OQ-002, PQ-001 | 生产事实和签名证据 | 未执行 | 5.3, 5.6 |
| URS-MES-002 | RSK-003 | `production-role-system-operations.md` | OQ-006, PQ-001 | PQC 提交/复核证据 | 未执行 | 5.3, 5.6 |
| URS-MES-003 | RSK-004 | 放行流程设计和 SQL | OQ-007, PQ-001 | 放行前置门禁证据 | 未执行 | 5.3, 5.6 |
| URS-IF-001 | RSK-011 | 生产角色文档、ERP 同步边界 | OQ-011, PQ-001 | ERP 同步对账和缺失阻断 | 未执行 | 5.14 |
| URS-IF-002 | RSK-012 | BPM 签名/审批设计 | OQ-012 | 审批任务、顺序和签名证据 | 未执行 | 5.14 |
| URS-SE-001 | RSK-008 | 权限设计、菜单/角色 SQL | OQ-001, SEC-001 | 角色权限和职责分离证据 | 未执行 | 5.10, 5.11 |
| URS-BC-001 | RSK-014 | 备份最小闭环设计 | IQ-009, OQ-014, PQ-004 | 备份链和恢复演练报告 | 未执行 | 5.9, 5.12 |
| URS-PERF-001 | RSK-017 | 性能/PQ 计划 | PQ-003 | 代表性生产数据性能报告 | 未执行 | 5.12 |
| URS-QMS-001 | RSK-016 | `12-qms-training-approval.md` | DR-003, OQ-016 | QMS、培训、偏差/CAPA 批准 | 未执行 | 5.10, 5.11, 5.13, 5.16 |
| URS-RET-001 | RSK-018 | `11-periodic-review-change-revalidation-retirement.md` | DR-004 | 退役归档方案和不适用确认 | 未执行 | 5.15 |

## 3. RTM 完整性规则

- 每条高风险 URS 至少追踪到一个风险、一个设计证据、一个 OQ/PQ 测试和一个执行结果。
- 执行结果不得写“默认通过”；必须引用测试记录、偏差编号或批准的不适用理由。
- 任何高风险需求缺少测试结果时，验证总结不能判定 GO。
- RTM 更新属于受控文档变更，必须保留版本历史。

## 4. 当前断链

| 断链 | 影响 |
| --- | --- |
| IQ/OQ/PQ 未执行 | 不能证明系统适合预期用途。 |
| QA 批准缺失 | 不能将草案转为受控验证证据。 |
| 培训/供应商/周期回顾记录缺失 | 5.8、5.9、5.10、5.11、5.16 不能关闭。 |
| PQ 代表性数据未批准 | 5.12 不能关闭。 |
