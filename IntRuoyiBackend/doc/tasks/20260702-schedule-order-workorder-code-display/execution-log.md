# Execution Log: 排产工单列表工单编号显示修复

BDD: 历史排产记录补齐工单编号 -> Given 排产工单记录存在 `workOrderId` 但历史冗余字段 `erpWorkOrderCode` 为空 / When 前端请求排产工单分页 / Then 响应中的 `erpWorkOrderCode` 应使用关联生产工单 `code`，列表显示工单编号。

BDD: 已有工单编号保持不变 -> Given 排产工单记录已经有 `erpWorkOrderCode` / When 前端请求排产工单分页 / Then 响应保持原字段，不影响既有显示与查询。

BDD: 工单编号仍可跳转 -> Given 排产工单存在生产工单 ID 和编码 / When 排产员点击工单编码 / Then 前端仍跳转到生产工单详情。

RED: `mvn -pl yudao-module-mes -Dtest=MesProScheduleOrderControllerTest#getScheduleOrderPage_backfillsWorkOrderCodeFromLinkedWorkOrderWhenScheduleCodeMissing test` -> FAIL, expected reason: 历史排产记录 `erpWorkOrderCode` 为空时响应没有从关联生产工单补齐工单编码。

GREEN: `mvn -pl yudao-module-mes -Dtest=MesProScheduleOrderControllerTest#getScheduleOrderPage_backfillsWorkOrderCodeFromLinkedWorkOrderWhenScheduleCodeMissing test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。

GREEN: `node tests/e2e/mes-schedule-order-workorder-link-static.spec.js` -> PASS, mes-schedule-order-workorder-link-static。

GREEN: `node tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js` -> PASS, MES schedule order freeze visibility static contract。

GREEN: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS, MES schedule order pool static contract。

Verification: 后端分页响应会在历史排产记录 `erpWorkOrderCode` 为空且 `workOrderId` 有效时，从关联生产工单补齐 `erpWorkOrderCode`；前端排产工单列表、冻结显示和工单池静态契约保持通过。

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260702-schedule-order-workorder-code-display/bug-regression-evidence.md` -> PASS, Bug regression evidence is valid。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260702-schedule-order-workorder-code-display --mode preview` -> PASS, status ready, blocked none；预览建议删除 `bug-regression-evidence.md`，本任务选择保留该证据文件，未执行 apply。
