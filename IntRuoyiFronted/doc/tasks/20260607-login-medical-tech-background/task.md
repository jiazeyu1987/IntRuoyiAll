# 任务：登录页医疗科技背景与平台名称

## 任务目标

给登录页增加一张高端、科技感的医疗背景图，并在登录页展示平台名称 `瑛泰数字化平台`。保留账号登录主路径、验证码、忘记密码与 SSO 授权承载能力。

## Previous Task Check

- 前端上一任务：`doc/tasks/20260607-mdm-product-update-date-format/`。
- 状态：completed，但相关文件当前已在暂存区，且不属于本次登录页背景任务。
- 处理：本任务只修改登录页背景、平台名称、生成图资产、测试和本任务文档；提交时使用明确 pathspec，避免带入产品主数据暂存文件。

## BDD 场景

- BDD: 登录页展示医疗科技背景 -> Given 用户打开登录页 / When 页面渲染 / Then 页面使用项目内医疗科技背景图作为全屏背景，并保持登录表单可见。
- BDD: 登录页展示平台名称 -> Given 用户打开登录页 / When 页面渲染 / Then 页面显示 `瑛泰数字化平台`，不再展示旧的 `瑛泰管理系统` 作为登录页平台名。
- BDD: 登录页保留账号登录主路径 -> Given 用户打开登录页 / When 页面渲染 / Then 租户、用户名、密码、记住我、忘记密码、登录按钮和验证码能力仍保留。

## Milestones

- [x] M1：检查上一任务状态，创建本任务文档和 BDD。
- [x] M2：生成并保存项目内医疗科技背景图资产。
- [x] M3：补静态 RED 测试，锁定背景图、平台名和登录主路径。
- [x] M4：最小化修改登录页模板和样式。
- [x] M5：运行静态测试、类型检查、桌面/移动 Playwright 页面验证和证据校验。
- [x] M6：运行 task-closeout-cleanup 预览并提交本任务相关改动。

## Expected Verification

- `node tests/e2e/login-medical-tech-background-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- Playwright 打开 `http://localhost:8081/login` 确认背景图、平台名和登录表单可见。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260607-login-medical-tech-background/frontend-feature-evidence.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。使用项目内正式背景图资产并在登录页明确引用，不依赖外部临时路径。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- `doc/tasks/20260607-login-medical-tech-background/frontend-feature-evidence.md`

## 当前状态

completed: 登录页已增加高端医疗科技背景图，并展示 `瑛泰数字化平台`；静态测试、登录页回归测试、类型检查、桌面/移动 Playwright 验证、证据契约校验和 task-closeout-cleanup 预览均已通过。
