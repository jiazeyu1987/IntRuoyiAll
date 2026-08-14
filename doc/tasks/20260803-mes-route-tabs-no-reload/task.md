# 工艺流程 / 批记录表单页签切回不刷新

## Task Goal

修复 MES “工艺流程”和“批记录表单”页面在顶部页签切换后再切回时重复刷新页面的问题。正式行为应与 DCC 文件上传/受控浏览一致：已打开页签应命中 `keep-alive` 缓存；若页面内部监听路由变化，也只能在有效路由状态变化时重新加载，不能在同状态切回时刷新首屏。

## Milestones

- [x] 建立任务文档、BDD 场景和适用门禁。
- [x] 定位“工艺流程”和“批记录表单”的正式动态菜单路径、组件名和页内加载触发点。
- [x] 增加 RED 静态合同，证明这两个页面同状态切回不得重新挂载或重新加载。
- [x] 实施最小修复，保留正常查询/状态变化时的正式加载。
- [x] 运行定向静态合同、真实 Playwright E2E、相邻页签缓存合同和 TypeScript 检查。
- [ ] 收尾提交/推送。验证报告和 cleanup 已完成；提交/推送被非本任务脏改与当前分支落后 origin 状态阻塞。

## Expected Verification

- `pnpm e2e:mes:route-tabs-no-reload:static`
- `pnpm e2e:mes:route-tabs-no-reload:real`
- `pnpm e2e:mes:route-flow-last-selection-restore:static`
- `pnpm e2e:dcc:browser-tab-return-no-reload:static`
- `pnpm e2e:dcc:upload-browser-tab-cache:static`
- `pnpm ts:check`
- `git diff --check -- IntRuoyiFronted/src/utils/routerHelper.ts IntRuoyiFronted/src/store/modules/tagsView.ts IntRuoyiFronted/src/views/mes/pro/route/index.vue IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue IntRuoyiFronted/tests/e2e/mes-route-tabs-no-reload-static.spec.js IntRuoyiFronted/tests/e2e/mes-route-tabs-no-reload-real.e2e.js IntRuoyiFronted/package.json doc/tasks/20260803-mes-route-tabs-no-reload`

## Applicable Gates

- 前端页签首屏按需挂载门禁：动态菜单页签必须核对 `componentName`、SFC `defineOptions({ name })`、`meta.noCache=false`、`tagsViewKeyMode='path'`、`AppView keep-alive` 和页内 `route.fullPath` watcher。
- 前端隐藏路由顶部页签状态门禁：若页面使用 query/activeMenu 保留编辑或详情上下文，切回时不得丢失当前对象或回到列表。
- 工艺路线三类配置术语契约：本任务只修页签缓存/刷新行为，不改变工艺路线“工序开始”“批记录表单”“表单槽位”的数据来源、展示口径或保存逻辑。
- E2E 脚本入口存在性门禁：静态合同必须在 `package.json` 中有明确脚本；静态 PASS 不冒充真实 Playwright 页面验收。

## Current Status

ready_for_closeout

## Completed Work

- 已读取 `bug-regression-fix-loop`、`frontend-feature-delivery`、`docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`。
- 已读取 `docs/experience-index.md` 并匹配到 `docs/frontend-development.md#前端页签首屏按需挂载门禁`、`docs/frontend-development.md#前端隐藏路由顶部页签状态门禁`、`AGENTS.md#工艺路线三类配置术语契约`。
- 已定位正式菜单与组件：工艺流程为 `mes/pro/route/index` / `MesProRoute`；批记录表单为 `mes/pro/batchrecordformlist/index` / `MesProBatchRecordFormList`。
- 已确认两个页面没有 `route.fullPath` watcher，同状态切回刷新主要来自动态路由未强制 `noCache=false` 导致 keep-alive 可能未命中。
- 已新增 `e2e:mes:route-tabs-no-reload:static` 静态合同，并在 `routerHelper.ts` 中为工艺流程/批记录表单建立正式缓存路径与组件集合，强制 `tagsViewKeyMode='path'` 与 `noCache=false`。
- 已验证批记录表单页中 `returnTo: route.fullPath` 仅用于模板/填写返回路径，不是页签切回 watcher；静态合同已收窄为禁止 `watch(() => route.fullPath)`。
- 已为工艺流程和批记录表单页内 query watcher 增加正式 path guard，避免 keep-alive 后台组件在用户切到其它顶部页签时响应其它路由并触发列表刷新。
- 已为工艺流程和批记录表单页增加有效 route state key 与“最后成功加载状态”记录；同一路径同查询切回时直接复用 keep-alive 实例，不再因数组 watcher 新引用重复请求列表。
- 已新增真实 Playwright E2E 脚本并完成本机真实页面验证：从 `/mes/pro/route` 打开工艺流程，再通过菜单进入“批记录表单”，最后点击顶部页签切回两页；目标列表 API 计数保持不变，且无 MES 写请求。
- 已生成 `verification-report.md`，记录本次定向合同、相邻回归、DCC 回归、类型检查和 diff 检查。
- 已按 `project-experience-consolidation` 复核，本次模式已由 `docs/frontend-development.md#顶部菜单页签切回缓存` 覆盖，不新增长期经验文档。
- 已通过 `bug-regression-fix-loop` 与 `frontend-feature-delivery` 证据校验，并执行 `task-closeout-cleanup` preview/apply；无删除项。

