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
- `mes_pqc_inspection_task`: task-owned pending PQC task for route process `928609`.
- `mes_qa_inspection_regulation_item_equipment`: task-owned equipment option for existing published regulation version `16`, required because the current QA item requires equipment.
- `mes_pro_process_pool_team_leader_scope`: task-owned PQC personnel binding for the PQC E2E user.

## Data Safety

- All inserted or updated rows use task marker `20260804-pqc-fill-fullscreen-toggle` in `creator`, `updater`, `remark`, code, or username where the table supports it.
- No schema changes are performed.
- No production or remote database is touched.
- Rollback is explicit: delete task-owned rows by marker and restore no shared rows except the task-owned inserts.

## BDD

- BDD: PQC E2E precondition -> Given local tenant 1 has a PQC E2E user, active order, published QA regulation item, and pending PQC task, When the PQC user opens PQC填写, Then the page can select/order-load without `当前没有活跃订单，PQC 不能选择订单`.

## RED

- RED: `workdir=E:\IntRuoyi; node doc\tasks\20260804-pqc-fill-fullscreen-toggle\pqc-fill-fullscreen-real.e2e.cjs` -> FAIL, expected reason before fixture: page error `当前没有活跃订单，PQC 不能选择订单`.

## GREEN

- Pending: fixture creation and real E2E rerun.

## Verification

- Pending: MySQL row-count verification for task marker.
- Pending: real Playwright E2E rerun with `PQC_FULLSCREEN_E2E_USERNAME`.

## Rollback

- Delete task-owned `mes_pqc_inspection_task`, `mes_pro_process_pool_active_order`, `mes_pro_work_order`, `mes_qa_inspection_regulation_item_equipment`, `mes_pro_process_pool_team_leader_scope`, `system_user_role`, and `system_users` rows using the task marker and generated IDs.

## Blockers

- None yet.
