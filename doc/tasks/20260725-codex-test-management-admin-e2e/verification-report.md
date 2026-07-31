# Verification Report

## Scope

本报告覆盖 `芋道源码/admin` 在本机 `int_main` 真实前端中查看 `系统管理 > 测试管理` 页签的权限修复与 Playwright E2E 验证。

## Root Cause

- 本机数据库未落入 `20260724_system_codex_test_management.sql` 中的测试管理权限迁移。
- 修复前只读 SQL 计数为 `codex_menu=0`、`codex_role=0`、`admin_role=0`，因此登录权限响应和动态菜单都不包含 `system:codex-test:query`。

## Fix Applied

- 在本机 `int-ruoyi-mysql` 容器执行既有幂等迁移 `IntRuoyiBackend/sql/mysql/20260724_system_codex_test_management.sql`。
- 修复后只读 SQL 计数为 `codex_menu=1`、`codex_permissions=7`、`codex_role=1`、`role_menu=7`、`admin_role=1`。
- 新增真实 E2E 脚本 `IntRuoyiFronted/tests/e2e/system-codex-test-management-real.e2e.js`，并增加 `pnpm e2e:system:codex-test-management:real` 命令。

## Passed Verification

- `node --check tests/e2e/system-codex-test-management-real.e2e.js` -> PASS。
- `pnpm e2e:system:codex-test-management:static` -> PASS。
- `pnpm e2e:system:codex-test-management:real` -> PASS。
- `python -X utf8 -m pytest script\tests\test_codex_test_management_migration.py -q` -> PASS，2 passed。
- `git diff --check` -> PASS，无 whitespace 错误。

## E2E Evidence

- 入口：`http://127.0.0.1:8081/login?redirect=/index`。
- 身份标签：`芋道源码/admin`。
- 权限响应：`hasCodexPermission=true`。
- 动态菜单：`hasCodexMenuContract=true`。
- 页面验证：`openedPage=true`，`/admin-api/system/codex-test-case/page` 返回业务码 0。
- 摘要文件：`doc/tasks/20260725-codex-test-management-admin-e2e/system-codex-test-management-real-summary.json`。

## Result

`芋道源码/admin` 已可在真实前端看到并进入 `系统管理 > 测试管理` 页签；本次未引入 fallback、mock、静默降级或前端绕过。
