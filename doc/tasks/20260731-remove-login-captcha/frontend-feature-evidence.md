# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 账号密码登录点击登录或回车时不再展示滑块、点选或图形验证码，直接进入正式登录接口。
- Non-goal: 不调整注册、忘记密码、短信验证码或其它非账号密码登录链路。

## Requirements And Acceptance

- Acceptance: 登录页不渲染 `Verify` 图形验证码组件。
- Acceptance: 登录按钮不调用 `verify.value.show()`。
- Acceptance: 登录 payload 不再注入 `captchaVerification`。
- Acceptance: 登录接口错误继续通过 `loginErrorMessage` 可见，不吞异常或默认成功。

## UI Entry Points And Owned Files

- Entry point: `IntRuoyiFronted/src/views/Login/components/LoginForm.vue`。
- API type: `IntRuoyiFronted/src/api/login/types.ts`。
- Static contract: `IntRuoyiFronted/tests/e2e/login-captcha-disabled-static.spec.cjs`。

## API Contracts And Data States

- `UserLoginVO.captchaVerification` 从必填改为可选。
- 登录表单状态只保留租户、用户名、密码和记住我，不再保存图形验证码票据。

## BDD Scenarios

- BDD: login without graphical captcha -> Given 登录页展示租户、用户名和密码输入框；When 用户点击登录；Then 不展示滑块/点选/图形验证码弹窗，直接调用 `/system/auth/login` 并保留真实错误返回。

## RED / GREEN Evidence

- RED: `node tests/e2e/login-captcha-disabled-static.spec.cjs` -> FAIL, `LoginForm must not render graphical Verify during account-password login.`
- GREEN: `node tests/e2e/login-captcha-disabled-static.spec.cjs` -> PASS, `GREEN: login-captcha-disabled-static -> PASS`
- REGRESSION: `pnpm ts:check` -> PASS。

## Responsive, Accessibility, Loading, Empty, Error, Permission

- Loading: 保留原 `loginLoading` 和 `ElLoading.service` 流程。
- Error: 保留 `loginErrorMessage` 展示登录、租户和权限错误。
- Permission: 登录成功后的路由跳转和权限路由读取未改动。
- Responsive/accessibility: 本次只删除弹窗验证码，不改变表单布局字段、按钮或响应式类。

## E2E Or Component Verification Path

- 静态合同覆盖当前登录页验证码移除和 payload 约束。
- 未启动本机 8081/48081 真实运行态，未执行 Playwright 登录实跑。

## Blockers And Follow-Up Skills

- Git closeout blocked: 当前 `int_main` 已 ahead 17 且存在大量非本任务脏改动；未执行提交/推送，避免混入并行任务。
