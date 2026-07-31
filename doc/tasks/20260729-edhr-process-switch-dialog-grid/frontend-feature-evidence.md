# Frontend Feature Evidence

## Feature Goal

把 eDHR 填写页“切换工序”弹框改为大尺寸 grid 卡片选择器，单屏至少展示 30 个工序卡片。

## Non-Goals

- 不修改后端接口、工序数据来源或权限判断。
- 不引入 fallback、mock 数据或默认成功分支。
- 不改变工序开始、批记录表单、表单槽位三条链路语义。

## Requirements

- REQ-1: 弹框尺寸接近页面主体区域，明显大于当前居中窄弹框。
- REQ-2: 工序候选以 grid 卡片展示，不使用纵向列表样式。
- REQ-3: 首屏展示区域至少容纳 30 个卡片。
- REQ-4: 保持现有工序点击切换、状态标签和提示信息。

## Acceptance

- AC-1: “切换工序”弹框使用 process-only 大尺寸宽度，任务/填写人切换弹框不受影响。
- AC-2: 工序候选渲染在 `data-assist-switch-process-grid` 中，每项是紧凑卡片。
- AC-3: 桌面布局使用 6 列 grid 和 64px 最小卡片高度，首屏可容纳至少 30 张卡片。
- AC-4: 既有工序候选来源、状态展示和点击切换处理函数保持不变。

## UI Entry Points

- eDHR 填写页顶部“工序”信息卡中的“切换”按钮。

## Owned Files

- `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
- `IntRuoyiFronted/tests/e2e/edhr-assist-process-switch-dialog-grid-static.spec.js`

## API Contracts And Data States

- 不新增或修改 API。
- 工序候选、状态和点击后导航继续复用现有前端数据与处理函数。

## BDD Scenarios

- BDD: 切换工序弹框大尺寸网格展示 -> Given 用户在 eDHR 填写页点击“切换工序”, When 弹框打开, Then 弹框宽高接近页面主体区域并以 grid 卡片展示工序，而不是纵向列表。
- BDD: 单屏展示至少三十个工序卡片 -> Given 当前批次存在三十个以上工序, When 用户打开切换工序弹框, Then 首屏网格区域无需逐条列表滚动即可容纳至少三十张紧凑工序卡片。
- BDD: 工序切换行为不变 -> Given 某工序可打开、已有执行记录或未开始, When 用户点击对应工序卡片, Then 仍沿用现有正式切换导航规则。

## Verification Plan

- RED: 新增或更新聚焦静态合同，先证明当前弹框仍是窄列表或缺少 grid 容量约束。
- GREEN: 修改组件后运行同一聚焦静态合同。
- REGRESSION: 运行相邻 eDHR 工序切换静态合同；如运行态齐备，执行真实页面只读检查。

## Responsive / Accessibility / States

- Process switch dialog uses a process-only large width and process-only CSS class.
- Process options use button cards, preserving keyboard focus behavior from the existing buttons.
- Grid uses 6 desktop columns, compact 64px minimum rows, and responsive fallbacks at 1200px and 900px.
- Status classes, status labels, active state, and click handlers remain unchanged.

## Blockers

- None for static/type verification.
