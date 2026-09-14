# Backend API Evidence

## Endpoint And Service Scope

- Endpoint: `POST /system/auth/login`。
- Service: `AdminAuthServiceImpl.login(AuthLoginReqVO reqVO)`。

## API Contract And Data Contract

- 账号密码登录不再要求 `captchaVerification`。
- `captchaVerification` 字段仍保留在共享 VO 中，供注册、重置密码短信发送等仍需图形验证码的链路使用。

## Auth, Permissions, Validation, Error Behavior

- Auth: 登录成功仍由 `authenticate(username, password)` 校验账号、密码、禁用状态和密码过期状态。
- Validation: 用户名和密码原有 Bean Validation 不变。
- Error behavior: 账号不存在、密码错误、用户禁用和密码过期仍按原服务异常返回；未新增吞异常或默认成功。

## Required Config, Services, Fixtures, Migrations

- No schema migration.
- No external fixture.
- No runtime config fallback.

## BDD Scenarios

- BDD: login without graphical captcha -> Given 验证码总开关为 true 且登录请求不带 `captchaVerification`；When 用户名密码正确；Then 后端不调用 `CaptchaService`，返回登录 token。

## RED / GREEN Evidence

- RED: `mvn -pl yudao-module-system -am "-Dtest=AdminAuthServiceImplTest#testLogin_successWithoutCaptchaVerificationWhenCaptchaEnabled" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL,旧 `login()` 仍调用 `validateCaptcha(reqVO)` 并触发验证码依赖。
- GREEN: 同一 Maven 定向命令 -> PASS，1 test / 0 failures / 0 errors。
- REGRESSION: `mvn -pl yudao-module-system -am "-Dtest=AdminAuthServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，16 tests / 0 failures / 0 errors。

## Contract Or Integration Verification

- 新增 `testLogin_successWithoutCaptchaVerificationWhenCaptchaEnabled` 断言登录成功且 `verifyNoInteractions(captchaService)`。
- 保留注册验证码回归测试和既有 `validateCaptcha` 测试。

## Observability Touchpoints

- 登录成功日志仍通过 `createTokenAfterLoginSuccess` 记录。
- 验证码错误日志不再属于账号密码登录路径；注册/重置相关验证码错误路径未改动。

## Blockers And Downstream Skill Needs

- Git closeout blocked: 当前共享工作区存在非本任务脏改动与 ahead 状态，未提交未推送。
