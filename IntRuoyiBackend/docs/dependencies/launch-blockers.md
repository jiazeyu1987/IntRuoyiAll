# SRM srm9 Launch Blockers

## Purpose and Scope

This document separates SRM `srm9` blockers by delivery phase so the team can see what prevents development, testing, staging, and launch.

It is based on the W0 freeze package and does not claim any missing external dependency is available.

## Evidence Reviewed

- `docs/dependencies/dependency-inventory.md`
- `docs/dependencies/srm9-blocker-manifest.json`
- `docs/srm/srm9-w0-freeze-pack.md`
- `docs/srm/srm9-landing-plan.md`
- `script/srm/check_srm_w0_freeze_gate.py`
- `script/srm/check_srm_blocker_manifest.py`
- `script/tests/test_srm_w0_freeze_gate.py`
- `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260621-srm9-worktree-delivery\verification-report.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\integrations\kingdee-erp-official-docs.md`
- 2026-06-21 主系统 `/system/nas` admin 只读发现：NAS 配置完整、根目录可读；未发现足以冻结 SRM 供应商 K3 写回的 FormId、字段映射或测试写入授权。
- 2026-06-21 主系统 `/erp/kingdee-config` admin 只读发现：金蝶基础连接和同步配置存在；真实库聚合显示已有金蝶读同步水位、供应商映射和 `PUR_PurchaseOrder` 同步记录；未输出敏感值。
- 2026-06-22 测试租户真实页面样本：`103 / 山东瑛泰医疗器械有限公司 / INT-010` 已通过门户审核台和准入资格校验 Playwright E2E，可作为 SRM 页面样本；K3 `BD_Supplier.Save` 写回样本仍未通过。

## Development Blockers

| ID | Blocker | Blocks | Required Resolution |
| --- | --- | --- | --- |
| SRM9-DEV-001 | Enterprise information provider contract missing | W1 | Provider, auth, field mapping, test samples, error cases |
| SRM9-DEV-002 | K3 supplier write-back contract missing | W1 | NAS 资料源、金蝶基础配置和读同步历史已确认可访问/可复用；`BD_Supplier.Save` 探针仍因组织字段失败；仍需 supplier FormId、SRM -> K3 字段映射、sandbox write/readback policy |
| SRM9-DEV-003 | Mold/ladder price rules missing | W2 | Approved rule examples and validation behavior |
| SRM9-DEV-004 | Quotation approval policy missing | W2 | Approval owner, state flow, reject/return/withdraw rules |
| SRM9-DEV-005 | Performance and dashboard metrics missing | W3 | Metric dictionary, weights, thresholds, source ownership |
| SRM9-DEV-006 | Mobile support scope missing | W3 | Decision: responsive Web, separate mobile client, or deferred |
| SRM9-DEV-007 | PDA/warehouse/quality contracts missing | W4 | API contracts, error model, state mapping |
| SRM9-DEV-008 | E-signature/finance contracts missing | W5 | API contracts, credentials, receipt fields |

## Test Blockers

| ID | Blocker | Blocks | Required Resolution |
| --- | --- | --- | --- |
| SRM9-TEST-001 | Enterprise/K3 sandbox samples missing | W1 E2E | SRM 页面样本 `103` 已补齐；仍需企业信息库样本和 K3 `BD_Supplier.Save` 写回/回读样本 |
| SRM9-TEST-002 | Pricing and approval edge-case samples missing | W2 tests | Mold price, ladder price, rejection, withdrawal samples |
| SRM9-TEST-003 | Real performance source data missing | W3 tests | Monthly source records and expected score examples |
| SRM9-TEST-004 | Real PDA/warehouse/quality samples missing | W4 E2E | Issue, receiving, inspection, return, reconciliation samples |
| SRM9-TEST-005 | Real signature/finance receipt samples missing | W5 E2E | Signature callback, finance success/failure receipts |
| SRM9-TEST-006 | Sample ownership ledger missing for future waves | W1-W5 E2E | Tenant, account, sample ID, reusable/consumed flag |

## Staging Blockers

| ID | Blocker | Blocks | Required Resolution |
| --- | --- | --- | --- |
| SRM9-STAGE-001 | External network and credential policy not approved | Staging integration | Approved credential storage and network allow-list |
| SRM9-STAGE-002 | No staging callback endpoints confirmed | Signature/finance/K3/PDA callbacks | Endpoint list and callback verification plan |
| SRM9-STAGE-003 | No rollback or replay policy for failed integrations | Staging incident handling | Retry, manual repair, replay, and audit policy |
| SRM9-STAGE-004 | No monitoring ownership for external integrations | Staging operations | Logs, correlation IDs, alerts, support owner |

## Launch Blockers

| ID | Blocker | Blocks | Required Resolution |
| --- | --- | --- | --- |
| SRM9-LAUNCH-001 | W1-W5 are not implementation-ready | Launch | Corresponding W0 packages must be `FROZEN` and implemented |
| SRM9-LAUNCH-002 | No real external integration evidence | Launch claims | Real interface hit and failure evidence for each claimed integration |
| SRM9-LAUNCH-003 | No merged-result verification | Launch | Verify on merged `int_main`, not only worktree |
| SRM9-LAUNCH-004 | No admin readonly verification for final scope | Launch | `芋道源码/admin` readonly verification after test-tenant writes |
| SRM9-LAUNCH-005 | No rollback statement for integration failures | Launch | Rollback or disable strategy per integration |

## Blocker Owners

| Area | Owner Status | Required Owner |
| --- | --- | --- |
| Enterprise information provider | 待指定 | 业务/IT 接口负责人 |
| K3 | 待指定 | ERP/K3 负责人 |
| Pricing rules | 待指定 | 采购业务负责人 |
| Quotation approval | 待指定 | 采购审批流程负责人 |
| Performance metrics | 待指定 | 采购管理、质量、交付负责人 |
| Dashboards and mobile | 待指定 | 管理层/采购负责人 |
| PDA/warehouse/logistics | 待指定 | 仓储/PDA/物流系统负责人 |
| Quality and return closure | 待指定 | 质检负责人 |
| Reconciliation | 待指定 | 财务/采购/仓储/质检联合负责人 |
| E-signature and finance push | 待指定 | 签章平台/财务系统负责人 |
| Test samples | 待指定 | 测试负责人 |

## Resolution Evidence

| Blocker Type | Evidence Required Before Status Change |
| --- | --- |
| Development blocker | Signed-off rule, API contract, field mapping, or state chart |
| Manifest blocker | `srm9-blocker-manifest.json` updated and `check_srm_blocker_manifest.py` passes |
| Test blocker | Owned sample ledger and successful preflight against approved environment |
| Staging blocker | Approved environment, credential handling, callback, logging, and rollback plan |
| Launch blocker | RED/GREEN/REGRESSION, real E2E evidence, merged-result verification, and admin readonly result |

## Open Questions

- Which exact owners can approve each external integration contract?
- Which integrations can provide sandbox access before production approval?
- Which datasets may be used for performance and dashboard verification?
- Whether mobile support is in the current SRM delivery scope or a separate project.
- Whether K3 and finance integrations must be synchronous or asynchronous with retry.
