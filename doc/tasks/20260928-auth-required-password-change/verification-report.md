# Verification Report

## Result

**Implementation: PASS. Closeout: BLOCKED by explicit no-Git constraint.**

管理员重置后，用户现在可从登录页用旧密码通过公开 pre-login endpoint 改密。该入口沿用登录相同租户上下文、用户启用检查、活动锁检查、旧密码匹配、失败计数及失败登录记录。只允许 `INITIAL`、`RESET_REQUIRED` 或现有正式过期策略判定为过期的账号；`ACTIVE` 未过期账号拒绝。密码更新调用既有正式用户服务。

## Security Checks

- Pre-login controller is `@PermitAll`, consumes a POST body, and returns only `CommonResult<Boolean>`.
- Request explicitly disables token attachment on the frontend; backend path does not call token creation. Controller and service tests verify no token/user-session service is invoked.
- Password history insertion and credential update are in `@Transactional(rollbackFor = Exception.class)`; the service first locks the current user row with `selectByIdForUpdate`, then revalidates the old password, applies strength/history policy, records history, and updates credential status to `ACTIVE`.
- Unknown user and wrong old password both return `AUTH_LOGIN_BAD_CREDENTIALS`; a known user with an incorrect password reuses login failure accounting and login-log recording.
- Frontend forced-change flow requires current/new/confirm password, shares `systemPasswordRule`, passes `rememberMe: false` to normal login, clears password refs after a submitted change attempt, and only stores a token after normal login succeeds.
- Login cache storage now excludes the password property entirely; legacy cached passwords are cleared and rewritten without password. New endpoint sends credentials only in a POST body, never URL/query, localStorage, or sessionStorage.

## Validation

### Real-page E2E (2026-09-28)
- Owner A and Owner B each entered the required-change form through the real tenant login page.
- For both identities, the browser-observed `/admin-api/system/auth/change-password-before-login` request and the subsequent normal `/admin-api/system/auth/login` request returned HTTP 200 with business code 0. The page left `/login` and reached `/index`.
- Each identity then used a fresh Playwright context and only the new password to log in again; both normal login requests returned HTTP 200/code 0 and reached `/index`.
- No password-named key was found in localStorage or sessionStorage after login. Passwords were not written to screenshots, result JSON, logs, or reports.
- Evidence: `doc/tasks/20260928-dcc-owner-permission-e2e/run-2026-09-27T22-20-34-232Z/ownerA-auth-result.json`, `ownerB-auth-result.json`, and their fresh-login screenshots.
- Live auth E2E: PASS. Task closeout remains BLOCKED only by the earlier explicit no-Git constraint; no Git action was performed in this turn.

- Maven command: `mvn -pl yudao-module-system -am "-Dtest=AdminAuthServiceImplTest,AuthControllerTest,AdminUserServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Result: BUILD SUCCESS; 125 tests, 0 failures, 0 errors, 0 skipped. Includes INITIAL and RESET_REQUIRED credential transitions, expired/ACTIVE route conditions, disabled/locked users, wrong and unknown credentials, failure-count/log delegation, no token issuance, weak password and password-history reuse rules.
- Frontend static: `node tests/e2e/login-required-password-change-static.spec.cjs` -> PASS.
- Login regressions: `node tests/e2e/login-default-credentials-static.spec.mjs` -> exit 0; `node tests/e2e/login-auth-error-message-static.spec.js` -> PASS.
- TypeScript: `pnpm ts:check` -> exit 0.
- Maven Surefire reports: `IntRuoyiBackend/yudao-module-system/target/surefire-reports/`.
- Console output was not redirected to a separate log file. Task command/results are recorded in `execution-log.md`.

## Not Run

- Service lifecycle operations were not run. The user supplied a running test runtime, and this turn used its real frontend/backend through the browser without starting, stopping, or restarting either service.
- No external or operational database was connected to or modified. The existing service unit-test suite used its isolated test database to verify password/status/history persistence; these test transactions are not live-environment changes.
- `git diff --check`, Git status, and task cleanup: omitted because the user explicitly prohibited Git operations. This also prevents the repository-required cleanup/closeout sequence; task status remains `blocked` for closeout only.

## Residual Risk

The owner permission-boundary test found no in-scope controlled file or pending task for either owner, so it does not extend auth verification beyond successful authentication. Runtime health and service lifecycle were not independently operated in this turn; the explicitly authorized browser test used the already-running 8062/48062 runtime.