## Verification Evidence

- RED: `pnpm e2e:mes:route-tabs-no-reload:static` -> FAIL，预期失败原因为 `routerHelper.ts` 缺少工艺流程/批记录表单正式缓存组件集合和强制 `noCache=false` 覆盖。
- RED: `pnpm e2e:mes:route-tabs-no-reload:real` -> FAIL，预期失败原因为切回“工艺流程”页签后 `/admin-api/mes/pro/route/page` 计数从 1 增至 2，说明同状态 query watcher 仍重复加载列表。
- GREEN: `pnpm e2e:mes:route-tabs-no-reload:static` -> PASS。
- GREEN: `pnpm e2e:mes:route-tabs-no-reload:real` -> PASS，`routeList` 和 `batchRecordFormList` 在两次页签切回前后均保持 `1`；结果文件 `output/playwright/20260803-mes-route-tabs-no-reload-real/mes-route-tabs-no-reload-result.json`，截图 `output/playwright/20260803-mes-route-tabs-no-reload-real/mes-route-tabs-no-reload-pass.png`。
- REGRESSION: `pnpm e2e:mes:route-flow-last-selection-restore:static` -> PASS。
- REGRESSION: `pnpm e2e:dcc:browser-tab-return-no-reload:static` -> PASS。
- REGRESSION: `pnpm e2e:dcc:upload-browser-tab-cache:static` -> PASS。
- TYPECHECK: `pnpm ts:check` -> PASS。
- DIFF: `git diff --check -- IntRuoyiFronted/src/utils/routerHelper.ts IntRuoyiFronted/src/store/modules/tagsView.ts IntRuoyiFronted/src/views/mes/pro/route/index.vue IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue IntRuoyiFronted/tests/e2e/mes-route-tabs-no-reload-static.spec.js IntRuoyiFronted/tests/e2e/mes-route-tabs-no-reload-real.e2e.js IntRuoyiFronted/package.json doc/tasks/20260803-mes-route-tabs-no-reload` -> PASS。
- EVIDENCE: `validate_bug_regression.py --evidence doc/tasks/20260803-mes-route-tabs-no-reload/bug-regression-evidence.md` -> PASS。
- EVIDENCE: `validate_frontend_feature.py --evidence doc/tasks/20260803-mes-route-tabs-no-reload/frontend-feature-evidence.md` -> PASS。
- CLEANUP: `task_closeout.py --task-id 20260803-mes-route-tabs-no-reload --mode apply` -> PASS，无删除项。

## Blockers

- 当前工作区存在多个与本任务无关的脏改动，且 `int_main...origin/int_main [behind 2]`；提交/推送不得在未隔离这些改动并同步分支前执行。

## Cleanup Keep

- `doc/tasks/20260803-mes-route-tabs-no-reload/bug-regression-evidence.md`
- `doc/tasks/20260803-mes-route-tabs-no-reload/frontend-feature-evidence.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，复用正式动态路由缓存契约，并为两个目标页面的 query watcher 增加有效 route state guard；同状态切回不会重挂载，也不会重复触发列表请求。
- `是否存在临时补丁或绕过`：否。
