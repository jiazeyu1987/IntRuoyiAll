# SRM srm9 Dependency Inventory

## Purpose and Scope

This inventory records the external dependencies and missing prerequisites that prevent SRM `srm9` from moving from W0 freeze work into W1-W5 production implementation.

Scope:

- SRM Excel requirements `1/2/3/4/7/8/9/10/11/12`.
- W0-01 through W0-05 blockers from `docs/srm/srm9-w0-freeze-pack.md`.
- Regression baseline W0-06 as a confirmed internal dependency.

Out of scope:

- No credentials, secrets, AppID, AppSecret, token, cookie, production endpoint, or password is recorded here.
- This document does not authorize remote server, test server, backup server, or production access.
- This document does not permit mock, default-success, static-dashboard, or controlled-simulation relabeling as real integration.

## Evidence Reviewed

- `docs/srm/srm9-w0-freeze-pack.md`
- `docs/srm/srm9-landing-plan.md`
- `docs/dependencies/srm9-blocker-manifest.json`
- `script/srm/check_srm_w0_freeze_gate.py`
- `script/srm/check_srm_blocker_manifest.py`
- `script/tests/test_srm_w0_freeze_gate.py`
- `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260621-srm9-worktree-delivery\w0-freeze-ledger.md`
- `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260621-srm-current-state-plan-refresh\implementation-readiness-ledger.md`
- `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260621-srm-requirements-conformance-refresh\nonconformance-list.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\integrations\kingdee-erp-official-docs.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 2026-06-21 主系统 `/system/nas` admin 只读发现证据：真实登录 `芋道源码/admin`，NAS 配置完整且根目录可读；未记录任何密码、token、外部端点或 NAS 账号明文。
- 2026-06-21 主系统 `/erp/kingdee-config` admin 只读发现证据：金蝶基础连接、账套、用户名、密码、LCID 和产品/生产/BOM/采购/销售同步配置均存在；证据已脱敏。
- 2026-06-21 真实库只读聚合证据：admin 租户已有 `98` 条金蝶供应商映射、`297` 条 `PUR_PurchaseOrder` 同步记录，以及多类金蝶读同步运行/水位记录。

## Dependency Categories

| Category | SRM Dependency | Related Wave | Status |
| --- | --- | --- | --- |
| External APIs | Enterprise information provider | W1 | `missing` |
| External APIs | K3 supplier master data write-back | W1 | `partially-confirmed` |
| Data, migration, import/export | Mold price and ladder price rule samples | W2 | `missing` |
| Identity and access | Quotation comparison approval owners and approvers | W2 | `missing` |
| Data, migration, import/export | Supplier performance metrics and source data | W3 | `missing` |
| Operations, support, monitoring | Dashboard aggregation refresh and ownership | W3 | `missing` |
| External APIs | PDA, warehouse, logistics, quality, reconciliation sources | W4 | `missing` |
| Payment and billing | E-signature and finance push integrations | W5 | `missing` |
| Security and secrets | External integration credentials and secret handling | W1/W4/W5 | `missing` |
| Operations, support, monitoring | Existing SRM regression scripts and baseline tests | W0-06 | `confirmed` |

## Confirmed Dependencies

| Dependency | Evidence | Verification Method | Notes |
| --- | --- | --- | --- |
| `srm9` backend worktree | `codex/srm9` branch with committed W0 freeze, gate, blocker, and landing docs | `git log --oneline --name-only` | Contains W0 freeze pack, executable gate, dependency inventory, and landing plan |
| `srm9` frontend worktree | `codex/srm9` branch, status only `.runtime/` | `git status --short` | No frontend production change in W0 |
| W0 executable gate | `script/srm/check_srm_w0_freeze_gate.py` | `python -X utf8 -m pytest script/tests/test_srm_w0_freeze_gate.py` | Blocks W1-W5 while dependencies remain blocked |
| Machine-readable blocker manifest | `docs/dependencies/srm9-blocker-manifest.json` | `python -X utf8 script/srm/check_srm_blocker_manifest.py --manifest docs/dependencies/srm9-blocker-manifest.json --freeze-pack docs/srm/srm9-w0-freeze-pack.md --landing-plan docs/srm/srm9-landing-plan.md` | Keeps W0-W6, Excel item coverage, blocker IDs, and freeze-package statuses consistent |
| W0-06 regression baseline | `docs/srm/srm9-w0-freeze-pack.md` | Review W0-06 section and existing SRM test scripts | Only regression protection, not new development |
| Admin NAS readonly source | Main system `/system/nas` with `芋道源码/admin` | Playwright real login, read-only NAS config and directory listing | Confirms the NAS source is reachable; does not provide SRM supplier K3 FormId, field mapping, sandbox write authorization, or failure policy |
| Existing ERP Kingdee config | Main system `/erp/kingdee-config` with `芋道源码/admin` | Playwright real login and readonly `/erp/kingdee-config/get` | Confirms base connection/account-set style configuration exists; does not authorize SRM supplier write-back |
| Existing Kingdee read-sync data | `erp_kingdee_supplier_sync_record`, `erp_kingdee_purchase_order_sync_record`, `erp_kingdee_sync_run`, `erp_kingdee_sync_watermark` | Read-only aggregate SQL; no supplier names, credentials, token, or endpoint host exported | Confirms reusable K3 read-sync history and supplier mapping; not a SRM -> K3 write-back contract |
| SRM page E2E sample | `103 / 山东瑛泰医疗器械有限公司 / INT-010` in test tenant | Playwright login as `测试租户/aoteman`, portal review approval, supplier access eligibility check | Confirms one K3-mapped SRM supplier sample is usable for current page verification; not a K3 `BD_Supplier.Save` write-back sample |
| Local default runtime targets | `.runtime/runtime.env` in both worktrees | Inspect runtime env files | Services not started in W0 |

## Placeholder Dependencies

| Dependency | Placeholder Evidence | Why Not Ready |
| --- | --- | --- |
| Kingdee/OpenAPI documentation entry | `docs/integrations/kingdee-erp-official-docs.md` lists official URLs | URLs do not provide project account, API package, target product line,账套, FormId, credentials, or write authorization |
| Phase45 controlled simulation baseline | SRM UI and E2E explicitly label controlled simulation | Simulation is valid regression evidence but not PDA/warehouse/quality/finance real integration |
| Existing SRM dashboards | No confirmed `src/views/srm` dashboard or performance module | No metric dictionary, source data ownership, or role-specific aggregation contract |

## Missing Dependencies

| ID | Missing Dependency | Blocks | Required Input | Impact |
| --- | --- | --- | --- | --- |
| SRM9-DEP-001 | Enterprise information provider contract | W1 | Provider, endpoint, auth, query key, field mapping, error cases, test sample | Cannot implement enterprise lookup or autofill |
| SRM9-DEP-002 | K3 supplier write-back contract | W1 | Supplier master target FormId, SRM -> K3 field mapping, sandbox write/readback sample, idempotency, retry policy | Existing ERP Kingdee config and read-sync history are reusable, but supplier master write-back contract remains missing; cannot implement supplier self-maintenance sync to K3 |
| SRM9-DEP-003 | Mold price and ladder price business rules | W2 | Pricing examples, quantity intervals, tax/currency rules, attachment rules | Cannot implement pricing model without inventing rules |
| SRM9-DEP-004 | Quotation comparison approval policy | W2 | Approver roles, BPM or SRM approval choice, reject/return/withdraw behavior | Cannot enforce final supplier selection gate |
| SRM9-DEP-005 | Supplier performance scoring contract | W3 | Metrics, weights, thresholds, yearly grading, warning closure rule | Cannot calculate fair or traceable scores |
| SRM9-DEP-006 | Dashboard metric and mobile carrier decision | W3 | Buyer/management KPI dictionary, refresh cadence, permissions, mobile scope | Cannot build dashboard without static or misleading data |
| SRM9-DEP-007 | PDA and warehouse integration contract | W4 | Issue, stock deduction, receiving endpoints, credentials, error codes, samples | Cannot upgrade controlled simulation to real warehouse chain |
| SRM9-DEP-008 | Quality and return closure contract | W4 | Mobile inspection entry, result enum, return, replenishment, deduction rules | Cannot close incoming inspection and return loops |
| SRM9-DEP-009 | Reconciliation authoritative data sources | W4 | PO price source, received quantity source, qualified quantity source, dispute confirmation | Cannot generate authoritative reconciliation |
| SRM9-DEP-010 | E-signature platform contract | W5 | Provider, environment, approval identities, signing evidence fields, failure cases | Cannot implement real signature approval |
| SRM9-DEP-011 | Finance push contract | W5 | Endpoint, auth, payload, receipt fields, retry and manual handling | Cannot implement real finance integration |
| SRM9-DEP-012 | Approved test samples for real integration | W1-W5 | Tenant/account, sample IDs, reset/ownership policy | SRM page sample `103` is now available; K3 write/readback, enterprise information, pricing, PDA, quality, signature and finance samples remain missing for future real integration E2E |

## Owners and Acquisition Steps

| ID | Owner | Acquisition Step | Ready Evidence |
| --- | --- | --- | --- |
| SRM9-DEP-001 | 待业务/IT 指定 | Confirm enterprise information provider and export API contract | Signed or reviewed provider contract plus sample request/response |
| SRM9-DEP-002 | 待 ERP/K3 负责人指定 | Confirm supplier master write-back API package, target FormId, field mapping, sandbox write/readback sample, idempotency and retry policy | K3 supplier write-back field mapping plus sandbox success/failure evidence |
| SRM9-DEP-003 | 采购业务负责人待指定 | Provide mold and ladder price examples | Approved pricing rule examples |
| SRM9-DEP-004 | 采购/审批流程负责人待指定 | Decide BPM engine vs SRM-local approval | Approved approval state diagram |
| SRM9-DEP-005 | 采购管理/质量/交付负责人待指定 | Freeze performance metrics and thresholds | Metric dictionary with owners |
| SRM9-DEP-006 | 管理层/采购负责人待指定 | Confirm dashboard KPI and mobile scope | Dashboard KPI dictionary and mobile decision |
| SRM9-DEP-007 | 仓储/PDA 系统负责人待指定 | Provide issue/stock/receiving API contracts | API contract and test sample |
| SRM9-DEP-008 | 质检负责人待指定 | Provide inspection and return closure rules | Inspection enum and closure state chart |
| SRM9-DEP-009 | 财务/采购/仓储/质检负责人待指定 | Confirm authoritative reconciliation sources | Source-of-truth matrix |
| SRM9-DEP-010 | 签章平台负责人待指定 | Provide e-signature test environment and callback contract | Callback sample and evidence fields |
| SRM9-DEP-011 | 财务系统负责人待指定 | Provide finance push endpoint and receipt contract | Finance sandbox receipt sample |
| SRM9-DEP-012 | 测试负责人待指定 | Prepare owned test samples and reset policy | Sample ledger with tenant/account and IDs |

## Verification Methods

| Dependency | Verification Method |
| --- | --- |
| Enterprise information provider | Contract review plus test call evidence; failure cases must be observable |
| Blocker manifest consistency | Script validation against W0 freeze pack and landing plan; status drift must fail |
| K3 write-back | Sandbox write/readback evidence and failed-sync evidence |
| Pricing rules | Rule examples converted to RED tests before implementation |
| Approval policy | State-machine tests for approve/reject/return/withdraw |
| Performance metrics | Formula tests using real source records, no static fixtures for final acceptance |
| Dashboard metrics | Aggregation tests and readonly Playwright path |
| PDA/warehouse/quality/reconciliation | Contract tests plus real user-path E2E when credentials and samples exist |
| E-signature/finance | Integration contract tests plus real receipt persistence evidence |
| Samples | Task execution log records tenant, account, sample ID, current status, reusable/consumed flag |

## Risks

- If W1 starts before K3 and enterprise contracts are frozen, the system may encode wrong master-data fields and require rework.
- If W2 starts before pricing rules are frozen, the quotation model may not match procurement approval practice.
- If W3 starts before metrics are frozen, dashboards may become static reporting pages rather than trusted operational data.
- If W4/W5 relabel controlled simulation as real integration, acceptance evidence will be materially wrong.
- If samples are not owned per task, E2E may consume shared data and create cross-task false failures.

## Open Questions

- Which enterprise-information provider is the system of record?
- Which K3 product line, account set, FormId, and API package are in scope?
- Does quotation comparison approval use BPM or SRM-local approval?
- Which data source owns on-time delivery, quality, response, and cost-saving metrics?
- Is mobile support a responsive Web scope or separate mobile client scope?
- Which systems own PDA issue, stock deduction, receiving, inspection, and finance push?
- Who owns integration credentials and secret rotation?

## Blockers

| Blocker | Type | Status | Impact |
| --- | --- | --- | --- |
| W1 cannot start | Development blocker | `BLOCKED` | Enterprise/K3 contracts missing |
| W2 cannot start | Development blocker | `BLOCKED` | Pricing and approval rules missing |
| W3 cannot start | Development blocker | `BLOCKED` | Metrics, dashboard, mobile scope missing |
| W4 cannot start | Development/test blocker | `BLOCKED` | PDA/warehouse/quality/reconciliation contracts and samples missing |
| W5 cannot start | Development/test blocker | `BLOCKED` | E-signature/finance contracts and credentials missing |
| W6 cannot start | Launch blocker | `BLOCKED` | No W1-W5 implementation wave is complete and merged |
