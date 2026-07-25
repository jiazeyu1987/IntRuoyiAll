# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 删除个人中心 eDHR 记录本全局开关卡片中的红框元信息块，并让蓝框开关区域整体可点击。
- Non-goal: 不修改后端接口、权限模型、全局开关业务语义或批次详情执行逻辑。

## Requirements And Acceptance

- 红框中的 `配置键`、`当前状态`、`最后更新人`、`最后更新时间` 不再展示。
- 蓝框中的“关闭记录本 / 开关 / 打开记录本”区域整体支持鼠标点击和键盘 Enter/Space 激活。
- 仍保留切换前确认、取消恢复原值、接口失败恢复原值并提示错误。

## UI Entry Points And Owned Files

- Entry: 个人中心 > 配置 > eDHR 记录本全局开关。
- Component: `IntRuoyiFronted/src/views/Profile/components/EdhrRecordbookGlobalSetting.vue`。
- Static contract: `IntRuoyiFronted/tests/e2e/edhr-recordbook-global-setting-static.spec.js`。

## API Contracts And Data States

- API unchanged: `GET/PUT /mes/pro/edhr-recordbook-setting/global`。
- Data state unchanged: `enabled=true/false` drives current switch state.

## BDD Scenarios

- BDD: 删除红框元信息 -> Given 金手指用户进入个人中心配置页 When eDHR 记录本全局开关卡片渲染 Then 不展示配置键、当前状态、最后更新人、最后更新时间元信息块。
- BDD: 蓝框区域可点击 -> Given eDHR 记录本全局开关卡片已加载 When 用户点击蓝框中的文字或开关区域 Then 触发同一套打开/关闭确认流程。

## RED / GREEN

- RED: `node tests/e2e/edhr-recordbook-global-setting-static.spec.js` -> FAIL, expected old component still rendered `el-descriptions` red-box metadata.
- GREEN: `node tests/e2e/edhr-recordbook-global-setting-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## UI State Checks

- Responsive: header remains flex layout with a compact clickable toggle area.
- Accessibility: toggle wrapper has `role="button"`, `tabindex="0"`, `aria-pressed`, and keyboard activation.
- Loading/saving: toggle area and switch are disabled while loading or saving.
- Error: existing load alert and API save error handling remain visible.
- Permission: profile config tab permission gating unchanged.

## Verification

- `node tests\e2e\edhr-recordbook-global-setting-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。

## Blockers

- None for this scope.
