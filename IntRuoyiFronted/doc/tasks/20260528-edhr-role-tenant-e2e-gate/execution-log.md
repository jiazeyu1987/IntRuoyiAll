# Execution Log

## 2026-05-28 Main Reviewer Planning

BDD: readonly users cannot write -> Given a readonly test-tenant eDHR account; When it navigates through eDHR readonly pages; Then no eDHR write request is sent and pages render or fail explicitly.

BDD: no-permission users fail visibly -> Given a test-tenant account without eDHR role menus; When it attempts to open eDHR pages; Then unauthorized/forbidden/no-menu evidence is captured and success is not reported.

BDD: role-specific access is separated -> Given executor, approver, and QA/archive accounts in tenant 122; When each opens its allowed eDHR page; Then the allowed page renders under that account.

BDD: formal admin is readonly only -> Given formal `芋道源码/admin`; When formal coverage runs; Then a write guard blocks any eDHR mutating request.

BDD: tenant fixture setup is explicit -> Given the fixture script is run without `--apply`; When it plans tenant 122 role setup; Then it writes nothing and reports dry-run output.

RED: `node --test scripts\edhr-permission-tenant-matrix-contract.test.mjs` -> expected FAIL, contract test does not exist yet.

RED: `pnpm e2e:edhr:permission-matrix:check` -> expected FAIL, package script and E2E file do not exist yet.

Current status: planning complete; implementation worker pending.

## 2026-05-28 Implementation Worker Evidence

RED: `node --test scripts\edhr-permission-tenant-matrix-contract.test.mjs` -> FAIL, contract test file did not exist before implementation.

RED: `pnpm e2e:edhr:permission-matrix:check` -> FAIL, package script `e2e:edhr:permission-matrix:check` was not registered before implementation.

GREEN: `node --test scripts\edhr-permission-tenant-matrix-contract.test.mjs` -> PASS, package scripts, E2E guards, fixture dry-run/apply guard, tenant 122 guard, write guard, and no-permission explicit evidence contract verified.

GREEN: `pnpm e2e:edhr:permission-matrix:check` -> PASS, `tests/e2e/edhr-permission-tenant-matrix.e2e.js` syntax check passed.

GREEN: `node --check doc\tasks\20260528-edhr-role-tenant-e2e-gate\scripts\prepare-edhr-role-matrix-fixtures.cjs` -> PASS, fixture script syntax check passed.

GREEN: `node doc\tasks\20260528-edhr-role-tenant-e2e-gate\scripts\prepare-edhr-role-matrix-fixtures.cjs` -> PASS, dry-run only; tenantId=122, tenantName=测试租户, 5 matrix accounts/roles planned, eDHR menu IDs resolved, `writesPerformed=false`, password hash not printed.

BLOCKED: `pnpm e2e:edhr:permission-matrix` -> NOT RUN, environment contains no `EDHR_MATRIX_*` variables; real UI E2E requires `EDHR_MATRIX_BASE_URL`, `EDHR_MATRIX_TENANT`, `EDHR_MATRIX_TENANT_ID`, `EDHR_MATRIX_EXECUTION_ID`, `EDHR_MATRIX_EXECUTION_CODE`, executor/approver/archiver/readonly/denied/admin usernames and passwords. Impact: cannot safely log in through real UI or generate role/tenant matrix runtime evidence; no mock, fallback, test button, or API shortcut was used.

## 2026-05-28 Reviewer Round 1 Fix Evidence

RED: Reviewer round 1 -> FAIL, formal admin reused test tenant runtime, fixture role_menu used permission lookup instead of explicit dynamic page menu IDs, and executor/approver/archiver allowed-path smoke accepted explicit permission errors.

GREEN: `node --test scripts\edhr-permission-tenant-matrix-contract.test.mjs` -> PASS, contract now asserts admin base/tenant/user/password env names, explicit menu IDs `5100`, `5700`, `900023`, `900024`, and allowed-path `requireRendered` guard.

GREEN: `pnpm e2e:edhr:permission-matrix:check` -> PASS, Playwright E2E syntax check passed after splitting formal admin runtime and enforcing rendered allowed-path smoke.

GREEN: `node doc\tasks\20260528-edhr-role-tenant-e2e-gate\scripts\prepare-edhr-role-matrix-fixtures.cjs` -> PASS, dry-run only; explicit menu IDs resolved for tenantId=122, including parent menus `5100`, `5700`, executor/readonly/archiver page `900023`, approver page `900024`, readonly pages `900025`, `900026`, and role-specific function menus; `writesPerformed=false`, password hash not printed.

## 2026-05-28 Reviewer Round 2 Fix Evidence

RED: Reviewer round 2 -> FAIL, executor/approver/archiver allowed-path smoke rendered checks did not also install the eDHR write guard, so potential eDHR mutating requests during smoke navigation were not blocked.

GREEN: `node --test scripts\edhr-permission-tenant-matrix-contract.test.mjs` -> PASS, contract now asserts allowed role smoke calls `runRoute` with both `writeGuard: true` and `requireRendered: true`.

GREEN: `pnpm e2e:edhr:permission-matrix:check` -> PASS, Playwright E2E syntax check passed after enabling write guard for executor/approver/archiver smoke routes.

GREEN: `node doc\tasks\20260528-edhr-role-tenant-e2e-gate\scripts\prepare-edhr-role-matrix-fixtures.cjs` -> PASS, dry-run only; tenantId=122 explicit role/menu plan still resolves and reports `writesPerformed=false`, password hash not printed.

