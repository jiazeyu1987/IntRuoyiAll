# Task: BPM 报销流程后端交付

## Goal

在后端仓库中交付可实际使用的报销流程能力，包括业务表、接口、流程启动与审批结果联动，并确保审批人为发起人的部门负责人。

## Previous Task Check

- Previous backend task: `doc/tasks/20260514-clean-backend-worktree-residuals/task.md`
- Status before this task: completed
- Impact: backend worktree hygiene is already fixed, so this task can focus on source and evidence changes only.

## Milestones

- [x] M1: 检查上一条后端任务状态并创建本任务文档。
- [x] M2: 记录 BDD 场景与后端 RED 证据。
- [x] M3: 复用现有 BPM 动态表单与流程定义能力，完成运行时流程配置，不新增业务表结构。
- [x] M4: 打通部门负责人审批链路的运行时校验与负责人待办验证。
- [x] M5: 运行后端验证并补齐证据文档。

## Expected Verification

- 运行时存在可发起的动态表单报销流程定义 `oa_expense`。
- 创建报销流程实例时会使用现有 BPM 流程表单机制，不引入额外业务表结构。
- 发起人位于 `Expense Verify Dept` 时，流程会把首个审批任务分配给该部门负责人 `expenselead1`。

## Current Status

Completed. The backend runtime now contains active process definition `oa_expense` version 2 and the department-leader approval chain has been validated through real instance creation and completion.

## Final Verification Result

- Active definition id: `oa_expense:2:6f8f51f8-4f44-11f1-8763-00155db32d8f`
- Verified approved instance id: `8d5e3e20-4f44-11f1-8912-00155db32d8f`
- Verified submitter: `expenseuser1` (`Expense Verify Dept`)
- Verified approver: `expenselead1`
