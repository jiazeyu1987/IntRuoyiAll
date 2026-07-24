# Execution Log: 工艺路线底部保存统一保存关系图

BDD: 底部保存统一持久化关系图 -> Given 用户编辑工艺路线并调整流转关系图 / When 点击底部保存 / Then 系统先校验关系图，再保存主表和关系图。
BDD: 关系图校验失败阻断主表保存 -> Given 当前流转关系图校验不通过 / When 用户点击底部保存 / Then 系统提示校验错误，且不调用主表保存。
BDD: 关系图页签不再单独保存 -> Given 用户进入流转关系图页签 / When 查看工具栏 / Then 页面不再显示 `保存关系图` 按钮，线性关系草稿提示用户点击底部保存。

GREEN: experience-preflight -> PASS，已读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、`frontend-feature-delivery` 与 `frontend-contract.md`。

RED: node tests/e2e/mes-route-bottom-save-flow-graph-static.spec.js -> FAIL, expected reason: flow graph toolbar still includes standalone `保存关系图` and parent bottom-save contract is missing.

GREEN: implementation -> PASS, removed standalone `保存关系图`, exposed `validateBeforeSubmit()` and `saveFromParent()`, changed linear draft copy to bottom-save effective, and changed bottom submit order to form validate -> graph validate -> route save -> graph save -> one success toast.

RED: node tests/e2e/mes-route-bottom-save-flow-graph-static.spec.js -> FAIL, expected reason after real-path diagnosis: `loadGraph()` still performed implicit route process key flag update, which fails on enabled routes and prevents graph nodes from rendering.

GREEN: implementation -> PASS, `loadGraph()` now calls `applyDefaultKeyProcessLocally()` and does not call `updateRouteProcessKeyFlag`; default key display is local-only during graph load.

GREEN: node tests/e2e/mes-route-bottom-save-flow-graph-static.spec.js -> PASS.

GREEN: node tests/e2e/mes-route-flow-graph-static.spec.js -> PASS.

GREEN: node tests/e2e/mes-route-flow-graph-one-screen-static.spec.js -> PASS.

GREEN: node --check tests/e2e/mes-route-flow-graph-real-flow.e2e.js -> PASS.

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check -> PASS.

BLOCKER: real-e2e -> `node tests/e2e/mes-route-flow-graph-real-flow.e2e.js` cannot complete because local backend route main-table endpoints fail before bottom save can be exercised. Direct API reproduction with test tenant `aoteman` succeeds for login and `/mes/pro/route-process-flow/get?routeId=922074`, but `/mes/pro/route/get?id=922074` and `/mes/pro/route/page?pageNo=1&pageSize=10&code=RT000017` return `{"code":500,"msg":"系统异常","data":null}`. Impact: Playwright can no longer verify the full route edit page main-table save path until backend route get/page is restored.