## 2026-05-28 Reviewer Round 3 Fix Evidence

RED: Reviewer round 3 real E2E/login API probe -> FAIL, first matrix login timed out waiting for `/index`; direct login API rejected `edhr_matrix_executor` with username regex `^[a-zA-Z0-9]{4,30}$` and message “账号格式为数字以及字母”.

GREEN: `node --test scripts\edhr-permission-tenant-matrix-contract.test.mjs` -> PASS, contract now verifies fixture/E2E target matrix usernames are alphanumeric only (`edhrmatrixexecutor`, `edhrmatrixapprover`, `edhrmatrixarchiver`, `edhrmatrixreadonly`, `edhrmatrixdenied`) and contain no underscores; fixture also keeps explicit legacy underscore migration logic.

GREEN: `pnpm e2e:edhr:permission-matrix:check` -> PASS, Playwright E2E syntax check passed after switching default matrix usernames to alphanumeric values.

GREEN: `node doc\tasks\20260528-edhr-role-tenant-e2e-gate\scripts\prepare-edhr-role-matrix-fixtures.cjs` -> PASS, dry-run only; target usernames are alphanumeric, existing legacy underscore accounts were detected for tenantId=122, apply migration is planned without printing password hash, `writesPerformed=false`.

## 2026-05-28 Reviewer Round 4 Fix Evidence

RED: Reviewer round 4 fixture apply -> FAIL, `node doc/tasks/20260528-edhr-role-tenant-e2e-gate/scripts/prepare-edhr-role-matrix-fixtures.cjs --apply` failed with MySQL `ERROR 1062 (23000): Duplicate entry '1' for key 'system_role.PRIMARY'`; root cause was aggregate `MAX(id)+1` inserts with direct `WHERE` predicates emitting one NULL aggregate row when insert conditions were false.

GREEN: `node --test scripts\edhr-permission-tenant-matrix-contract.test.mjs` -> PASS, contract now asserts `system_role`, `system_users`, `system_user_role`, and `system_role_menu` id allocation uses derived seed subqueries and rejects direct aggregate `MAX(id)+1 ... FROM <table> WHERE` insert patterns.

GREEN: `pnpm e2e:edhr:permission-matrix:check` -> PASS, matrix E2E syntax check still passes after fixture-only SQL safety fix.

GREEN: `node doc\tasks\20260528-edhr-role-tenant-e2e-gate\scripts\prepare-edhr-role-matrix-fixtures.cjs` -> PASS, dry-run only; tenantId=122 plan still resolves legacy users/roles and explicit eDHR menu IDs, `writesPerformed=false`, password hash not printed.

## 2026-05-28 Reviewer Round 5 Fix Evidence

RED: Reviewer round 5 real login API -> FAIL, after successful fixture `--apply` and DB verification, matrix users in tenantId=122 had correct roles/menu bindings but `system_users.password_update_time` was NULL; backend rejected `edhrmatrixexecutor` with “密码已过期，请修改密码后再登录” while source user `aoteman.password_update_time` was populated.

GREEN: `node --test scripts\edhr-permission-tenant-matrix-contract.test.mjs` -> PASS, contract now asserts legacy migration, user creation, and rerun-update paths set or repair `password_update_time`.

GREEN: `pnpm e2e:edhr:permission-matrix:check` -> PASS, matrix E2E syntax check still passes after fixture password timestamp safeguard.

GREEN: `node doc\tasks\20260528-edhr-role-tenant-e2e-gate\scripts\prepare-edhr-role-matrix-fixtures.cjs` -> PASS, dry-run only; tenantId=122 plan still resolves matrix accounts/roles and explicit eDHR menu IDs, `writesPerformed=false`, password hash not printed.

## 2026-05-28 Main Reviewer Final Gate

GREEN: `node --test scripts\edhr-permission-tenant-matrix-contract.test.mjs` -> PASS, main reviewer reran contract checks after Round 5.

GREEN: `pnpm e2e:edhr:permission-matrix:check` -> PASS, main reviewer reran E2E syntax check after Round 5.

GREEN: `node doc\tasks\20260528-edhr-role-tenant-e2e-gate\scripts\prepare-edhr-role-matrix-fixtures.cjs` -> PASS, dry-run only; tenantId=122, `writesPerformed=false`, no password hash printed.

GREEN: `node doc\tasks\20260528-edhr-role-tenant-e2e-gate\scripts\prepare-edhr-role-matrix-fixtures.cjs --apply` -> PASS, fixture applied only to tenantId=122 and repaired matrix users' `password_update_time`.

GREEN: SQL verification -> PASS, tenantId=122 has five alphanumeric matrix users, no active legacy underscore users, five non-null `password_update_time` values, exact matrix user-role bindings, and denied role has zero active eDHR menu bindings.

GREEN: login API probe -> PASS, `edhrmatrixexecutor` authenticated in tenantId=122 and returned an access token; token value was not recorded.

GREEN: `pnpm e2e:edhr:permission-matrix` -> PASS, real UI E2E completed for executor, approver, archiver, readonly, denied, and formal admin readonly smoke. All recorded write guards were `clean`; denied path produced `explicit-permission-block`.

GREEN: Independent tester -> PASS, subagent `019e6c5e-d382-7f83-b7ca-e807fb169ba3` wrote `doc/tasks/20260528-edhr-role-tenant-e2e-gate/test-report.md` and found no release blocker for this gate.
