# Execution Log: BPM 报销流程前端交付

BDD: 发起人可以在前端提交报销单 -> Given 发起人进入报销发起页, When 填写报销信息并提交, Then 前端调用真实报销创建接口并提示发起成功。

BDD: 发起页展示部门负责人审批时间线 -> Given 报销流程定义已经部署, When 发起页加载并且表单金额可用于流程预测, Then 页面展示当前流程的审批时间线并体现部门负责人审批节点。

BDD: 用户可以查看自己的报销单与流程状态 -> Given 用户已提交报销单, When 用户打开报销列表并进入详情, Then 页面展示报销业务字段与对应流程状态。

RED: `rg -n "报销|reimburse|expense|oa/expense" -S src\\views src\\api src\\router` -> FAIL, frontend currently has no reimbursement route, no reimbursement API module, and no reimbursement pages.

GREEN: BPM frontend entry repair -> PASS, added hidden routes for `BpmProcessInstanceCreate`, `BpmProcessInstanceMy`, and `BpmTodoTask` so the current app can open process start and todo pages without 404.

GREEN: Playwright submitter flow -> PASS, `expenseuser1` opened the start page on `http://127.0.0.1:8081`, selected the reimbursement definition card, filled the dynamic form, and triggered live instance `8d5e3e20-4f44-11f1-8912-00155db32d8f`.

GREEN: Playwright leader approval flow -> PASS, `expenselead1` opened the todo page, entered the process detail page for the new instance, submitted approval, and saved `expense-leader-approve.png`.
