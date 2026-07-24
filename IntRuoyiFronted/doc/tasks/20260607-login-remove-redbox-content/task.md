# 任务：删除登录页红框内容

## 任务目标

删除登录页面截图红框中的可见内容：右上角主题/语言控件，以及账号登录表单下方的手机登录、二维码登录、注册、其他登录方式、萌新必读链接区域。保留账号密码登录、租户选择、记住我、忘记密码、验证码和 SSO 授权路由承载能力。

## Previous Task Check

- 前端上一任务：`doc/tasks/20260607-mdm-product-update-date-format/`。
- 状态：已记录为 blocked，因为其未提交改动不属于本次请求，缺少继续完成和提交该任务的明确归属确认。
- 处理：本任务只修改登录页相关文件和本任务测试/文档，不暂存或提交上一任务文件。

## BDD 场景

- BDD: 登录页隐藏红框附加入口 -> Given 用户打开登录页面 / When 页面渲染账号登录表单 / Then 页面不显示右上角主题语言控件，也不显示手机登录、二维码登录、注册、其他登录方式、萌新必读及其外链入口。
- BDD: 登录页保留账号登录主路径 -> Given 用户打开登录页面 / When 页面渲染账号登录表单 / Then 租户、用户名、密码、记住我、忘记密码、登录按钮和验证码能力仍保留。

## Milestones

- [x] M1：检查上一任务状态，创建本任务文档和 BDD。
- [x] M2：补静态 RED 测试，锁定红框内容删除和主登录路径保留。
- [x] M3：最小化修改登录页模板和无用脚本引用。
- [x] M4：运行静态测试、类型检查、浏览器页面验证和证据校验。
- [x] M5：运行 task-closeout-cleanup 预览并提交本任务相关改动。

## Expected Verification

- `node tests/e2e/login-remove-redbox-content-static.spec.js`
- `pnpm ts:check`
- 浏览器打开 `http://localhost:8081/login` 确认红框内容不可见。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260607-login-remove-redbox-content/frontend-feature-evidence.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接移除登录页附加入口的渲染与未使用脚本引用，不通过 CSS 隐藏绕过。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- `doc/tasks/20260607-login-remove-redbox-content/frontend-feature-evidence.md`

## 当前状态

completed: 登录页红框内容已删除，账号登录主路径保留；静态测试、类型检查、桌面/移动 Playwright 验证、证据契约校验和 task-closeout-cleanup 预览均已通过。
