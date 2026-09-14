# Feature

生产报工页面隐藏截图红框中的一线固定填报面板。

## Goal And Non-Goals

- Goal: `报工` 页面不再显示 `工序/员工/主页/填数量/设备参数/提交` 一线固定填报面板。
- Goal: 保留正式报工列表、筛选区、操作区和导入归属相关能力。
- Non-goal: 不修改 `edhr-batch` 生产/PQC 独立填报页的一线固定填报能力。
- Non-goal: 不调整后端 API、权限、路由或数据结构。

## Acceptance

- Acceptance: 报工页 `src/views/mes/pro/feedback/index.vue` 不导入、不渲染 `FrontlineFixedTemplatePanel`。
- Acceptance: 正式报工 `UnifiedListTemplate` 和 `feedback-filter-action-relocation` 操作区仍存在。
- Acceptance: `BatchProductionFillPage.vue` 与 `BatchPqcFillPage.vue` 继续显式挂载各自模式的 `FrontlineFixedTemplatePanel`。

## UI Entry Points

- Route/page: `src/views/mes/pro/feedback/index.vue`。
- Component removed from this entry: `src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`。
- Preserved independent entries: `src/views/mes/pro/edhr-batch/BatchProductionFillPage.vue`、`src/views/mes/pro/edhr-batch/BatchPqcFillPage.vue`。

## API Contracts And States

- API contracts: 未变更。
- Loading/empty/error states: 正式报工列表沿用现有 `UnifiedListTemplate` 与原查询逻辑，未新增吞异常或降级路径。
- Permission checks: 原 `v-hasPermi` 操作按钮与列表权限逻辑未变更。

## BDD

- BDD: 报工页隐藏一线固定填报面板 -> Given 用户打开生产报工页面 / When 页面渲染正式报工页签 / Then 截图红框中的 `工序/员工/主页/填数量/设备参数/提交` 一线固定填报面板不得显示，正式报工列表仍可见。

## RED

- RED: `node tests/e2e/mes-feedback-hide-frontline-panel-static.spec.js` -> FAIL, expected reason: 报工页仍渲染截图红框中的一线固定填报面板。

## GREEN

- GREEN: `node tests/e2e/mes-feedback-hide-frontline-panel-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-feedback-header-action-relocation-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-feedback-unified-list-template-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs` -> PASS。

## Verification

- Static contract verifies the 报工 page no longer imports or renders `FrontlineFixedTemplatePanel` in the formal feedback tab.
- Static contract verifies the formal list and action relocation container remain.
- Adjacent eDHR static contract verifies the independent production/PQC fill pages still retain their frontline panel.

## Blockers

- Blockers: 无。
