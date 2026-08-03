# 工艺流程 / 批记录表单页签切回不刷新

## Task Goal

修复 MES “工艺流程”和“批记录表单”页面在顶部页签切换后再切回时重复刷新页面的问题。正式行为应与 DCC 文件上传/受控浏览一致：已打开页签应命中 `keep-alive` 缓存；若页面内部监听路由变化，也只能在有效路由状态变化时重新加载，不能在同状态切回时刷新首屏。

## Milestones

- [x] 建立任务文档、BDD 场景和适用门禁。
- [ ] 定位“工艺流程”和“批记录表单”的正式动态菜单路径、组件名和页内加载触发点。
- [ ] 增加 RED 静态合同，证明这两个页面同状态切回不得重新挂载或重新加载。
- [ ] 实施最小修复，保留正常查询/状态变化时的正式加载。
- [ ] 运行定向静态合同、相邻页签缓存合同和 TypeScript 检查。
- [ ] 收尾状态、验证报告、清理和提交推送。

## Expected Verification

- `pnpm e2e:mes:route-tabs-no-reload:static`
- `pnpm ts:check`
- `git diff --check -- IntRuoyiFronted/src/utils/routerHelper.ts IntRuoyiFronted/src/views/mes IntRuoyiFronted/tests/e2e/mes-route-tabs-no-reload-static.spec.js IntRuoyiFronted/package.json doc/tasks/20260803-mes-route-tabs-no-reload`

## Applicable Gates

- 前端页签首屏按需挂载门禁：动态菜单页签必须核对 `componentName`、SFC `defineOptions({ name })`、`meta.noCache=false`、`tagsViewKeyMode='path'`、`AppView keep-alive` 和页内 `route.fullPath` watcher。
- 前端隐藏路由顶部页签状态门禁：若页面使用 query/activeMenu 保留编辑或详情上下文，切回时不得丢失当前对象或回到列表。
- 工艺路线三类配置术语契约：本任务只修页签缓存/刷新行为，不改变工艺路线“工序开始”“批记录表单”“表单槽位”的数据来源、展示口径或保存逻辑。
- E2E 脚本入口存在性门禁：静态合同必须在 `package.json` 中有明确脚本；静态 PASS 不冒充真实 Playwright 页面验收。

## Current Status

in_progress

## Completed Work

- 已读取 `bug-regression-fix-loop`、`frontend-feature-delivery`、`docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`。
- 已读取 `docs/experience-index.md` 并匹配到 `docs/frontend-development.md#前端页签首屏按需挂载门禁`、`docs/frontend-development.md#前端隐藏路由顶部页签状态门禁`、`AGENTS.md#工艺路线三类配置术语契约`。

## Verification Evidence

- PENDING: 等待定位后先新增 RED 静态合同。

## Blockers

- 当前工作区存在多个与本任务无关的脏改动，且 `int_main` 已领先 `origin/int_main`；本任务不得提交/推送无关改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：待实现；目标是复用正式动态路由缓存契约，并在必要时按有效 route state 阻断同状态切回加载。
- `是否存在临时补丁或绕过`：否。
