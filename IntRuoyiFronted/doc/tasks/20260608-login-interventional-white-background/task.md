# 任务：登录页白色介入医疗器械背景

## 任务目标

将登录页背景图替换为白色主调、与登录框背景融合的高端介入医疗器械主题背景。保留平台名称 `瑛泰数字化平台`、账号登录主路径、验证码、忘记密码与 SSO 授权承载能力。

## Previous Task Check

- 前端上一登录页任务：`doc/tasks/20260607-login-medical-tech-background/`。
- 状态：completed，已提交医疗科技背景与平台名称。
- 当前工作区存在非本任务改动：`tests/e2e/dcc-upload-test-file.e2e.js`。
- 处理：本任务只修改登录页背景相关资产、登录页样式、登录页测试和本任务文档；提交时使用明确 pathspec，不包含 DCC 测试文件。

## BDD 场景

- BDD: 登录页展示白色介入医疗器械背景 -> Given 用户打开登录页 / When 页面渲染 / Then 页面使用项目内白色主调介入医疗器械背景图，并与登录框白色背景融合。
- BDD: 登录页保留平台名称 -> Given 用户打开登录页 / When 页面渲染 / Then 页面显示 `瑛泰数字化平台`。
- BDD: 登录页保留账号登录主路径 -> Given 用户打开登录页 / When 页面渲染 / Then 租户、用户名、密码、记住我、忘记密码、登录按钮和验证码能力仍保留。

## Milestones

- [x] M1：检查上一任务状态，创建本任务文档和 BDD。
- [x] M2：生成并保存项目内白色介入医疗器械背景图资产。
- [x] M3：补静态 RED 测试，锁定新背景图、白色融合样式和登录主路径。
- [x] M4：最小化修改登录页背景引用和样式，删除旧背景资产。
- [x] M5：运行静态测试、类型检查、桌面/移动 Playwright 页面验证和证据校验。
- [x] M6：运行 task-closeout-cleanup 预览并提交本任务相关改动。

## Expected Verification

- `node tests/e2e/login-interventional-white-background-static.spec.js`
- `node tests/e2e/login-medical-tech-background-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- Playwright 打开 `http://localhost:8081/login` 确认白色介入医疗器械背景、平台名和登录表单可见。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260608-login-interventional-white-background/frontend-feature-evidence.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。使用项目内正式背景图资产并更新登录页样式，不依赖外部临时路径。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- `doc/tasks/20260608-login-interventional-white-background/frontend-feature-evidence.md`

## Final Verification Result

- 静态测试：通过。
- 类型检查：通过。
- Playwright 桌面/移动登录页验证：通过。
- 前端功能证据校验：通过。
- task-closeout-cleanup 预览：通过，无删除项、无阻塞、无警告。

## 当前状态

completed: 登录页已替换为白色主调介入医疗器械背景，保留 `瑛泰数字化平台` 和账号登录主路径，验证与收尾预览均已通过。
