# 任务：删除登录页左上角 Logo

## 任务目标

删除登录页左上角红框中的头像/Logo 图片，只保留平台名称 `瑛泰数字化平台`、白色介入医疗器械背景和账号登录主路径。

## Previous Task Check

- 上一任务文档：`doc/tasks/20260608-runtime-console-test-root-cleanup/`。
- 状态：blocked，原因是用户切换到登录页 Logo 删除任务，运行台清理任务有未提交改动且缺少继续验证上下文。
- 当前工作区存在非本任务改动：`tests/e2e/dcc-upload-test-file.e2e.js`、`tests/e2e/runtime-control-restore-target-static.spec.js`、`tests/e2e/runtime-control-rollback-target-static.spec.js`、`doc/tasks/20260608-runtime-console-test-root-cleanup/`、`tests/e2e/dcc-backup-boundary-static.spec.js`、`tests/e2e/dcc-restore-verify.e2e.js`。
- 处理：本任务只提交登录页 Logo 删除相关源码、测试和本任务文档；旧任务阻塞记录不混入本次提交。

## BDD 场景

- BDD: 登录页不展示左上角 Logo -> Given 用户打开登录页 / When 页面渲染 / Then 左上角不再显示头像/Logo 图片。
- BDD: 登录页保留平台名称 -> Given 用户打开登录页 / When 页面渲染 / Then 页面仍显示 `瑛泰数字化平台`。
- BDD: 登录页保留账号登录主路径 -> Given 用户打开登录页 / When 页面渲染 / Then 租户、用户名、密码、记住我、忘记密码、登录按钮和验证码能力仍保留。

## Milestones

- [x] M1：检查上一任务状态，创建本任务文档和 BDD。
- [x] M2：补静态 RED 测试，锁定左上角 Logo 删除与登录主路径保留。
- [x] M3：最小化修改登录页模板和样式，删除左上角 Logo 渲染。
- [x] M4：运行静态测试、类型检查、Playwright 页面验证和证据校验。
- [x] M5：运行 task-closeout-cleanup 预览并提交本任务相关改动。

## Expected Verification

- `node tests/e2e/login-remove-top-left-logo-static.spec.js`
- `node tests/e2e/login-remove-left-panel-content-static.spec.js`
- `node tests/e2e/login-interventional-white-background-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- Playwright 打开 `http://localhost:8081/login` 确认左上角 Logo 不显示、平台名称和登录表单可见。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260608-login-remove-top-left-logo/frontend-feature-evidence.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接移除登录页左上角 Logo 渲染与对应专用样式，不添加隐藏分支或临时覆盖。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- `doc/tasks/20260608-login-remove-top-left-logo/frontend-feature-evidence.md`

## Verification Result

- RED 静态测试：通过，初次失败于 `login-page__brand-logo` 仍存在。
- 登录页静态回归：通过。
- Playwright 桌面/移动端页面验证：通过，左上角品牌区无图片节点，平台名称和登录表单可见。
- 前端功能证据校验：通过。
- task-closeout-cleanup 预览：通过，无删除项、无阻塞、无警告。
- 类型检查：通过。`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

## 当前状态

completed
