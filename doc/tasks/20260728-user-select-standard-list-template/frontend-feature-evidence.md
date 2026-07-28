# Frontend Feature Evidence

## Feature Goal

人员选择弹窗右侧用户列表区域改为项目标准列表模板，保持筛选、部门树、选择、分页和确认行为不变。

## Non-Goals

- 不修改后端接口契约。
- 不修改人员、部门数据来源。
- 不引入 fallback、mock 数据或默认成功状态。

## Requirements And Acceptance

- A1: 人员选择弹窗用户列表必须接入 `UnifiedListTemplate`。
- A2: 用户列表列必须接入显示字段/列宽持久化配置。
- A3: 模板区域保留搜索条件外部布局、空状态、选择列、分页和确认按钮链路。

## UI Entry Points

- `IntRuoyiFronted/src/views/system/user/components/UserSelectDialogV2.vue`

## API Contracts

- 保留现有用户查询、部门树和分页接口调用方式。

## BDD Scenarios

- Given 用户打开人员选择弹窗并查看右侧用户列表 When 列表渲染 Then 红框区域由标准列表模板承载，显示字段配置和重置入口来自模板，用户列继续按用户编号、用户名称、用户昵称、部门、手机号、创建时间展示。

## Verification Path

- `node IntRuoyiFronted/tests/e2e/user-select-standard-list-template-static.spec.js`

## Blockers

- 无。

