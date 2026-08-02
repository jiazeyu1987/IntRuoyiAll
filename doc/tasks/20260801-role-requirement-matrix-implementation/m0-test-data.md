# M0 Test Data - Role Requirement Matrix

## Scope

本文件记录 2026-08-02 按用户授权在本机租户 `芋道源码` 准备的 M0 真实 E2E 前置夹具。本文不记录密码、token、数据库连接密钥或签名凭据。

## Tenant

- Tenant ID: `1`
- Tenant name: `芋道源码`
- Authorization: 用户明确授权本轮在该本机租户测试，并使用显式预检令牌 `USER_APPROVED_YUDAO_SOURCE_20260802`。

## Role Accounts

| Role Slot | User ID | Username | Nickname | Local Test Permission |
|---|---:|---|---|---|
| productionEmployee | 964 | liuyueyue | 刘悦悦 | super_admin + approval_center_entry |
| productionLeader | 1520 | lvyujie | 吕玉洁 | super_admin + approval_center_entry |
| qa | 1301 | sunxiaoqing | 孙晓庆 | super_admin + approval_center_entry |
| pqcInspector | 659 | shangmengying | 商孟莹 | super_admin + approval_center_entry |
| pqcLeader | 512 | huzonggang | 胡宗港 | super_admin + approval_center_entry |
| releaseOwner | 1618 | zhengxiaofang | 郑小方 | super_admin + approval_center_entry |

- Password status: six accounts were reset to the user-specified shared test password; value is intentionally not repeated in task documents.
- Permission note: `super_admin` was added as a local M0 test expansion to unblock menu/login coverage. Existing role `approval_center_entry` was preserved.

## M6 Unauthorized Permission Fixture

| Fixture Slot | User ID | Username | Nickname | Local Test Permission |
|---|---:|---|---|---|
| unauthorizedActor | 910272 | aoteman | 芋艿1 | approval_center_entry |

- Password status: existing account `aoteman` was verified to log in with the user-specified shared local test password; value is intentionally not repeated in task documents.
- Permission note: `aoteman` does not have `super_admin`, `*:*:*`, or `mes:pro-process-pool-team-leader:maintain`; it is used only to prove wrong-role active-order mutation rejection and does not replace the six business role accounts.

## Electronic Signature Fixtures

| Role Slot | User ID | Signature Image ID | Authorization State |
|---|---:|---:|---|
| productionEmployee | 964 | 22 | ENABLED |
| productionLeader | 1520 | 23 | ENABLED |
| qa | 1301 | 24 | ENABLED |
| pqcInspector | 659 | 25 | ENABLED |
| pqcLeader | 512 | 26 | ENABLED |
| releaseOwner | 1618 | 27 | ENABLED |

- The six signature image rows reuse the existing local test image file metadata from `dcc-e2e-signature.png`.
- No historical signature records were forged; only authorization and active image fixtures were created for future real E2E login/signature paths.

## Pressure Pump Route

| Field | Value |
|---|---|
| Route ID | 922119 |
| Route code | RT000028 |
| Route name | 球囊扩张压力泵 |
| Latest active version ID | 448 |
| Latest active version | V21 |
| First route process | 928609 / 粗洗工序 |
| Second route process | 928610 / 精洗工序 |
| Batch record report ID | 1d05410f1d3140c5b8aa6786887ae69c |
| Process inspection bindings | 928609 and 928610 both have `PROCESS_INSPECTION` / `过程检验记录` bindings |

## Local Production And Transfer Fixtures

| Fixture | ID / Code | Notes |
|---|---|---|
| Work order | `980008` / `RRM-20260801-PP-MO-001` | Local M0 fixture, product `902149`, quantity `300`, not ERP authoritative source |
| Transfer 1 | `1` / `RRM-20260801-PP-TRANSFER-01` | Quantity `150`, pressure pump item `902149`, batch `RRM-20260801-PP-BATCH-01` |
| Transfer 2 | `2` / `RRM-20260801-PP-TRANSFER-02` | Quantity `150`, pressure pump item `902149`, batch `RRM-20260801-PP-BATCH-02` |

## QC / QA Local Fixture

| Fixture | ID / Code | Notes |
|---|---|---|
| QC template | `5` / `RRM-20260801-IPQC-PRESSURE-PUMP` | Existing QC/IPQC template fixture only; does not replace formal QA regulation version model |
| Indicator | `5` / `RRM-20260801-IPQC-CUXI-WG` | 粗洗工序外观确认 |
| Indicator | `6` / `RRM-20260801-IPQC-JINGXI-CLEAN` | 精洗工序清洁确认 |
| Indicator | `7` / `RRM-20260801-IPQC-PRESSURE-FUNC` | 压力泵功能确认 |
| Template item | `10` | Links template `5` to pressure pump item `902149` |
| Derived QA template | `6` / `RRM-20260801-QA-REG-PP-V21` | Temporary QA/PQC regulation fixture reverse-derived from `过程检验记录 V3.0`; 49 inspection method rows |
| Derived source template | `28 / 32` | `过程检验记录 / V3.0`, source file `过程检验记录.docx` |
| Temporary first-inspection quantity | source quantity or `5` | Stored in `mes_qc_template_indicator.check_method`; user-authorized temporary value |
| Temporary patrol coefficient | `0.05` | Stored in `mes_qc_template_indicator.check_method` and `remark`; user-authorized temporary value |

Derived QA fixture coverage is documented in `m0-derived-qa-regulation.md`. Source row `组装Ⅲ` does not have an exact V21 route process match and is retained as an unmatched source item rather than silently mapped to packaging.

## Remaining Source Limits

- The QC template fixture is not a formal QA regulation/version/published snapshot model.
- The derived QA template fixture is not a formal QA regulation/version/published snapshot model; it only provides local M0 preflight data.
- The work order and transfer rows do not create an activeOrderId relation.
- The pressure pump route V21 is used as-is; production coefficients were not modified.
- M0 is accepted under the revised 2026-08-02 gate because formal source gaps are structured and assigned to M1-M5; those owner milestones remain blocked until their formal source models and production code gates pass.
