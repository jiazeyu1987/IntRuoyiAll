# Execution Log

## Intent

- 用户要求：去除登录时候的图形化验证。
- 当前范围：登录页账号密码登录的图形验证码弹窗和对应 `captchaVerification` 依赖。

## Preflight

- 已读取 `docs/task-closeout-rules.md`。
- 已读取 `docs/frontend-development.md`。
- 已读取 `docs/backend-development.md`。
- 已读取 `frontend-feature-delivery` 与 `backend-api-delivery` 技能说明及 evidence contract。
- 已读取 `docs/experience-index.md`。
- Git 状态：`int_main...origin/int_main [ahead 17]`，存在大量非本任务改动；本任务只允许触碰登录验证码相关文件和本任务文档。

## BDD

- BDD: login without graphical captcha -> Given 登录页展示租户、用户名和密码输入框；When 用户点击登录；Then 不展示滑块/点选/图形验证码弹窗，直接调用 `/system/auth/login` 并保留真实错误返回。

## TDD

- RED: `node tests/e2e/login-captcha-disabled-static.spec.cjs` -> FAIL, `LoginForm must not render graphical Verify during account-password login.`
- RED: `mvn -pl yudao-module-system -am "-Dtest=AdminAuthServiceImplTest#testLogin_successWithoutCaptchaVerificationWhenCaptchaEnabled" test` -> FAIL, reactor 兄弟模块无匹配测试，按 Maven 门禁改用 `"-Dsurefire.failIfNoSpecifiedTests=false"` 复跑。
- RED: `mvn -pl yudao-module-system -am "-Dtest=AdminAuthServiceImplTest#testLogin_successWithoutCaptchaVerificationWhenCaptchaEnabled" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 旧 `login()` 仍调用验证码校验。
- GREEN: `node tests/e2e/login-captcha-disabled-static.spec.cjs` -> PASS。
- GREEN: `mvn -pl yudao-module-system -am "-Dtest=AdminAuthServiceImplTest#testLogin_successWithoutCaptchaVerificationWhenCaptchaEnabled" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- REGRESSION: `pnpm ts:check` -> PASS。
- REGRESSION: `mvn -pl yudao-module-system -am "-Dtest=AdminAuthServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，16 tests / 0 failures / 0 errors。

## Milestone Updates

- 登录页 `LoginForm.vue` 已删除 `Verify` 弹窗、`verify.value.show()` 和 `captchaVerification` 表单状态。
- `UserLoginVO.captchaVerification` 改为可选，账号密码登录 payload 不再注入该字段。
- 后端 `AdminAuthServiceImpl.login` 不再执行图形验证码校验，注册/重置相关验证码链路未改。
- 新增前端静态合同和后端定向单测覆盖登录无图形验证码。

## Verification Evidence

- `doc/tasks/20260731-remove-login-captcha/frontend-feature-evidence.md`
- `doc/tasks/20260731-remove-login-captcha/backend-api-evidence.md`
- `doc/tasks/20260731-remove-login-captcha/verification-report.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260731-remove-login-captcha/frontend-feature-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260731-remove-login-captcha/backend-api-evidence.md` -> PASS。

## Experience Consolidation

- 已读取 `project-experience-consolidation` 技能。
- 已将“登录图形验证码改动必须同时核对前端登录入口、登录 payload 和后端登录服务校验；账号登录范围不得扩大到注册/重置验证码链路”沉淀到 `docs/login-access.md#登录图形验证码链路门禁`。
- 已在 `docs/experience-index.md` 添加 `登录图形验证码 / captchaVerification / LoginForm Verify / AdminAuthServiceImpl.login` 关键词路由。

## Remaining Blockers

- Git closeout blocked: `int_main...origin/int_main [ahead 17]` 且工作区已有大量非本任务脏改动；当前任务未执行提交/推送，避免混入并行任务。
