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
- GREEN: 待补充。
- REGRESSION: 待补充。

## Blockers

- 当前前端仓库存在其他任务的未提交改动；本任务不得覆盖、回滚或清理这些改动。
- 真实 Playwright 验证所需的前后端运行态、登录租户和权限尚未核对。
