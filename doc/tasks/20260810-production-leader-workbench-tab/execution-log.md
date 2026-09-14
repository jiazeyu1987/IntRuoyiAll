# Execution Log：生产组长页签增加生产组长工作台

## User Intent

用户要求：将生产组长工作台作为一个 tab 放在生产组长页签下，只要是生产组长都能看到。

## BDD

BDD: 生产组长看到工作台 tab -> Given 用户进入生产组长页签且具备生产组长身份 / When 页面渲染顶部 tab / Then 能看到“生产组长工作台”tab 并可切换进入现有工作台内容。

BDD: 非当前工作台逻辑不变 -> Given 用户使用人员管理、报工管理、活跃订单池、工序配置等既有 tab / When 新增工作台 tab 后切换其它 tab / Then 既有 tab key、列表和操作入口不被重命名或替换。

## Command And Verification Log

- Read: `frontend-feature-delivery` skill and project frontend/task/encoding rules.
- Created task directory and initial task records.
- Experience Gate: 读取 `docs/experience-index.md`，命中 `docs/frontend-development.md#前端角色内容页签拆分口径门禁`，无需新增长期经验文档。
- RED: `node tests\\e2e\\edhr-batch-record-leader-tabs-static.spec.js` -> FAIL, 相邻合同仍依赖旧 `leaderType === 'PQC'` 文本，无法证明当前共享组件的正式 PQC filter gate。
- GREEN: `node tests\\e2e\\production-leader-workbench-tab-static.spec.cjs` -> PASS。
- GREEN: `node tests\\e2e\\production-leader-function-tabs-static.spec.js` -> PASS。
- GREEN: `node tests\\e2e\\production-leader-tabs-flat-style-static.spec.js` -> PASS。
- GREEN: `node tests\\e2e\\production-leader-remove-team-config-tab-static.spec.cjs` -> PASS。
- GREEN: `node tests\\e2e\\production-leader-active-order-pool-tab-static.spec.js` -> PASS。
- GREEN: `node tests\\e2e\\edhr-batch-record-leader-tabs-static.spec.js` -> PASS after narrowing the adjacent assertion to the current PQC formal filter gate.
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted\\src\\views\\mes\\pro\\processpool\\TeamLeaderWorkbenchPage.vue IntRuoyiFronted\\tests\\e2e\\production-leader-workbench-tab-static.spec.cjs IntRuoyiFronted\\tests\\e2e\\edhr-batch-record-leader-tabs-static.spec.js doc\\tasks\\20260810-production-leader-workbench-tab` -> PASS，only CRLF conversion warnings.
