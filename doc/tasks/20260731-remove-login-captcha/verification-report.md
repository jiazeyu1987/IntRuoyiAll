# Verification Report

## Summary

- 登录页账号密码登录已移除图形验证码弹窗。
- 后端账号密码登录已移除 `captchaVerification` 校验依赖。
- 注册、重置密码短信等其它图形验证码链路未纳入本次修改。

## Commands

- RED: `node tests/e2e/login-captcha-disabled-static.spec.cjs` -> FAIL, 旧登录页仍渲染 `Verify`。
- RED: `mvn -pl yudao-module-system -am "-Dtest=AdminAuthServiceImplTest#testLogin_successWithoutCaptchaVerificationWhenCaptchaEnabled" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 旧后端登录仍调用验证码校验。
- GREEN: `node tests/e2e/login-captcha-disabled-static.spec.cjs` -> PASS。
- GREEN: `mvn -pl yudao-module-system -am "-Dtest=AdminAuthServiceImplTest#testLogin_successWithoutCaptchaVerificationWhenCaptchaEnabled" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- REGRESSION: `pnpm ts:check` -> PASS。
- REGRESSION: `mvn -pl yudao-module-system -am "-Dtest=AdminAuthServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 16 tests / 0 failures / 0 errors。
- EVIDENCE: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260731-remove-login-captcha/frontend-feature-evidence.md` -> PASS。
- EVIDENCE: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260731-remove-login-captcha/backend-api-evidence.md` -> PASS。
- STRUCTURAL: `git diff --check -- <current task files>` -> PASS。

## Scope Check

- Frontend changed files: `LoginForm.vue`, `src/api/login/types.ts`, task static contract.
- Backend changed files: `AdminAuthServiceImpl.java`, `AdminAuthServiceImplTest.java`.
- Experience docs: `docs/login-access.md`, `docs/experience-index.md` keyword route.
- No DB migration, menu change, tenant data change, runtime restart, or remote server operation.

## Remaining Blockers

- Closeout commit/push not performed because the shared `int_main` worktree is pre-existing dirty and ahead of `origin` with many unrelated task files. Committing now would risk mixing concurrent work into this task.
