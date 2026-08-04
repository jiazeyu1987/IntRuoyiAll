# Database Fixture Evidence

## Goal

Create local, task-owned PQC E2E preconditions so the real PQC填写 fullscreen path can be validated with a PQC identity instead of admin.

## Engine And Tooling

- Database engine: MySQL 8.0.39 in local Docker container `int-ruoyi-mysql`.
- Migration tool: none for this task; this is a reversible local test fixture only, not a release migration.
- Connection source: local `application-local.yaml`; credentials are read at runtime and not recorded.

## Affected Entities

- `system_users` / `system_user_role`: task-owned PQC E2E login identity.
- `mes_pro_work_order`: task-owned work order using existing product `902149`.
- `mes_pro_process_pool_active_order`: task-owned active order for existing route `922119` and route version `448`.
- `mes_pqc_inspection_task`: 14 task-owned pending PQC tasks, one for each published route process under the selected route/version.
- `mes_qa_inspection_regulation_item_equipment`: task-owned equipment options for all equipment-required PATROL items under the selected published regulations.
- `mes_pro_process_pool_team_leader_scope`: task-owned PQC personnel binding for the PQC E2E user.

## Data Safety

- All inserted or updated rows use task marker `20260804-pqc-fill-fullscreen-toggle` in `creator`, `updater`, `remark`, code, or username where the table supports it.
- No schema changes are performed.
- No production or remote database is touched.
- The fixture remains in the local database because the user explicitly requested the PQC E2E condition to be created and configured.
- Rollback is explicit: delete task-owned rows by marker and restore no shared rows except the task-owned inserts.

## BDD

- BDD: PQC E2E precondition -> Given local tenant 1 has a PQC E2E user, active order, published QA regulation item, and pending PQC task, When the PQC user opens PQC填写, Then the page can select/order-load without `当前没有活跃订单，PQC 不能选择订单`.

## RED

- RED: `workdir=E:\IntRuoyi; node doc\tasks\20260804-pqc-fill-fullscreen-toggle\pqc-fill-fullscreen-real.e2e.cjs` -> FAIL, expected reason before fixture: page error `当前没有活跃订单，PQC 不能选择订单`.

## GREEN

- GREEN: local MySQL fixture transaction -> PASS, task-owned user `914524`, work order `980019`, active order `30`; 14 PENDING tasks cover 14 route processes, 32 task-owned equipment options exist, and missing task count is `0`.
- GREEN: `PQC_FULLSCREEN_E2E_USERNAME=pqce2efullscreen; node doc\tasks\20260804-pqc-fill-fullscreen-toggle\pqc-fill-fullscreen-real.e2e.cjs` -> PASS, identity `芋道源码/pqce2efullscreen`.

## Verification

- MySQL 8.0.39 local Docker connection: PASS.
- Fixture counts: user `1`, super-admin role binding `1`, PQC personnel scope `1`, work order `1`, ACTIVE order `1`, PENDING task count `14`, distinct route-process count `14`.
- Post-E2E data check: all 14 task-owned tasks remain `PENDING`; task-owned PQC piece detail count is `0`, proving the fullscreen test did not submit inspection data.
- Playwright result: `output/playwright/20260804-pqc-fill-fullscreen-toggle/result.json` reports `status=PASS`, `pqcSubmitRequests=[]`, `pageErrors=[]`, `consoleErrors=[]`, and `targetFailures=[]`.

## Rollback

- Delete task-owned `mes_pqc_inspection_task`, `mes_pro_process_pool_active_order`, `mes_pro_work_order`, `mes_qa_inspection_regulation_item_equipment`, `mes_pro_process_pool_team_leader_scope`, `system_user_role`, and `system_users` rows using the task marker and generated IDs.

## Blockers

- Formal repository closeout remains blocked by unrelated dirty workspace/branch state; the local fixture and real E2E are complete.
