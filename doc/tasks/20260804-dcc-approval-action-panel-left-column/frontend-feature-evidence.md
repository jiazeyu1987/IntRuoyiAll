# Frontend Feature Evidence

## Feature Goal

将 DCC 上传审批处理页的审批操作区移动到左侧文件信息栏下方。

## Non-Goals

- 不修改审批动作、权限、API、状态或签名流程。
- 不修改右侧附件预览链路。
- 不调整普通详情页、只读预览页或追溯页。

## Requirements And Acceptance IDs

- AC-1：审批要求、当前任务与操作按钮位于左侧文件信息栏下方。
- AC-2：右侧附件预览保持现有正式预览链路。
- AC-3：页面底部不重复渲染审批操作区。
- AC-4：窄屏下沿用现有两列转单列响应式行为。

## UI Entry Points, Routes, Components, And Owned Files

- 入口：审批中心 DCC 待办的模块处理入口。
- 路由：`/dcc/controlled-file/detail/:id?handling=approval&from=approval-center`
- 组件：`IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue`
- 测试：待新增聚焦静态合同。

## API Contracts And Data States

- 沿用 `approvalTodoTask`、`approvalActionLabels`、`approvalLoading` 与既有动作处理器。
- 空任务态继续显示“当前没有待处理审批任务”。
- 不新增 API 或数据转换。

## BDD Scenarios

- Given 用户进入 DCC 上传审批处理页，When 页面完成加载，Then 左侧文件信息下方显示审批操作区，右侧显示附件预览，底部无重复操作区。

## RED Command And Expected Failure

- Pending.

## GREEN Command And Passing Result

- Pending.

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Responsive：沿用 `el-row` / `el-col` 现有布局，聚焦合同验证操作区与左列同域。
- Accessibility：保留现有 Element Plus 按钮语义和文本。
- Loading：保留 `approvalLoading`。
- Empty：保留无待办任务空态。
- Error：不改变既有错误呈现。
- Permission：不改变审批动作权限和任务判定。

## E2E Or Component Verification Path

- 聚焦静态合同验证 DOM 归属和无重复渲染。
- 相邻 `dcc-approval-upload-view-static.spec.js` 验证审批处理页正式链路。

## Blockers And Follow-Up Skills

- 当前无产品前置阻塞；Git 工作区需先按规则保存既有脏改动基线。
