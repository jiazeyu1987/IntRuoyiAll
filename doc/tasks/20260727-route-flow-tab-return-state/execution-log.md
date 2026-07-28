# Execution Log

## User Intent

用户反馈：路线流转关系图页面从顶部 tab 切换走，再切回来后变成路线列表页面；期望切回后仍停留在原来的流转关系图。

## BDD Scenarios

BDD: 顶部页签返回保留流转关系图 -> Given 用户已打开某条路线的 `tab=flow` 流转关系图页面，When 用户从顶部页签切换到另一个已打开页面再切回该路线页签，Then 当前页面仍显示原路线的流转关系图而不是路线列表。

## Milestone 1 - Root Cause

- Status: completed
- Completed work: 已确认 `MesProRouteEdit` 使用 `noTagsView: true`，顶部“工艺流程”标签仍保存路线列表路由；切换顶部页签后点击该标签会按其原始 `fullPath` 回到路线列表。
- Verification evidence: `RouteEditPage.vue` 的默认 `tab=flow` 初始化正常，但没有同步/恢复顶部标签目标；现有 `mes-route-flow-entry-readonly-static.spec.js` 证明编辑页必须继续保持隐藏路由。
- Remaining blockers: 需要增加隐藏编辑路由与现有 activeMenu 标签之间的受控同步。

## TDD Evidence

- RED: `node tests/e2e/mes-route-flow-tab-return-state-static.spec.js` -> FAIL，当前 `tagsView.ts` 没有按 `activeMenu` 替换隐藏编辑路由对应顶部标签的能力。
- GREEN: `node tests/e2e/mes-route-flow-tab-return-state-static.spec.js` -> PASS，隐藏编辑路由会替换 `activeMenu` 对应顶部页签目标，并可恢复原列表页签快照。
- REGRESSION: `node tests/e2e/mes-route-flow-entry-readonly-static.spec.js` -> PASS，编辑页继续保持 `noTagsView: true` 和原有只读流转关系图入口。
- REGRESSION: `pnpm ts:check` -> PASS，项目标准 8 GB Node 堆配置下 Vue/TypeScript 类型检查通过。
- GREEN: `node tests/e2e/mes-route-flow-tab-return-state-real.e2e.js` -> PASS，本机 `8081/48081`、身份标签 `芋道源码/admin` 下，从流转关系图通过 MES 菜单切到“生产工单”，再点击顶部“工艺流程”页签返回，URL 仍为路线编辑页且保留 `tab=flow`，流转节点可见，全程无 MES 写请求。
- GREEN: experience-preflight -> PASS，经验已合并到 `docs/frontend-development.md` 的“前端隐藏路由顶部页签状态门禁”，并在 `docs/experience-index.md` 增加检索入口。
- BLOCKED E2E SAMPLE: `node tests/e2e/mes-route-flow-tab-return-state-real.e2e.js` -> FAIL, expected reason: 固定默认样本 `RT000017` 在当前本机租户路线列表未命中；脚本改为只有显式传入路线编码时才按编码过滤，未传入时从真实页面列表选择第一条可编辑路线。
- BLOCKED E2E PATH: `node tests/e2e/mes-route-flow-tab-return-state-real.e2e.js` -> FAIL, expected reason: 路线列表“编辑”按钮进入候选版本生产配置流程，不是只读打开流转关系图；脚本改为点击真实列表的“当前生效版本”链接进入 `tab=flow`。
- E2E PATH ADJUSTMENT: fresh browser session under the current local runtime only exposed one top tag after entering route flow; script leaves through an in-app MES menu item and then clicks the top `工艺流程` tag back, preserving the user-visible return behavior under test without refreshing the app.

## Milestone 2 - Minimal Fix

- Status: completed
- Completed work: `tagsView.ts` 增加按隐藏路由 `activeMenu` 替换/恢复顶部页签的能力；`RouteEditPage.vue` 在挂载、激活和路由 query 变化时同步页签目标，只在显式返回路线列表时恢复原始列表页签。
- Verification evidence: 聚焦静态合同和相邻只读入口合同均通过，现有单页签行为与 `noTagsView: true` 保持不变。
- Remaining blockers: 无生产代码阻塞。

## Milestone 3 - Verification

- Status: completed_with_environment_note
- Completed work: 静态合同、相邻回归、类型检查和一次完整真实 Playwright 路径通过。
- Verification evidence: 详见 `verification-report.md`。
- Environment note: 收尾阶段再次复跑真实 E2E 时，本机登录成功响应后页面自动导航出现间歇性超时；项目官方 `scripts/preflight/login-preflight.mjs` 在同一 `8081` 运行态也超时，故该次复跑归类为本机登录运行态阻塞，不覆盖此前已取得的真实页签路径 PASS。

## Build Verification

- BLOCKER: `pnpm build:local` -> TIMEOUT，分别在 180 秒和 600 秒超时，未取得构建通过结果。
- CLEANUP: 已核对并停止本任务遗留的 `pnpm/cross-env/vite build` 进程 `42084/32812/47944`，未停止 `8081/48081` 本地服务。
- DIAGNOSTIC: 直接调用 4 GB 默认堆的 `vue-tsc` 出现 JavaScript heap out of memory；按项目标准 `pnpm ts:check` 使用 8 GB 堆串行复跑通过。

## Milestone 4 - Closeout Cleanup

- Status: completed
- PREVIEW: `task_closeout.py --task-id 20260727-route-flow-tab-return-state --mode preview` -> PASS，保留 `task.md`、`execution-log.md`、`verification-report.md`，仅计划删除两份临时 evidence，无 blocked/warnings。
- APPLY: `task_closeout.py --task-id 20260727-route-flow-tab-return-state --mode apply` -> PASS，仅删除已完成 validator 校验的 `bug-regression-evidence.md` 和 `frontend-feature-evidence.md`。
- Remaining blocker: 受并行脏工作区和基线提交规则限制，无法安全提交本任务剩余文件，任务继续保持 `ready_for_closeout`。

## Blockers

- 当前前端仓库存在其他任务的未提交改动；本任务不得覆盖、回滚或清理这些改动。
- 当前工作区存在并行任务改动，无法安全执行“全部脏改动基线提交”；本任务不得把其他任务文件纳入提交，因此保持 `ready_for_closeout`，不标记 `completed`。
- `pnpm build:local` 未在 600 秒内完成；本任务不宣称构建通过。
