# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: eDHR 填写辅助模式下，每张字段卡片只显示必要填写内容，隐藏截图红框对应的辅助元信息。
- Non-goals: 不调整后端接口、字段解析、提交逻辑、权限逻辑、切换工序/填写人弹窗和原表模式业务规则。

## Requirements And Acceptance

- R1: 字段卡片不显示可选/必填/已填等徽标。
- R2: 字段卡片不显示字段说明、自动映射、位置或元信息单位行。
- R3: 字段卡片保留字段名称、填写控件、控件旁单位和真实校验错误。
- R4: “任务 / 批次、工序、填写人”三张切换卡继续显示。

## UI Entry And Owned Files

- Entry: eDHR 执行填写页辅助模式。
- Route component: `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
- Static contracts:
  - `IntRuoyiFronted/tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js`
  - `IntRuoyiFronted/tests/e2e/edhr-assist-fill-mode-static.spec.js`

## API Contracts And Data States

- No API contract changes.
- Existing field state, validation state and draft values remain unchanged.

## BDD Scenarios

- BDD: 隐藏辅助填写卡片内部元信息 -> Given 用户打开 eDHR 填写辅助模式 / When 页面渲染每个字段卡片 / Then 卡片只显示字段名称、填写控件、控件单位和真实校验错误，不显示可选/已填、自动映射、位置或字段说明占位。

## RED

- RED: `node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js` -> FAIL，当前辅助填写卡片仍包含 `edhr-fill-workspace__assist-help`。

## GREEN

- GREEN: `node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-fill-workspace-static.spec.js` -> PASS
- GREEN: `pnpm ts:check` -> PASS

## Responsive Accessibility Loading Empty Error Permission

- Responsive: 卡片隐藏元信息后降低最小高度，字段名称和控件仍在列表/配置网格两种布局中保留。
- Accessibility: 字段名称仍作为可见标签，真实校验错误仍显示。
- Loading/empty/error: 未修改加载态、空态和接口错误链路。
- Permission: 未修改权限指令或提交/保存权限。

## E2E Or Component Verification

- 本轮使用静态合同和 `pnpm ts:check` 验证；未启动本地服务或运行真实 Playwright。

## Blockers And Follow-Up Skills

- Git closeout blocked: 验证期间仓库出现并发任务改动且分支推进到 `ahead 3`，本轮不提交/推送，避免混入非本任务文件。
