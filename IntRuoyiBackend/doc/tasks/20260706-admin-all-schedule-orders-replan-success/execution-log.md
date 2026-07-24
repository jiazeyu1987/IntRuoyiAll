# 执行日志

BDD: admin 全量排产工单重排成功 -> Given 用户要求使用芋道源码/admin真实数据对当前所有排产工单进行排产, When 对当前未完成排产工单执行预览、补齐缺口并应用重排, Then 应用重排成功且不再存在班次产能缺失/可补齐基础数据阻断。
GREEN: experience-preflight -> PASS, 已读取 PowerShell、经验索引、登录方式；用户明确授权本机 `芋道源码/admin` 真实数据写入型排产验证。

BDD: startTime 字符串按自然日起排 -> Given 前端发送 `2026-07-06 00:00:00`, When 后端反序列化手动重排 startTime, Then 计算锚点保持为 `2026-07-06 00:00:00` 而不是 1970。
RED: mvn -pl yudao-framework/yudao-common -Dtest=JsonUtilsTest#testTimestampLocalDateTimeDeserializer_shouldParseFormattedDateTimeString test -> FAIL, 旧反序列化得到 `1970-01-01T08:00`。
GREEN: mvn -pl yudao-framework/yudao-common -Dtest=JsonUtilsTest#testTimestampLocalDateTimeDeserializer_shouldParseFormattedDateTimeString test -> PASS。

BDD: 当前路线已删除但工单快照仍有剩余量的工序必须生成活动任务 -> Given 排产工单快照存在剩余工序且当前路线缺失该工序, When 手动重排预览, Then 使用排产工单快照补入计算路线并生成任务，不产生 `ACTIVE_TASK` 阻断。
RED: mvn -pl yudao-module-mes -am -Dtest=MesProAutoScheduleAlgorithmContractTest#preview_shouldScheduleRemainingSnapshotProcessWhenCurrentRouteRemovedIt test -> FAIL, 未补入快照工序时生成任务数量不足且存在活动任务承接缺口。
GREEN: mvn -pl yudao-module-mes -am -Dtest=MesProAutoScheduleAlgorithmContractTest#preview_shouldScheduleRemainingSnapshotProcessWhenCurrentRouteRemovedIt+preview_shouldMatchScheduleOrderProcessByRouteSortWhenRouteProcessIdDrifted -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test -> PASS, Tests run: 2, Failures: 0, Errors: 0。
GREEN: mvn -pl yudao-module-mes -am -Dmaven.test.skip=true package -> PASS。

GREEN: backend-runtime-update -> PASS, `yudao-server-exec.jar` 写入更新后的 `yudao-common-2026.04-SNAPSHOT.jar` 与 `yudao-module-mes-2026.04-SNAPSHOT.jar`，两者均为 `compress_type=0`。
GREEN: backend-health -> PASS, `http://127.0.0.1:48081/actuator/health` 返回 HTTP 200。
GREEN: real-e2e-admin-apply-replan -> PASS, Playwright 真实登录 `http://localhost:8081`，租户 `芋道源码`、账号 `admin`，选择当前 9 个排产工单 `[124,88,117,118,119,120,121,122,123]`，`startTime=2026-07-06 00:00:00`，preflight PASS，preview `generatedTaskCount=432`、`blockingIssueCount=0`、`capacityMissingMessages=[]`、`hasCalendarContextToken=true`，apply `applied=true`、`createdTaskIds=432`、`deletedTaskIds=404`、`preservedTaskIds=2`。
GREEN: readonly-db-verification -> PASS, 9 个排产工单均有任务；`全检导丝(process_id=900378)` 在 9 个排产工单均生成任务；总活动任务 434 个，最早 `2026-07-06 08:00:00`，最晚 `2026-08-27 13:30:00`，早于 `2026-07-06 00:00:00` 的任务数为 0。