# Pre-login Authentication Error Normalization

## Current Status
blocked

实现及指定离线验证已通过；仓库收尾因本轮明确禁止 Git 操作而阻塞。未提交、未推送，也未运行会改动仓库资产的 cleanup apply。

## Goal

Normalize externally visible authentication failures from the public pre-login password-change path without changing ordinary login behavior. Keep internal login-result audit classifications distinct and ensure rejected requests cannot change credentials, clear an active lock, or issue a token.

## BDD

- BDD: pre-login account-state privacy -> Given requests in the same tenant for an unknown username, a disabled account, an actively locked account, an incorrect old password, or an ACTIVE non-expired account, When `change-password-before-login` is called, Then every request returns the same `AUTH_LOGIN_BAD_CREDENTIALS` code and message.
- BDD: ordinary login compatibility -> Given the same account states, When ordinary login authentication runs, Then unknown/incorrect credentials remain `AUTH_LOGIN_BAD_CREDENTIALS`, disabled remains `AUTH_LOGIN_USER_DISABLED`, locked remains `AUTH_LOGIN_USER_LOCKED`, and ACTIVE non-expired remains eligible for ordinary login.
- BDD: private audit classification and no side effects -> Given any rejected pre-login request, When authentication rejects it, Then persisted login-result audit remains `BAD_CREDENTIALS`, `USER_DISABLED`, `USER_LOCKED`, or `PASSWORD_CHANGE_NOT_ALLOWED` as applicable; no rejected request updates a password, resets an active lock counter, or creates a token.
- BDD: no secret in ordinary logs -> Given any authentication attempt, When application logging is inspected, Then this service does not log the supplied username or either password as ordinary application log content.

## Milestones

1. Record BDD and add service/controller tests that fail against the current behavior (RED) - complete.
2. Normalize only the `preLoginPasswordChange=true` outward error while preserving internal login-result logging and ordinary login errors (GREEN) - complete.
3. Run the requested focused Maven tests and `git diff --check`; review the diff and document residual risks - complete.
4. Complete repository-required Git and cleanup closeout - blocked by the explicit no-Git scope for this turn.

## Expected Verification

- RED: `mvn -pl yudao-module-system -am "-Dtest=AdminAuthServiceImplTest,AuthControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` fails on pre-login external code/message assertions before the implementation change.
- GREEN/REGRESSION: the same Maven command passes and executes both named test classes.
- `git diff --check` passes (read-only diff validation only; no Git mutation commands).
- Review confirms no production changes outside `AdminAuthServiceImpl.java` and no test changes outside `AdminAuthServiceImplTest.java` and `AuthControllerTest.java`.
- `verification-report.md` records functional results, residual timing-side-channel risk, and closeout status.

## Design Constraints

- Write scope: `AdminAuthServiceImpl`, its service/controller tests, only necessary auth error/login-result enums, and this task directory.
- Reuse `AUTH_LOGIN_BAD_CREDENTIALS` for normalized pre-login failures; do not change ordinary login error semantics.
- Preserve persisted login-result distinctions and existing wrong-password failure accounting.
- Rejected requests must not update a password or issue a token. An actively locked account must not have its lock counters reset by this endpoint.
- Do not add ordinary logs containing usernames or passwords. This task does not claim constant-time behavior and does not test timing side channels.
- No database writes outside isolated unit-test infrastructure, no Git mutations, and no service start/stop/restart.
