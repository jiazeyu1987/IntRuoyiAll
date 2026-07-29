# Frontend Feature Evidence

## Feature Goal

让辅助填写页/原表填写页顶部“工序”切换保持在当前执行页内完成，而不是跳转到批次流程详情页。

## Non-goals

- 不修改后端接口契约。
- 不开放保存、提交、接管、跳过等写权限。
- 不改变填写人切换和 FormCenter 槽位导航链路。

## Requirements

- R1: 从执行页选择任意同批次普通工序后，仍停留在 `/mes/pro/feedback/edhr-execution/form`。
- R2: 非进行中、无工作任务或无执行记录的工序也可被选中查看。
- R3: 不再因缺少可查看执行记录或工作任务跳到流程页或弹阻断错误。
- R4: 状态颜色继续沿用批次执行口径，查看切换不提升写操作权限。

## UI Entry Points

- `src/views/mes/pro/edhr/ExecutionPage.vue`
- process switch menu: `data-assist-switch-menu="process"`

## API Contracts

- Batch detail read: `getEdhrBatchExecution(batchExecutionId)`
- Editable open still uses: `openEdhrBatchTask(...)`
- View-only context must remain on execution form route.

## Evidence

Pending.
