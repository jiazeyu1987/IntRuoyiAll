# Bug Regression Evidence

## Bug

用户反馈 MES “工艺流程”和“批记录表单”正式菜单页签从其它顶部页签切回时会重新刷新页面；期望与 DCC 页签缓存行为一致。

## Expected

- 已打开的工艺流程和批记录表单页签切走再切回时应命中 `keep-alive`，不重新执行首屏加载。
- 若页面存在路由监听，只允许在正式有效 route state 变化时重新加载，同状态切回不得刷新。

## Reproduction

- RED: `pnpm e2e:mes:route-tabs-no-reload:static` -> FAIL，`routerHelper.ts` 缺少 MES 工艺流程/批记录表单正式缓存路径与组件集合，也未强制 `meta.noCache=false`。
- RED: `pnpm e2e:mes:route-tabs-no-reload:real` -> FAIL，切回“工艺流程”页签后 `/admin-api/mes/pro/route/page` 计数从 1 增至 2。

## Root Cause

动态路由生成时未把 `mes/pro/route/index` 和 `mes/pro/batchrecordformlist/index` 纳入正式页签缓存覆盖；当运行态菜单 `keepAlive` 或 path 身份未闭合时，顶部页签切回可能重新挂载目标页面。同时，keep-alive 保活组件中的 query watcher 使用数组 source，同一路径同查询切回仍可能因为新数组引用触发回调；必须先确认当前正式 path，并在有效 route state 未变化且上次列表成功加载后跳过重复列表请求。

## Verification

- GREEN: `pnpm e2e:mes:route-tabs-no-reload:static` -> PASS。
- GREEN: `pnpm e2e:mes:route-tabs-no-reload:real` -> PASS，切回“工艺流程”和“批记录表单”前后目标列表 API 计数均保持 `1`；无 MES 写请求、无目标网络失败、无 console error、无 pageerror。
- REGRESSION: `pnpm e2e:mes:route-flow-last-selection-restore:static` -> PASS。
- REGRESSION: `pnpm e2e:dcc:browser-tab-return-no-reload:static` -> PASS。
- REGRESSION: `pnpm e2e:dcc:upload-browser-tab-cache:static` -> PASS。
- TYPECHECK: `pnpm ts:check` -> PASS。

## Blockers

- 提交和推送仍被非本任务脏改动和当前分支 `int_main...origin/int_main [behind 2]` 状态阻塞；目标静态合同、真实 E2E、相邻回归和类型检查已通过。
