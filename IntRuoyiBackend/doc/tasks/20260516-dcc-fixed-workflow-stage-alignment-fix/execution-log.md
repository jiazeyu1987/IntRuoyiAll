# Execution Log: DCC 固定四层审批阶段对齐修复

BDD: 第一层文控审核必须能在真实固定四层待办上通过 -> Given 一个真实提交成功且类别已派生固定四层路线的 DCC 文件进入 `PENDING_DOC_CONTROL_REVIEW` and 当前用户拥有该层真实待办 / When 用户对该文件执行第一层审批通过 / Then 后端必须接受该待办并推进到下一固定阶段，而不是报阶段错位。

BDD: 固定四层阶段解析必须与 BPM 当前节点一致 -> Given BPM 当前待办节点、流程变量和 DCC 文件状态均来自同一条真实流程实例 / When 后端解析当前审批动作所属的固定 DCC 阶段 / Then 解析结果必须与 `文控审核 / 审核会签 / 批准 / 文控批准` 之一精确对齐，不得出现通用 `approveTask` 与固定阶段错位。

BDD: 真实审批失败必须暴露精确 blocker -> Given 固定四层审批链在真实运行时发生阶段映射异常 / When 前端或接口提交审批通过动作 / Then 后端必须返回精确错误并保留 fail-fast 行为，不得静默降级或自动跳过阶段校验。

- M1: Completed. Backend task document created after the real E2E reproduced the live blocker and before any backend code changes.
- RED: `POST /admin-api/dcc/controlled-files/2054545668044042245/approve-task` with the real pending task id `45b55bae-508d-11f1-9f49-00155d09335a` -> FAIL, response `500 Controlled file task stage is not aligned with the fixed DCC workflow`.
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest#submitControlledFile_success -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, `PROCESS_START_USER_SELECT_ASSIGNEES` is null because submit still writes all fixed stages into `PROCESS_APPROVE_USER_SELECT_ASSIGNEES`.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest,DccBaseSchemaTest,DccCategoryApprovalMatrixAdminServiceImplTest,DccApprovalRouteAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 45 tests passed with the generic `approveTask` regression covered.
GREEN: `npx --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-four-stage-approval-e2e\scripts\verify-dcc-four-stage-approval-e2e.mjs` -> PASS, the real file `2054545668044042256` advanced through `DOC_CONTROL_REVIEW -> MATRIX_REVIEW -> MATRIX_APPROVAL -> DOC_CONTROL_APPROVAL` and ended in backend status `FINALIZING`.
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest#submitControlledFile_success -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS after setting `startUserSelectAssignees` on the BPM start DTO.
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest,DccCategoryApprovalMatrixAdminServiceImplTest,DccApprovalRouteAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 41 tests green.
- GREEN: `PUT /admin-api/bpm/model/update-bpmn` + `POST /admin-api/bpm/model/deploy?id=44a108c8-4eb4-11f1-950d-00155db32d8f` -> PASS, DCC runtime BPM model redeployed from a single generic `approveTask` node to fixed nodes `DOC_CONTROL_REVIEW`, `MATRIX_REVIEW`, `MATRIX_APPROVAL`, `DOC_CONTROL_APPROVAL`.
- GREEN: Local backend restart required explicit datasource overrides to `127.0.0.1:23306`, because fresh startup with the checked-in `3306` datasource failed authentication for `root`.
- GREEN: Real Playwright E2E advanced a live controlled file through all four signatures and reached frontend status `发布处理中`.
