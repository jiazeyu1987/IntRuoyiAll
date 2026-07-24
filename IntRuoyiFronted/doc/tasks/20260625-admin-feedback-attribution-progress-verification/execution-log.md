# 执行日志：芋道源码租户模拟报工归属与排产进度核验

BDD: 芋道源码租户可完成模拟报工归属 -> Given admin 已登录芋道源码租户报工页签 / When 用户执行一轮模拟报工并选择订单工序归属 / Then 页面归属成功且真实写入发生。
BDD: 归属后排产工单进度正确更新 -> Given 某排产工单工序被成功归属正式报工 / When 归属成功后查看排产工单相关进度 / Then 已完成、剩余或相关进度字段与本次报工数量一致更新。
GREEN: experience-preflight -> PASS，用户已在当前任务明确授权对 `芋道源码/admin` 执行报工及相关数据写入；已读取登录门禁，后续仅走真实页面与现有业务接口。
GREEN: backend-health-old-runtime -> PASS，旧本机后端 `E:\Int\CacheData\IntRuoyi\runtime\backend-20260624-235419.jar` 在 `48081` 健康返回 `{"status":"UP"}`，确认基础本机环境可用。
RED: node doc/tasks/20260625-admin-feedback-attribution-progress-verification/verify-admin-feedback-attribution-progress.e2e.js -> FAIL，真实链路中 `芋道源码/admin` 登录成功、模拟报工成功、选择归属成功，但排产工单工序 `reportedQuantity` 未按归属数量更新，失败断言：`before=0, fill=99, after=0`。根因为后端运行逻辑只把 `FINISHED` 报工计入已报工数量，未计入模拟归属后立即生成的 `APPROVING` 正式报工。
RED: MesProScheduleOrderProgressServiceTest -> FAIL，旧业务契约 `syncFeedbackProgress_shouldIgnoreApprovingFeedbackCreatedByImportAttributionForCompletedQuantity` 证明 `APPROVING` 报工不会更新排产工单进度，与本次用户期望冲突。
GREEN: mvn -pl yudao-module-mes -Dtest="MesProScheduleOrderProgressServiceTest,MesProScheduleOrderFourRiskContractTest" test -> PASS，8 个测试通过；回归覆盖 `APPROVING` 与 `UNCHECK` 已归属报工计入排产工单已报工数量。
GREEN: mvn -pl yudao-module-bpm,yudao-module-mes -am -DskipTests package -> PASS，模块级打包通过，用于生成本地验证运行包。
GREEN: backend-health-patched-runtime -> PASS，生成本地验证包 `E:\Int\CacheData\IntRuoyi\runtime\backend-mes-progress-stored-singleclass-20260625.jar`，保持 `BOOT-INF/lib/yudao-module-mes-2026.04-SNAPSHOT.jar` 为 `ZIP_STORED`，启动 `48081` 后健康返回 `{"status":"UP"}`。
GREEN: node doc/tasks/20260625-admin-feedback-attribution-progress-verification/verify-admin-feedback-attribution-progress.e2e.js -> PASS，真实链路使用 `芋道源码/admin` 完成模拟报工与选择归属；`importRecordId=454`，`feedbackId=263`，归属数量 `99`，`scheduleOrderId=13`，`scheduleOrderProcessId=296`，归属后工序 `reportedQuantity: 0 -> 99`，`remainingQuantity: 99 -> 0`。
