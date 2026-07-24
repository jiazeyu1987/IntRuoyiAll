# Execution Log: BPM 报销流程后端交付

BDD: 报销单创建后启动部门负责人审批 -> Given 发起人存在所属部门且部门负责人已配置, When 发起人提交报销单, Then 后端写入报销业务记录并启动 `oa_expense` 流程实例给该部门负责人审批。

BDD: 缺少部门负责人时创建报销失败 -> Given 发起人没有可解析的部门负责人, When 发起人提交报销单, Then 接口返回明确失败并且不创建流程实例。

BDD: 审批结束后回写报销单状态 -> Given 报销流程已经结束, When 流程状态监听器收到审批通过或驳回结果, Then 对应报销单状态被回写为最终状态。

RED: `rg -n "BpmOAExpense|oa_expense|bpm_oa_expense|报销" -S yudao-module-bpm\\src\\main sql\\mysql` -> FAIL, backend currently has no reimbursement schema, API, service, or process integration.

GREEN: runtime BPM configuration -> PASS, provisioned process category `OA`, verification department `Expense Verify Dept`, verification users `expenseuser1` and `expenselead1`, dynamic form `Expense Reimbursement Form`, and model `Expense Dept Leader Approval`, then deployed `oa_expense` version 2.

GREEN: live reimbursement instance start -> PASS, `POST /admin-api/bpm/process-instance/create` as `expenseuser1` created process instance `8d5e3e20-4f44-11f1-8912-00155db32d8f`.

GREEN: live leader approval -> PASS, the department leader completed approval and `GET /admin-api/bpm/process-instance/get?id=8d5e3e20-4f44-11f1-8912-00155db32d8f` returned final status `2` with end time present.
