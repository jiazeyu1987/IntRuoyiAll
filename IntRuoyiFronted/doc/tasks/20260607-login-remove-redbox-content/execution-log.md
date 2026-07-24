# 执行日志：删除登录页红框内容

- BDD: 登录页隐藏红框附加入口 -> Given 用户打开登录页面 / When 页面渲染账号登录表单 / Then 页面不显示右上角主题语言控件，也不显示手机登录、二维码登录、注册、其他登录方式、萌新必读及其外链入口。
- BDD: 登录页保留账号登录主路径 -> Given 用户打开登录页面 / When 页面渲染账号登录表单 / Then 租户、用户名、密码、记住我、忘记密码、登录按钮和验证码能力仍保留。
- RED: `node tests/e2e/login-remove-redbox-content-static.spec.js` -> FAIL, expected reason: 当前 `src/views/Login/Login.vue` 仍包含 `ThemeSwitch` 右上角红框控件。
- GREEN: `node tests/e2e/login-remove-redbox-content-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。补充说明：直接 `pnpm ts:check` 曾因 Node 默认 4GB 堆内存 OOM 退出，未产生类型错误明细。
- GREEN: Playwright desktop `/login` render check at `1920x900` -> PASS，红框文字不可见，账号登录主控件保留，`.el-switch` 数量为 0。
- GREEN: Playwright mobile `/login` render check at `390x844` -> PASS，红框文字不可见，账号登录主控件保留，`.el-switch` 数量为 0。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260607-login-remove-redbox-content/frontend-feature-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260607-login-remove-redbox-content --mode preview` -> PASS，keep `task.md`、`execution-log.md`、`frontend-feature-evidence.md`，delete/blocked/warnings 均为 `<none>`。
