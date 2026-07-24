# Execution Log: MES 排产物料校验统一为工单物料需求口径

- 2026-05-22 13:25: 已检查上一条同仓任务 `20260522-showroom-product-narration-restart-and-monitor` 状态为 `Completed`，不阻塞本次任务。
- BDD: 自动排产按工单物料需求校验库存 -> Given 工单 BOM 存在半成品/产品节点且工单详情页“物料需求”已展开为叶子物料 / When 生成自动排产预览 / Then 短缺校验只按叶子物料需求计算，不再按工单 BOM 原始行计算。
- BDD: 缺少工单物料需求时只给 warning -> Given 某工单没有可展开的物料需求 / When 生成自动排产预览或发布排产 / Then 系统返回 `工单缺少物料需求` warning，但不阻塞生成或发布。
- BDD: 排程日历短缺统计与工单物料需求同口径 -> Given 排产任务已落到某日 / When 查看月视图或日详情短缺统计 / Then 短缺数量、短缺量与工单详情页“物料需求”口径一致。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProWorkOrderBomServiceImplTest,MesProWorkOrderBomControllerTest,MesProAutoScheduleServiceImplTest,MesProScheduleCalendarServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL（旧实现预期），原因是旧代码仍直接按工单 BOM 原始行做排产与日历短缺计算，且缺少物料需求时会走 BOM 阻塞语义。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProWorkOrderBomServiceImplTest,MesProWorkOrderBomControllerTest,MesProAutoScheduleServiceImplTest,MesProScheduleCalendarServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，28 tests 全部通过。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-mes-auto-schedule-material-demand-unification\backend-api-evidence.md` -> PASS，后端证据结构满足技能契约。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-mes-auto-schedule-material-demand-unification --mode preview` -> PASS，预览结果仅保留 `task.md / execution-log.md`，`backend-api-evidence.md` 被识别为可清理附属证据。
