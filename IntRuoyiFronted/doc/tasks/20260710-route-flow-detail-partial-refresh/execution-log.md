# 执行日志

BDD: 切换工序仅刷新字段内容 -> Given 用户已选中一个工序并看到左侧详情 / When 用户点击另一个工序且详情请求尚未返回 / Then 侧栏结构、关键工序开关、删除按钮、字段选择器和字段卡片保持显示，仅字段值区域显示加载骨架。

BDD: 普通详情和关联设备独立更新 -> Given 工序详情接口先于关联设备接口返回 / When 普通详情请求完成 / Then 普通字段立即显示新工序内容，关联设备字段继续显示字段内加载骨架直到自己的请求完成。

BDD: 快速切换只应用最后响应 -> Given 用户快速依次点击多个工序且接口乱序返回 / When 较早选择的响应晚于最后选择返回 / Then 页面只显示最后选中工序的数据，过期响应不得覆盖当前内容。

BDD: 当前请求失败明确可见 -> Given 当前选中工序详情接口失败 / When 请求结束 / Then 页面显示原有明确错误提示并结束对应字段加载，不使用旧值、模拟值或 fallback。

ROOT_CAUSE: `RouteFlowGraphDesigner.vue` 在整个详情 `aside` 上绑定 `v-loading="selectedProcessDetailLoading"`，切换工序时 `loadSelectedProcessDetail` 同时清空普通详情和设备列表，并通过 `Promise.all` 等待两个接口完成，导致红框侧栏整体遮罩和所有字段一起刷新；请求缺少身份校验，快速切换还存在旧响应覆盖新选择的风险。

RED: `node tests/e2e/mes-route-flow-detail-partial-refresh-static.spec.js` -> FAIL，当前组件仍包含侧栏整体 `v-loading="selectedProcessDetailLoading"`，尚无字段级骨架、设备独立 loading 和过期响应保护。

GREEN: `node tests/e2e/mes-route-flow-detail-partial-refresh-static.spec.js` -> PASS。

GREEN: `node tests/e2e/mes-route-flow-selected-process-detail-static.spec.js`、`mes-route-flow-detail-visible-items-static.spec.js`、`mes-route-flow-link-return-state-static.spec.js` -> PASS。

GREEN: experience-preflight -> PASS，已读取 `docs/login-access.md`；本机 `localhost:8081` 正在监听，前端仓 Playwright 为 `1.60.0`，真实验证限定测试租户 `aoteman`、本机入口和系统 Chrome，不访问远端环境，不伪造接口响应。

BLOCKER: priority-switch -> 用户切换到更高优先级的 `20260710-route-flow-boundary-links`；两项任务共同修改 `RouteFlowGraphDesigner.vue`，当前分支未包含本任务对应生产代码改动，因此本任务显式阻塞，避免并行覆盖。

RESUMED: 用户明确要求继续实施本任务；重新核对目标组件和当前工作区后，保留其他任务改动并完成字段级刷新实现。

GREEN: official-login-preflight -> PASS，真实登录测试租户 `aoteman` 并进入 `/mes/pro/route`。

BLOCKER: 首次真实 E2E -> TIMEOUT，局部刷新断言重复点击当前已选中的第二个工序，未触发新的详情请求；已在断言前先切回第一个工序。

BLOCKER: 第二次真实 E2E -> FAIL，字段可见判断被字段选择器当前选项文本误命中；已改为使用稳定的 `data-flow-detail-field` 键判断字段是否挂载。

GREEN: `node tests/e2e/mes-route-flow-detail-visible-items-real.e2e.js` -> PASS，测试租户路线 `RT000017` 使用真实接口响应验证：侧栏无整体 loading mask，字段卡片不卸载，普通详情与设备独立更新，过期响应不覆盖最后选择；`pageErrors`、`consoleErrors`、`requestFailures` 均为空。

GREEN: 视觉复核 -> PASS，真实页面截图显示侧栏结构完整，三个字段仅值区域显示骨架，关键工序开关、删除工序、字段选择器和字段标题保持可见。

GREEN: `node tests/e2e/mes-route-flow-detail-partial-refresh-static.spec.js`、工序详情、字段持久化、返回状态、流程图回归 -> PASS。

GREEN: 目标 ESLint、`pnpm.cmd ts:check`、前端证据校验、缺陷证据校验和 `git diff --check` -> PASS。

GREEN: task-closeout preview/apply -> PASS，仅保留 `task.md` 与 `execution-log.md`，删除前端/缺陷证据和本任务 E2E 临时产物。

GREEN: git commit -> PASS，提交信息 `任务: 工序侧栏字段级刷新`，仅包含本任务生产代码、测试、任务记录和请求日志片段。
