# eDHR Role/Tenant Matrix E2E Evidence

- Task ID: `20260528-edhr-role-tenant-e2e-gate`
- Generated at: 2026-05-30T16:23:34.903Z
- Worktree: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Command: `pnpm e2e:edhr:permission-matrix`
- Check command: `pnpm e2e:edhr:permission-matrix:check`
- Result JSON: `test-results/edhr-permission-tenant-matrix/result.json`
- Status: PASS

## BDD

- BDD: readonly users cannot write -> Given readonly account, When it opens eDHR readonly pages, Then write guard observes zero eDHR POST/PUT/PATCH/DELETE requests.
- BDD: no-permission users fail visibly -> Given denied account, When it opens eDHR pages, Then explicit 403/no-permission/no-menu/404 evidence is captured.
- BDD: role-specific access is separated -> Given executor, approver, and archiver accounts, When each opens its role page, Then the UI must render recognizable eDHR text under that account.
- BDD: formal admin is readonly only -> Given formal admin account, When it performs smoke coverage, Then write guard prevents every eDHR mutating request.

## GREEN

- GREEN: `pnpm e2e:edhr:permission-matrix` -> PASS, role/tenant matrix real UI gate completed.
- executor /mes/pro/feedback/edhr-execution/detail?id=56 -> rendered; writeGuard=clean
- approver /mes/pro/feedback/edhr-approval?executionCode=BRE202605281813460410056 -> rendered; writeGuard=clean
- archiver /mes/pro/feedback/edhr-execution/detail?id=56 -> rendered; writeGuard=clean
- readonly /mes/pro/feedback/edhr-execution/detail?id=56 -> rendered; writeGuard=clean
- readonly /mes/pro/feedback/edhr-tracking?executionCode=BRE202605281813460410056 -> rendered; writeGuard=clean
- readonly /mes/pro/feedback/edhr-signatures?executionId=56 -> rendered; writeGuard=clean
- readonly /mes/pro/feedback/edhr-field-audit?executionId=56 -> rendered; writeGuard=clean
- readonly /mes/pro/feedback/edhr-domain-trace/detail?executionId=56&executionCode=BRE202605281813460410056 -> rendered; writeGuard=clean
- denied /mes/pro/feedback/edhr-execution/detail?id=56 -> explicit-permission-block; writeGuard=clean
- admin /mes/pro/feedback/edhr-execution/detail?id=56 -> rendered; writeGuard=clean
