# Frontend Feature Evidence

## Feature Goal And Non-Goals

- 目标：审批中心四个列表视图增加独立“申请人”列，并避免 DCC 摘要重复展示申请人。
- 非目标：不修改审批中心 API、后端数据来源、权限、审批动作、筛选、分页或页面整体视觉。

## Requirements And Acceptance IDs

- AC-1：待办、已办、我发起的、抄送列表均存在“申请人”独立列。
- AC-2：申请人列使用正式 `initiatorUserId`，不从业务摘要或其它字段猜测。
- AC-3：DCC 业务摘要不重复展示申请人。
- AC-4：四个视图升级稳定 table key，使新默认列对既有用户生效。
- AC-5：显示字段、列宽保存、排序、分页、错误和审批动作保持现有契约。

## UI Entry Points, Routes, Components, And Owned Files

- 路由：`/approval-center/todo`、`/approval-center/done`、`/approval-center/my-initiated`、`/approval-center/cc`。
- 组件：`IntRuoyiFronted/src/views/approval-center/index.vue`。
- 测试：审批中心申请人列聚焦静态合同和四个标准列表合同。

## API Contracts And Data States

- 数据源：`ApprovalTaskSummaryVO.initiatorUserId`。
- 有值：显示 `用户 #<id>`，与现有申请人摘要语义一致。
- 无值：显示既有审批中心空值标识，不新增默认成功或猜测来源。
- Loading、empty、error、permission 状态保持现有实现。

## BDD Scenarios

- Given 审批任务有正式申请人 ID，When 任一审批中心列表渲染，Then 业务摘要后显示独立申请人列。
- Given DCC 审批任务渲染业务摘要，When 独立申请人列存在，Then DCC 关键字段不再重复申请人。
- Given 用户存在旧列配置，When 新版本加载审批中心，Then 新 table key 使用包含申请人的默认列集合。

## RED Command And Expected Failure

- Pending.

## GREEN Command And Passing Result

- Pending.

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Responsive：沿用 Element Plus 表格横向滚动和现有列宽策略。
- Accessibility：保留表头文本和单元格 title。
- Loading/Empty/Error：不修改。
- Permission：不修改。

## E2E Or Component Verification Path

- 任务专用静态合同覆盖列位置、正式字段、重复摘要移除和 table key 升级。
- 四个标准列表合同验证统一列表、用户列配置和审批动作无回归。
- `pnpm ts:check` 验证 Vue/TypeScript。

## Blockers And Follow-Up Skills

- 当前无 blocker。
