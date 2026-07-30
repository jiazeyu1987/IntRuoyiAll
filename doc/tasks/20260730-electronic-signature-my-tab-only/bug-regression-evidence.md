# Bug Regression Evidence

## Bug Summary

普通用户进入电子签名时会默认落到“签名记录”页签，页面触发全量签名记录查询并展示“没有该操作权限”。期望行为是普通用户只看到“我的签名”。

## Expected

普通用户进入电子签名页面时只看到“我的签名”，且不触发全量签名记录或治理页签查询。

## Reproduction

- 打开普通用户电子签名入口。
- 旧实现根路由固定重定向到 `/signature-governance/signature-records`。
- 结果页面渲染签名记录列表并触发无权限接口响应。

## Root Cause

- 电子签名根路由固定重定向到 `/signature-governance/signature-records`。
- `permissionStore` 合并隐藏静态壳路由时，会把未授权的电子签名隐藏静态子路由补回普通用户路由表。
- 历史菜单授权曾让普通角色获得签名记录菜单，需要正式 SQL 收回普通角色治理页签范围。

## Regression Tests

- `IntRuoyiFronted/tests/e2e/electronic-signature-my-tab-only-static.spec.js`
- `IntRuoyiBackend/script/tests/test_signature_regular_users_my_signature_only_sql.py`

## RED

- `RED: node tests/e2e/electronic-signature-my-tab-only-static.spec.js -> FAIL, 管理页签未建正向授权集合且签名记录页签可直接 mount`
- `RED: python -m pytest script/tests/test_signature_regular_users_my_signature_only_sql.py -> FAIL, 缺少普通角色只保留“我的签名”的正式 SQL 迁移`

## Verification

- `GREEN: node tests/e2e/electronic-signature-my-tab-only-static.spec.js -> PASS`
- `GREEN: python -m pytest script/tests/test_signature_regular_users_my_signature_only_sql.py -> PASS`
- `GREEN: node --test scripts/signature-governance-page-contract.test.mjs -> PASS`
- `GREEN: pnpm ts:check -> PASS`
- `GREEN: python script/release/run-release-migration-policy-gate.py --sql-root sql/mysql -> PASS`

## Risk

管理员仍可访问治理页签；普通用户只保留根入口和“我的签名”。未执行真实登录 E2E，后续可在本机运行态可用时用普通账号补一次页面验证。

## Blockers

- Final commit/push closeout is blocked by unrelated ahead/dirty repository state.
