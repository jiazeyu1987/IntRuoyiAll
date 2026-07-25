# Execution Log

用户要求：解决图1 `光固Ⅰ` 和图2 `清洁` 截图位置识别错误时，不做某个表单的特例；用适合所有表格的全局方式解决，并用这两个表单验证。

## BDD

- BDD: packed material continuation lines stay in one material cell -> Given Word 表格中的 packed 物料矩阵包含多行物料名称续行（例如括号说明）, When 批记录导入展开该 packed 矩阵, Then 续行应合并回前一个物料名称，后续物料不应整体错位。
- BDD: detail operation area stops before self-inspection block -> Given 工序生产操作明细表后紧跟 `生产自检` 等说明区块, When 解析和校验操作明细区域, Then 明细区域不得把说明区块误算为操作明细截图范围。

## Command And Evidence Log

- PRECHECK: `git status --short --branch` -> PASS，当前分支 `int_main`，工作区已有大量其他任务改动；本任务只修改批记录解析相关文件和 `doc/tasks/20260725-batch-record-global-table-position-fix/`。
- PRECHECK: `docs/task-closeout-rules.md`, `docs/backend-development.md`, `docs/powershell-encoding.md`, `bug-regression-fix-loop` skill and `bug-contract.md` read -> PASS。
- PRECHECK: `docs/experience-index.md` read -> PASS；适用门禁已摘入 `task.md`。

## Current Status

in_progress
