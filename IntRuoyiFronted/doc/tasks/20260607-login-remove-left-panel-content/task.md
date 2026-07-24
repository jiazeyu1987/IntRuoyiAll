# 任务：删除登录页左侧红框内容

## 任务目标

删除登录页面截图红框中的左侧品牌展示区内容，包括左侧深色背景、左上角 logo/系统标题、插画、欢迎语和说明文案。保留右侧账号登录主路径，以及移动端顶部 logo/系统标题。

## Previous Task Check

- 前端上一任务：`doc/tasks/20260607-login-remove-redbox-content/`。
- 状态：completed，已提交 `1a79f46a7 任务: 删除登录页红框内容`。
- 处理：本任务只修改登录页左侧展示区相关文件、测试和本任务文档；不暂存或提交当前工作区里的产品主数据未提交文件。

## BDD 场景

- BDD: 登录页隐藏左侧品牌展示区 -> Given 用户打开登录页面 / When 页面渲染登录页 / Then 左侧深色展示区、左上角 logo/系统标题、插画、欢迎语和说明文案不再显示。
- BDD: 登录页保留账号登录主路径 -> Given 用户打开登录页面 / When 页面渲染登录页 / Then 租户、用户名、密码、记住我、忘记密码、登录按钮和验证码能力仍保留。

## Milestones

- [x] M1：检查上一任务状态，创建本任务文档和 BDD。
- [x] M2：补静态 RED 测试，锁定左侧展示区删除和登录主路径保留。
- [x] M3：最小化修改登录页模板、脚本和样式。
- [x] M4：运行静态测试、类型检查、桌面/移动 Playwright 页面验证和证据校验。
- [x] M5：运行 task-closeout-cleanup 预览并提交本任务相关改动。

## Expected Verification

- `node tests/e2e/login-remove-left-panel-content-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- Playwright 打开 `http://localhost:8081/login` 确认左侧红框内容不可见、账号登录仍可见。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260607-login-remove-left-panel-content/frontend-feature-evidence.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接删除左侧展示区渲染与对应样式资源引用，不使用 CSS 隐藏绕过。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- `doc/tasks/20260607-login-remove-left-panel-content/frontend-feature-evidence.md`

## 当前状态

completed: 登录页左侧红框内容已删除，账号登录主路径保留；静态测试、回归测试、类型检查、桌面/移动 Playwright 验证、证据契约校验和 task-closeout-cleanup 预览均已通过。
