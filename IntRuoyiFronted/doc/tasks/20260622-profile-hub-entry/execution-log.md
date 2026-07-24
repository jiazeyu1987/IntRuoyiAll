# Execution Log: 20260622 个人中心聚合入口版

BDD: 用户可在个人中心完成当前账号资料维护 -> Given 用户进入个人中心 When 切换到资料或改密页签 Then 仍可使用现有个人资料与修改密码能力。

BDD: 用户可在个人中心看到审批与任务入口汇总 -> Given 用户进入个人中心 When 查看工作台页签 Then 可看到 BPM、DCC、eDHR 的任务汇总、快捷入口和关键说明。

BDD: 个人中心显式保留模块边界 -> Given DCC 与 eDHR 不能走 BPM 通用审批 When 用户点击对应入口 Then 应跳转到各自真实处理页，而不是伪造统一审批按钮。

BDD: 无权限模块在个人中心不误导用户 -> Given 当前账号缺少某模块权限 When 进入工作台页签 Then 该模块应显示无权限或不展示入口，不得报错或假装可用。

RED: node scripts/profile-hub-entry-static.test.mjs -> FAIL, 缺少 `src/views/Profile/components/ProfileWorkbench.vue`，个人中心尚未实现聚合入口工作台组件

INFO: Workbench design -> 复用现有 `/user/profile`，新增 `ProfileWorkbench.vue` 作为聚合入口页签，不新增后端聚合接口。

INFO: BPM aggregation -> 通过 `getTaskTodoPage`、`getTaskDonePage`、`getProcessInstanceMyPage` 汇总待办、已办、我的申请，并跳转既有 `/bpm/task/todo`、`/bpm/task/done`、`/bpm/process-instance/my`。

INFO: DCC aggregation -> 通过 `getTaskTodoPage + processDefinitionKey` 汇总 DCC 审批待办，通过 `getMyTrainingTaskPage` 汇总培训，通过 `getDccElectronicSignaturePage` 按当前用户 `signerUserId` 汇总签名记录。

INFO: eDHR aggregation -> 通过 `getEdhrWorkTaskMyPage`、`getEdhrWorkTaskCandidateTodoPage`、`getEdhrWorkTaskDonePage`、`getEdhrWorkTaskStats`、`getEdhrExecutionSignaturePage` 汇总工作任务、候选审核、已处理、逾期与签名记录。

INFO: Permission boundary -> 使用 `checkPermi` 对 BPM / DCC / eDHR 分区与快捷入口做权限判断；无权限时显示“无权限”，不伪造可用能力。

GREEN: node scripts/profile-hub-entry-static.test.mjs -> PASS

GREEN: node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json -> PASS
