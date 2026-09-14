# Frontend Feature Evidence

## Feature Goal

隐藏工艺路线配置右侧动态表单列表中的“记录本”开关，并让所有表单绑定默认启用记录本。

## Non-goals

- 不修改后端 API 字段结构。
- 不改变批次详情页“批记录/记录本”填写方式切换和全局记录本开关。
- 不新增兼容 fallback 或静默降级。

## Requirements

- REQ-1: 动态表单配置卡片不得显示 `recordbook-enabled` 开关。
- REQ-2: 新增、读取、草稿快照和保存 payload 都必须写入 `recordbookEnabled: true`。
- REQ-3: 旧真实 E2E 不得继续尝试通过隐藏开关创建记录本禁用样本。

## Acceptance

- AC-1: `RouteFlowGraphDesigner.vue` 不包含 `data-route-process-setting-field="recordbook-enabled"`。
- AC-2: `RouteFlowGraphDesigner.vue` 不包含 `recordbookEnabled: report|binding.recordbookEnabled !== false`。
- AC-3: 静态合同确认读取、草稿快照和保存 payload 至少三处写入 `recordbookEnabled: true`。

## UI Entry Points

- Component: `IntRuoyiFronted/src/views/mes/pro/route/RouteFlowGraphDesigner.vue`。
- Static contracts: `IntRuoyiFronted/tests/e2e/edhr-recordbook-config-default-open-static.spec.js`, `IntRuoyiFronted/tests/e2e/edhr-recordbook-batch-sync-static.spec.js`。

## API Contracts And Data States

- Existing field retained: `recordbookEnabled?: boolean | null` in route flow config API types.
- Frontend save state: always emits `recordbookEnabled: true` for form bindings.

## BDD Scenarios

- BDD: 隐藏记录本开关并默认开启 -> Given 用户打开工艺路线/批记录配置右侧动态表单列表, When 页面渲染每个表单配置卡片, Then 不显示“记录本”开关且每个表单配置按记录本开启保存。

## RED

- RED: `node tests\e2e\edhr-recordbook-config-default-open-static.spec.js` -> FAIL, expected reason: 旧实现仍包含 `data-route-process-setting-field="recordbook-enabled"`。

## GREEN

- GREEN: `node tests\e2e\edhr-recordbook-config-default-open-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-recordbook-batch-sync-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-recordbook-global-setting-static.spec.js` -> PASS。
- GREEN: `node --check tests\e2e\edhr-recordbook-batch-sync-real.e2e.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Verification

- Verification: 新增聚焦静态合同、既有记录本批次同步合同、全局开关合同、真实 E2E 语法检查、右侧红框隐藏合同、单据填写人合同和 `pnpm ts:check` 均通过。

## Responsive Accessibility Loading Empty Error Permission

- Accessibility: hidden switch removes obsolete control; no new interactive element added.
- Loading/empty/error/permission: unchanged, because route designer data loading and permission gates remain untouched.
- E2E path: static contracts cover the rendered source contract; no write-type real E2E was run because this task removes a configuration toggle and does not require creating tenant data.

## Blockers And Follow-up Skills

- No functional blocker.
- `project-experience-consolidation` must run before final summary per project closeout rule.
