# Frontend Feature Evidence

## Feature

MES 工艺流程和批记录表单两个用户可见正式菜单页签切回时不重新刷新，复用 DCC 已建立的动态路由缓存契约。

## Acceptance

- `Acceptance-1`: 工艺流程菜单 `mes/pro/route/index` / `MesProRoute` 必须稳定命中 keep-alive include。
- `Acceptance-2`: 批记录表单菜单 `mes/pro/batchrecordformlist/index` / `MesProBatchRecordFormList` 必须稳定命中 keep-alive include。
- `Acceptance-3`: 两个页面在 `routerHelper.ts` 中强制 `tagsViewKeyMode='path'` 和 `meta.noCache=false`。
- `Acceptance-4`: 两个页面不得用 `watch(() => route.fullPath)` 在同状态切回时触发恢复加载。
- `Acceptance-5`: 两个页面的 query watcher 必须先确认当前仍是本页正式 path，再执行列表加载或 designer 清理。
- `Acceptance-6`: 两个页面的 query watcher 必须比较有效 route state；同路径同查询切回不得重新请求目标列表 API。

## BDD:

- BDD: MES route-flow/batch-record tabs keep cached -> Given 用户已打开“工艺流程”和“批记录表单”两个顶部页签 / When 用户切到其它页签后再切回 / Then 已打开页签保留在 `keep-alive` 缓存中，不重新执行首屏加载。
- BDD: MES route-flow/batch-record tabs avoid same-state route watcher reload -> Given 目标页面已完成首屏加载 / When 用户切走再切回且有效路由状态没有变化 / Then 页面保留当前内容，不因 `route.fullPath` watcher 或 query 同步重复刷新。

## RED:

- `pnpm e2e:mes:route-tabs-no-reload:static` -> FAIL，预期失败原因为缺少 MES 工艺流程/批记录表单缓存路由集合和 `noCache=false` 覆盖。
- `pnpm e2e:mes:route-tabs-no-reload:real` -> FAIL，预期失败原因为同路径同查询切回“工艺流程”页签后目标列表 API 计数增加。

## GREEN:

- `pnpm e2e:mes:route-tabs-no-reload:static` -> PASS。
- `pnpm e2e:mes:route-tabs-no-reload:real` -> PASS，目标列表 API 计数在两次顶部页签切回前后均不增加。

## Verification

- `pnpm e2e:mes:route-flow-last-selection-restore:static` -> PASS。
- `pnpm e2e:dcc:browser-tab-return-no-reload:static` -> PASS。
- `pnpm e2e:dcc:upload-browser-tab-cache:static` -> PASS。
- `node --check tests/e2e/mes-route-tabs-no-reload-real.e2e.js` -> PASS。
- `pnpm ts:check` -> PASS。
- `git diff --check -- IntRuoyiFronted/src/utils/routerHelper.ts IntRuoyiFronted/src/store/modules/tagsView.ts IntRuoyiFronted/src/views/mes/pro/route/index.vue IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue IntRuoyiFronted/tests/e2e/mes-route-tabs-no-reload-static.spec.js IntRuoyiFronted/tests/e2e/mes-route-tabs-no-reload-real.e2e.js IntRuoyiFronted/package.json doc/tasks/20260803-mes-route-tabs-no-reload` -> PASS。

## Blockers

- 提交/推送前必须隔离或处理当前工作区非本任务脏改动和 `int_main...origin/int_main [behind 2]` 状态。
