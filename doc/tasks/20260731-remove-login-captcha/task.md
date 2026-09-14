# 20260731 Remove Login Captcha

## Task Goal

去除登录时的图形化安全验证，使账号密码登录不再弹出滑块/点选/图形验证码，同时保持正式登录接口错误可见、不吞异常。

## Milestones

- [x] 定位登录页图形验证码触发点、登录 API payload 和后端验证码开关契约。
- [x] 先用最小静态合同和后端单测复现登录仍依赖图形验证码的失败状态。
- [x] 实施最小正式修改，移除登录图形验证入口与必填 payload 依赖。
- [x] 运行目标静态合同、前端类型/相邻验证，并记录验证结果。
- [x] 沉淀登录图形验证码链路经验门禁。

## Expected Verification

- `node tests/e2e/login-captcha-disabled-static.spec.cjs`
- `pnpm ts:check`
- `mvn -pl yudao-module-system -am "-Dtest=AdminAuthServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Experience Gate Summary

- `docs/experience-index.md` 已读取；本任务命中登录页验证码、真实 E2E 登录前置相关经验入口。
- 适用门禁：登录页不得要求人工验证码作为真实 E2E 前置；移除图形验证码必须同时覆盖 UI 触发点与登录 payload，不得用隐藏弹窗或吞登录错误冒充完成。
- 已补充 `docs/login-access.md#登录图形验证码链路门禁`，并在 `docs/experience-index.md` 增加关键词路由。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是移除登录图形验证码入口和 payload 依赖。
- `是否存在临时补丁或绕过`：否。
