# DCC admin 测试租户访问入口执行日志

## BDD

- BDD: admin 可通过真实页面访问测试租户 -> Given 用户具备 `system:tenant:visit` 或全权限 When 用户登录 `芋道源码/admin` Then 顶栏显示租户访问选择器并可选择测试租户。
- BDD: 无租户访问权限不显示入口 -> Given 用户不具备 `system:tenant:visit` 且不是全权限 When 用户进入后台 Then 顶栏不显示租户访问选择器。

## Evidence

- 2026-05-27：测试服 Playwright 只读 E2E 输出 `VISIT_TENANT_SELECTOR_MISSING`，导致 DCC 查询仍落在 tenant 1，签名记录为 0。

## TDD

- RED: `node --test scripts\dcc-tenant-visit-header.test.mjs` -> FAIL, expected reason: `ToolHeader.vue` 未导入/渲染 `TenantVisit`，也没有 `system:tenant:visit` 权限门禁。
- GREEN: `node --test scripts\dcc-tenant-visit-header.test.mjs` -> PASS, 2 tests.
- GREEN: `pnpm exec eslint src/layout/components/ToolHeader.vue scripts/dcc-tenant-visit-header.test.mjs` -> PASS.
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS.
- RED: `node --test scripts\dcc-tenant-visit-header.test.mjs` -> FAIL, expected reason: 门禁未显式覆盖 `super_admin` 角色，真实 admin 权限接口包含该角色但顶栏仍未显示租户访问入口。
- GREEN: `node --test scripts\dcc-tenant-visit-header.test.mjs` -> PASS after adding `super_admin` role gate.
- GREEN: `pnpm exec eslint src/layout/components/ToolHeader.vue scripts/dcc-tenant-visit-header.test.mjs` -> PASS after role gate update.
GREEN: `node --test scripts\dcc-tenant-visit-header.test.mjs` -> PASS, 2 tests passed.

GREEN: `pnpm exec eslint src/layout/components/ToolHeader.vue scripts/dcc-tenant-visit-header.test.mjs` -> PASS.

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS.

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; $env:VITE_BASE_URL='http://172.30.30.58:48081'; $env:VITE_BASE_PATH='/'; $env:VITE_OUT_DIR='dist-intruoyi-test'; pnpm build:prod` -> PASS.

GREEN: 测试服前端镜像 `intruoyi-frontend:20260527_dcc_admin_e2e_repair` 发布并重建容器 -> PASS，`http://172.30.30.58:8081` 可访问。

GREEN: 后端任务脚本 `$env:DCC_ADMIN_E2E_REQUIRE_VALID_SIGNATURE='true'; node doc\tasks\20260527-dcc-admin-e2e-repair\scripts\dcc-admin-readonly-e2e.mjs` -> PASS，`芋道源码/admin` 顶栏选择 `测试租户` 后 `visitTenantId=122`，输出 `YUDAO_ADMIN_DCC_SIGNATURE_PASS`，DCC 写请求数为 0。

GREEN: `git merge --ff-only codex/20260527-dcc-admin-e2e-repair` on frontend `int_main` -> PASS, frontend main reached `fd0d1be9`; backend `int_main` reached `4871b0f02c` after backend fast-forward integration.

GREEN: `node --test scripts\dcc-tenant-visit-header.test.mjs` on frontend `int_main` -> PASS, 2 tests passed.

GREEN: 后端主干脚本 `$env:DCC_ADMIN_E2E_REQUIRE_VALID_SIGNATURE='true'; node doc\tasks\20260527-dcc-admin-e2e-repair\scripts\dcc-admin-readonly-e2e.mjs` -> PASS，`芋道源码/admin` 选择 `测试租户` 后输出 `YUDAO_ADMIN_DCC_SIGNATURE_PASS`，`mutatingDccRequests=0`。

GREEN: `git merge-base --is-ancestor 39bceda2 int_main` after subsequent NAS fast-forward -> PASS, DCC frontend closeout commit remains included in latest `int_main`.

GREEN: `node --test scripts\dcc-tenant-visit-header.test.mjs` on latest frontend `int_main` -> PASS, 2 tests passed.

GREEN: 后端最新主干脚本 `$env:DCC_ADMIN_E2E_REQUIRE_VALID_SIGNATURE='true'; node doc\tasks\20260527-dcc-admin-e2e-repair\scripts\dcc-admin-readonly-e2e.mjs` -> PASS，`芋道源码/admin` 选择 `测试租户` 后输出 `YUDAO_ADMIN_DCC_SIGNATURE_PASS`，`mutatingDccRequests=0`。
