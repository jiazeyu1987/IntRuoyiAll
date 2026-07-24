# 任务：电子签名页面去除重复页头与内层 Tab

## 任务目标

左侧菜单已经承载 `电子签名` 下的 8 个子页签后，电子签名页面内部不再显示重复的标题栏、READY 说明和内层 `el-tabs`。访问 `/signature-governance/<child>` 时直接显示当前子页签的实际内容。

## 里程碑

- [ ] M1：写 RED 契约，要求移除页面内 tabs 和红框页头文案。
- [ ] M2：改造 `src/views/signature-governance/index.vue`，按路由直接渲染内容。
- [ ] M3：运行契约测试与类型检查。
- [ ] M4：用测试租户真实登录验证关键子路由。
- [ ] M5：记录证据、运行 closeout，并只提交本任务文件。

## 预期验证

- `node scripts\signature-governance-page-contract.test.mjs`
- `npm run ts:check`
- Playwright 登录 `http://localhost:8081`，使用 `测试租户/aoteman/111111`，分别访问 `/signature-governance/overview`、`/signature-governance/file-signatures`、`/signature-governance/batch-signatures`、`/signature-governance/authorizations`，确认无页面内 tabs 且真实内容直接显示。

## 当前状态

已完成。已移除 `src/views/signature-governance/index.vue` 内部的标题工具栏和 `el-tabs`，子路由现在直接显示实际内容；总览刷新按钮移动到总览内容区。

## Current Status

completed

## 完成记录

- 已用 RED 契约复现重复页头和内层 tabs。
- 已改为按当前路由直接渲染 8 个电子签名内容块。
- 已通过静态契约、类型检查和测试租户真实 Playwright 验证。

## 前一任务检查

- 上一电子签名前端任务 `20260624-signature-governance-sidebar-children` 已完成。
- 当前前端仓库存在其它任务脏改动；本任务只修改电子签名页面、电子签名契约测试和本任务文档。

## 经验门禁

- `docs/login-access.md`：真实 E2E 默认本机 `http://localhost:8081`，使用测试租户 `测试租户/aoteman/111111`；登录失败不得静默切换租户或账号。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：避免嵌套卡片和装饰性页头，保留密集、任务导向的操作台风格。
- `docs/worktree-memory.md`：提交隔离时只暂存本任务文件；真实 E2E 需记录 baseUrl、租户账号和关键接口结果。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，移除重复导航层，让动态菜单子路由成为唯一子页签入口。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 子路由直接显示实际内容 -> Given 用户从左侧电子签名子菜单进入任一 /signature-governance/<child> 路由 / When 页面加载 / Then 不显示页面内标题卡和内层 tabs，直接显示当前子页签实际内容。`
- `BDD: 总览刷新只属于总览内容 -> Given 用户进入非总览电子签名子路由 / When 页面加载 / Then 不显示“刷新电子签名”全局页头按钮；进入总览时刷新按钮位于总览内容区。`

## Cleanup Keep

- `doc/tasks/20260624-signature-governance-flatten-content/task.md`
- `doc/tasks/20260624-signature-governance-flatten-content/execution-log.md`
- `doc/tasks/20260624-signature-governance-flatten-content/frontend-feature-evidence.md`
