# 任务：DCC 培训执行确认时间格式与文件名称列

## 目标

调整 `DCC培训 -> 培训执行` 表格：

- 将 `确认完成时间` 从原始时间戳显示改为 `YYYY-MM-DD`
- 在现有列中增加 `文件名称` 列，保留现有 `文件标题`、`文件编号` 等契约不变

## 前置任务检查

- 已检查上一条前端任务文档 `doc/tasks/20260519-showroom-remediation-f3-admin-workflow-workbenches/task.md`
- 状态：已完成
- 结论：允许开始当前任务

## 里程碑

- [x] M1：记录 BDD 场景并补充失败测试
- [x] M2：最小化修改培训执行表格列和日期展示
- [x] M3：运行前端验证并更新证据
- [x] M4：运行 closeout 预览并准备仅提交本任务文件

## 范围

- `src/views/dcc/controlled-file/training/components/TrainingExecutionTab.vue`
- `src/api/dcc/controlledFile/training.ts`
- `src/utils/formatTime.ts`
- `scripts/dcc-training-execution-table.test.mjs`
- `doc/tasks/20260519-dcc-training-execution-date-and-file-name/**`

## 非范围

- 不修改后端接口契约
- 不改 `我的培训` 页面
- 不调整培训执行筛选条件、分页、权限或数据来源

## 预期验证

- `node --test scripts/dcc-training-execution-table.test.mjs`
- `pnpm exec eslint src/views/dcc/controlled-file/training/components/TrainingExecutionTab.vue scripts/dcc-training-execution-table.test.mjs`

## 当前状态

已完成。前端验证、evidence 校验与 closeout 预览均已通过，待在前端仓库提交本任务文件。

## Cleanup Keep

- `doc/tasks/20260519-dcc-training-execution-date-and-file-name/frontend-feature-evidence.md`
