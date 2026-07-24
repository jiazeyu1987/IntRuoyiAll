# Execution Log：排产工单人工完成与未完成筛选

BDD: 排产员可将排产工单人工设为已完成 -> Given 排产员拥有人工完成权限且工单未完成未取消 / When 填写原因并二次确认完成 / Then 系统把该工单标记为人工完成，订单级进度变为 100%，并写入 MANUAL_FINISH 追溯日志。
BDD: 人工完成后报工同步不得覆盖订单级完成口径 -> Given 工单已被人工完成 / When 后续有报工同步刷新 / Then 工序明细继续按真实报工更新，但工单汇总仍保持已完成与 100%。
BDD: 管理员可撤销人工完成 -> Given 工单已人工完成且当前用户拥有撤销权限 / When 填写原因并二次确认撤销 / Then 系统清除人工完成字段，按真实报工重新计算汇总，并写入 REVOKE_MANUAL_FINISH 追溯日志。
BDD: 排产工单默认筛选未完成 -> Given 用户打开排产工单列表 / When 页面请求分页接口且未主动切换筛选 / Then 后端只返回待排产、已排产、生产中的工单，不返回已完成与已取消。

GREEN: experience-preflight -> PASS，本轮高风险动作限定为本机 Docker MySQL schema/menu/role 变更与本机 8081/48081 真实验证；已读取 PowerShell 与登录门禁，不触碰测试服/正式服。
RED: `@' ... git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro show HEAD:yudao-module-mes/.../MesProScheduleOrderController.java 与相关 VO 文件，再断言 manualFinish( / revokeManualFinish( / completionFilter / manualFinishedReason 存在 ... '@ | python -X utf8 -` -> FAIL，旧版 `HEAD` 缺少人工完成接口与完成筛选合同，`AssertionError: HEAD missing token: manualFinish(`。
RED: `@' ... git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro show HEAD:sql/mysql/20260629_mes_schedule_order_manual_finish.sql ... '@ | python -X utf8 -` -> FAIL，旧版 `HEAD` 不存在本次正式迁移，返回 `fatal: path 'sql/mysql/20260629_mes_schedule_order_manual_finish.sql' exists on disk, but not in 'HEAD'`。
GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProScheduleOrderServiceImplTest,MesProScheduleOrderControllerTest,MesProScheduleOrderProgressServiceTest,MesProScheduleOrderRespVOContractTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，`44 tests` 全绿。
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_schedule_order_manual_finish_sql.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q` -> PASS，`15 passed`。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-manual-finish-real-flow.e2e.js` -> PASS，结果 `status=PASS`、`scheduleOrderId=9`、`workOrderCode=CODexERP20260610B`、`plannerUsername=smokeplan1`、`adminUsername=smokeappr1`。
GREEN: `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro -e "UPDATE mes_pro_schedule_order SET manual_finished_time = NULL, manual_finished_by = NULL, manual_finished_reason = NULL WHERE id = 8 AND tenant_id = 122 AND status = 0 AND manual_finished = b'0'; ..."` -> PASS，仅修正测试租户 `id=8` 这条旧运行态残留脏数据，回查结果 `manual_finished=0` 且时间/原因字段已清空。
