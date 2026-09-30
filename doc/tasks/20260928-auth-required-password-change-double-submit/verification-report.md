# Verification Report

## Result

强制改密已改为单一原生表单 submit 入口，具备同步 pending guard；一次点击或 Enter 只会进入一个 submit handler，重复 submit 在等待 tenant、表单校验或写请求之前即返回。

## BDD

- Given 强制改密表单已显示且没有提交中的请求；When 点击 submit 按钮或按 Enter；Then 两者均触发表单 submit handler，调用写接口一次。
- Given 改密请求正在处理；When 再次触发 submit；Then pending guard 拒绝第二次调用。
- 改密成功后仍固定以 `rememberMe=false` 调用正常登录；仅正常登录成功后保存 token。

## RED/GREEN

- RED：`node tests/e2e/login-required-password-change-static.spec.cjs` -> FAIL；原组件缺少按钮原生 submit 与 pending guard，并存在独立 click/Enter handler。
- GREEN：同一命令 -> PASS：`PASS: required pre-login password change static contract`。

## Regression

- `pnpm ts:check` -> PASS。
- `login-auth-error-message-static.spec.js` -> PASS。
- `login-captcha-disabled-static.spec.cjs` -> PASS。
- `login-default-credentials-static.spec.mjs` -> PASS。
- `login-required-password-change-static.spec.cjs` -> PASS。
- `auth-refresh-token-business-failure-static.spec.js` -> PASS。
- `mes-frontline-pqc-login-employee-lock-static.spec.cjs` -> PASS。
- `system-login-security-idle-logout-static.spec.js` -> PASS。
- `git diff --check` -> PASS (exit 0); only line-ending conversion warnings were emitted.
- Five unrelated visual login static contracts fail because they still require the absent `<Verify` captcha component: `login-interventional-white-background-static.spec.js`, `login-medical-tech-background-static.spec.js`, `login-remove-left-panel-content-static.spec.js`, `login-remove-redbox-content-static.spec.js`, `login-remove-top-left-logo-static.spec.js`. They were not changed because captcha/login visual behavior is outside this task's write set.

## Scope And Safety

- Production source changed only in `IntRuoyiFronted/src/views/Login/components/LoginForm.vue`; static contract changed only in `IntRuoyiFronted/tests/e2e/login-required-password-change-static.spec.cjs`.
- No backend, database, service, browser session, or Git write operation was used. `git diff --check` was run as explicitly requested.
- Existing security contracts remain asserted: attempted submissions clear password fields in `finally`; current password remains in reactive in-memory form state; changed-password normal login uses `rememberMe=false`; token persistence remains inside successful normal login completion; password is not persisted in URL or browser storage.
- No live browser E2E was run; verification is the requested static-contract and TypeScript validation.

## Current Status

blocked

Implementation and requested verification are complete. Git-based cleanup/closeout is not performed because the user prohibited Git operations.
