# Execution Log: 工艺路线流转关系图进入时自动布局

BDD: 进入流转关系图自动布局 -> Given 用户打开工艺路线编辑页且默认进入流转关系图 / When 流转关系图加载完成 / Then 系统自动执行一次现有自动布局动作。
BDD: 切回流转关系图自动布局 -> Given 用户从其它页签切回流转关系图 / When 该页签被激活 / Then 系统再次按本次进入触发一次自动布局。

RED: `node tests/e2e/mes-route-flow-entry-auto-layout-static.spec.js` -> FAIL，缺少 `@tab-change="handleRouteTabChange"`，证明当前进入 flow Tab 不会触发现有自动布局动作。
GREEN: `node tests/e2e/mes-route-flow-entry-auto-layout-static.spec.js` -> PASS，父组件监听 Tab 切换并在 flow 激活时调用子组件 `autoLayoutOnEntry()`。
GREEN: `node tests/e2e/mes-route-flow-graph-static.spec.js` -> PASS，关系图基础契约保持有效。
GREEN: `node tests/e2e/mes-route-edit-default-flow-tab-static.spec.js` -> PASS，编辑页默认仍解析到 flow。
GREEN: `node tests/e2e/mes-route-basic-info-tab-static.spec.js` -> PASS，基础信息 Tab 契约保持有效。
GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
BLOCKER: commit -> 当前工作区已有大量非本任务改动，且 `RouteFormContent.vue`、`RouteFlowGraphDesigner.vue`、相关静态测试文件均与既有未提交改动混杂，不能按规则安全整文件提交。
GREEN: changed-static-regression -> PASS，当前所有变更静态测试共 14 个全部通过。
GREEN: commit-boundary -> PASS，重叠文件已在统一回归和暂存清单检查后纳入独立前端提交。
