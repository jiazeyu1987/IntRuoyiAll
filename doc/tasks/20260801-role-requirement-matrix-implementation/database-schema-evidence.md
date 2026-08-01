# Database Schema Evidence - M0 Test Fixtures

## Data

- Goal: prepare local M0 test fixtures for the role requirement matrix real E2E preflight in tenant `1 / 芋道源码`.
- Affected entities: `system_users`, `system_user_role`, `dcc_electronic_signature_authorization`, `dcc_electronic_signature_image`, `mes_pro_work_order`, `mes_wm_batch`, `mes_wm_material_stock`, `mes_wm_transfer`, `mes_wm_transfer_line`, `mes_wm_transfer_detail`, `mes_qc_template`, `mes_qc_indicator`, `mes_qc_template_indicator`, `mes_qc_template_item`.
- Created or updated: six role accounts, six role bindings to `super_admin`, six electronic signature authorizations/images, one RRM work order, two RRM transfer fixtures, one IPQC QC template, three indicators, three template-indicator links, and one template-product link.
- Sensitive data handling: account password values are not written to this evidence file; `real:check` evidence redacts passwords.

## Migration

- Migration tool: none. This was a local data fixture write against existing MySQL schema; no DDL, indexes, constraints, or production schema files were changed.
- Schema verification: `DESCRIBE` and `SHOW INDEX` were run before writes for all touched tables with Chinese text handled through `python -X utf8`.
- Fixture prefix: `RRM-20260801-`.
- Formal source caveat: these fixtures do not create the formal activeOrderId/QA regulation schema required by M1-M4.

## Safety

- Scope limited to tenant `1` after explicit user authorization for this local test run.
- No rows were deleted.
- Existing user roles were preserved; `super_admin` was added for local test coverage only.
- Existing pressure pump route/version/process/batch-record bindings were read but not modified.
- Electronic signature fixture rows reuse an existing local test image file metadata; no historical signature records were forged.
- QC/IPQC fixture rows are task-prefixed and documented as local preflight fixtures, not formal QA regulation versions.

## Rollback

Rollback is task-scoped and can be performed by deleting or reverting rows with `creator/updater = 'codex-rrm'` or `RRM-20260801-` codes, then removing added `system_user_role` rows for role `1` from the six selected users if no longer needed. Password rollback would require a secure credential decision from the user because old password hashes were not recorded in task documents.

## BDD

BDD: M0 authorized local tenant fixture setup -> Given the user authorized local tenant `芋道源码`, six selected accounts, and pressure pump route V21 When local M0 fixtures are prepared Then ENV/RUNTIME preflight data for tenant/accounts/permissions/signatures/order/route/transfer/QC exists without claiming formal SOURCE blockers are solved.

## RED

RED: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> FAIL, expected reason: real E2E preflight did not yet require the explicit local baseline tenant authorization token.

RED: `pnpm e2e:role-requirement-matrix:real:check` -> FAIL, expected reason: user-authorized tenant `芋道源码` was still rejected by the ENV tenant guard before the explicit authorization token existed.

## GREEN

GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS.

GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS.

GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS.

GREEN: database verification query -> PASS; six users have enabled authorizations and active signature images, roles include `super_admin`, work order `980008`, transfers `1,2`, QC template `5`, and indicators `5,6,7` exist.

GREEN: `pnpm e2e:role-requirement-matrix:real:check` with redacted password env and explicit local tenant authorization -> EXPECTED_BLOCKED with 31 SOURCE blockers and no ENV/RUNTIME blockers.

## Verification

- Tenant: `芋道源码`, `status=0`, not deleted.
- Accounts: `liuyueyue`, `lvyujie`, `sunxiaoqing`, `shangmengying`, `huzonggang`, `zhengxiaofang`.
- Signature image IDs: `22`, `23`, `24`, `25`, `26`, `27`.
- Route: `922119 / RT000028 / 球囊扩张压力泵`; active version `448 / V21`.
- Process inspection binding: route processes `928609` and `928610` have `PROCESS_INSPECTION` / `过程检验记录`.
- Work order: `980008 / RRM-20260801-PP-MO-001`.
- Transfer IDs: `1,2`.
- QC template: `5 / RRM-20260801-IPQC-PRESSURE-PUMP`.
- Latest real preflight evidence: `doc/tasks/20260801-role-requirement-matrix-implementation/role-requirement-matrix-real-e2e-evidence.md`.

## Blockers

- M0 remains blocked by 31 SOURCE gaps in activeOrderId, PQC source, ERP transfer relations, QA regulation model, PQC task/piece model, production coefficient snapshots, batch record slot defaulting, and eDHR release sources.
- The local QC template fixture is not a formal QA regulation/version model.
- The local work order and transfer fixtures are not an activeOrderId relation source.
