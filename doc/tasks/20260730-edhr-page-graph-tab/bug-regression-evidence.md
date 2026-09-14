# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: eDHR 共享页签显示“批记录页面关系图”，但真实浏览器点击页签后 URL 不变化，无法进入关系图页面。
- Expected: 用户从批次执行、生产填写或 PQC填写页面点击“批记录页面关系图”后，应进入 `/mes/pro/feedback/edhr-batch-page-graph`。

## Reproduction

- Environment: `int_main`，前端 `http://127.0.0.1:8081`，后端 `http://127.0.0.1:48081`。
- Identity: 本机默认只读身份标签 `芋道源码/admin`。
- Path: 登录后进入 `/mes/pro/feedback/edhr-batch-execution`，点击页签“批记录页面关系图”。
- Result before fix: Playwright 等待 `location.pathname` 包含 `edhr-batch-page-graph` 超时。

## Root Cause

- `EdhrBatchRecordTabs.vue` 仅监听 Element Plus `tab-change`。
- 当前真实点击路径没有稳定触发共享组件的路由处理，页签视觉可点击但未执行 `router.push`。
- 修复为监听 Element Plus `tab-click`，从 `TabsPaneContext.props.name` 读取页签名称并调用统一路由函数。

## Regression Test

- Updated: `IntRuoyiFronted/tests/e2e/edhr-batch-page-graph-tab-static.spec.js`。
- Contract: 必须存在 `@tab-click="handleTabClick"`，并禁止继续只依赖 `@tab-change="handleTabChange"`。

## RED / GREEN

- RED: `node tests/e2e/edhr-batch-page-graph-tab-static.spec.js` -> FAIL，原因：共享页签缺少 `@tab-click="handleTabClick"`。
- GREEN: `node tests/e2e/edhr-batch-page-graph-tab-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs` -> PASS。
- TYPE CHECK: `pnpm ts:check` -> PASS。

## Real E2E Verification

- Official login preflight: PASS。
- Graph page: PASS。
- Visible page nodes: 12。
- Relationship edges: 11。
- Pending disabled nodes: 生产工单、工序池、班组长复核、FIFO分配、归档、MES工序/班组设置。
- Route navigation: 生产填写、PQC填写、正式批记录均完成真实前端路由跳转。
- MES mutating requests: 0。
- Downstream rerun: 生产填写和 PQC填写均未出现设备账号工艺路线绑定来源缺失或账号路线/岗位工位绑定缺失阻塞。
- Screenshot: `E:\IntRuoyi\output\playwright\edhr-page-graph-real-e2e-rerun.png`。

## Risk And Regression Scope

- Fix is limited to the shared eDHR batch tab click event and route selection.
- Existing active-tab behavior and all five tab route mappings remain unchanged.

## Blockers And Follow-Up

- Full business flow for this page graph verification is PASS after current backend runtime reload.
- Browser still reports non-MES avatar resource `502`; this is outside the page graph, production/PQC and formal batch-record route assertions.
