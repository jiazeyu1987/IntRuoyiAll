# Frontend Feature Evidence

## Feature Goal

生产组长人员管理不再按启用状态分组，已禁用和未禁用人员在同一列表展示；已禁用人员显示名使用红色文字。

## Non-goals

- 不修改后端接口或数据结构。
- 不修改新增、编辑、启用、禁用和重置签名密码操作。
- 不修改 PQC 人员管理。
- 不增加 fallback、mock 或静默错误处理。

## Requirements

- AC-1：生产人员列表不显示启用状态筛选。
- AC-2：生产人员列表请求不携带 `enabled` 过滤条件。
- AC-3：禁用和未禁用人员共享同一个表格和分页。
- AC-4：`enabled === false` 的人员显示名为红色。

## UI Entry And Owned Files

- Route: `/mes/pro/process-pool/production-leader`
- Component: `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- Test: `IntRuoyiFronted/tests/e2e/production-personnel-unified-status-list-static.spec.cjs`

## API Contract And Data States

- 继续调用 `getProductionPersonnelList`。
- 不传 `enabled` 时返回当前生产组长关联的全部人员。
- `row.enabled === false` 表示禁用；其它值按未禁用展示。
- 接口错误继续由现有 `refreshProductionPersonnel` 错误链路暴露，不新增降级。

## BDD Scenarios

- Given 当前组长同时关联已禁用和未禁用人员, When 打开人员管理, Then 不显示状态筛选且两类人员出现在同一分页列表。
- Given 列表行已禁用, When 渲染显示名, Then 姓名为红色；未禁用行保持普通文字。

## RED

- `node tests\e2e\production-personnel-unified-status-list-static.spec.cjs` -> FAIL，生产人员区域仍渲染 `productionPersonnelQuery.enabled` 状态分组控件。

## GREEN

- Pending.

## Responsive And Accessibility

- 删除固定宽度筛选控件后不会增加窄屏拥挤。
- 红色仅作为附加视觉提示；“状态”列仍保留“已禁用 / 可选择”文字，不依赖颜色单独传达状态。

## Loading Empty Error Permission

- 继续复用现有 loading、空表格、错误提示和权限边界。
- 不改变人员写操作权限和确认弹框。

## Verification Path

- 聚焦静态合同。
- 相邻人员新增与头部结构静态合同。
- `pnpm ts:check`。
- 真实前置齐备时运行生产人员管理 Playwright。

## Blockers

- Pending.
