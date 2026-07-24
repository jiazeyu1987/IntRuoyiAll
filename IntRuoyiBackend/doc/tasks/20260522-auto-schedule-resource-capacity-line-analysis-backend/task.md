# Task: 自动排产资源产能与工单产线分析后端改造

## Goal

将 MES 自动排产算法改为“按工单数量 + 工序资源产能”驱动，并补充可复用于预览与正式排程的工单产线分析后端接口。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\schedule\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\controller\admin\pro\schedule\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\schedule\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-auto-schedule-resource-capacity-line-analysis-backend\**`

## Non-Scope

- 不修改数据库 schema。
- 不调整工作站主数据录入页面。
- 不修改排程日历月视图/日详情既有接口字段结构。
- 不自动修复历史跨产线脏数据。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-auto-schedule-material-shortage-reality-check\task.md`
- Status before this task: `Completed`
- Impact: 不存在未完成前置任务阻塞，可在当前排产切片基础上继续改造算法。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在与本任务无关的并行改动和未追踪文件。
- Impact: 本任务只允许修改 MES 排产相关后端代码、定向测试和本任务文档，提交时单独暂存。

## Milestones

- [x] M1: 建立任务文档，补 BDD 场景并锁定接口/错误语义。
- [x] M2: 先写 RED 测试，覆盖固定单产线、工序资源产能、瓶颈工序和工单分析接口。
- [x] M3: 实现自动排产内核改造与预览分析输出。
- [x] M4: 实现正式排程工单产线分析接口与历史跨产线冲突显式返回。
- [x] M5: 跑定向后端测试、回写证据、执行 closeout preview，并按任务单独提交。

## Expected Verification

- `mvn -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest,MesProScheduleCalendarServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-auto-schedule-resource-capacity-line-analysis-backend\backend-api-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-auto-schedule-resource-capacity-line-analysis-backend --mode preview`

## Current Status

Completed.

## Completed Work

- 自动排产改为固定单产线排产：同一工单整条路线只会选一条产线，不再按工序逐段切线。
- 自动排产的工序计划时长改为按工序资源产能计算：
  - 有设备时按设备标准小时产能与设备数量汇总
  - 无设备时按工作站单人标准小时产能与当前在岗人数计算
  - 同产线同工序多个工作站能力会汇总后参与排产
- 排产资源池的可用时间改为按 `lineId + processId` 维度维护，不再整条产线串行锁死。
- 预览响应新增 `workOrderAnalyses`，可返回工单归属产线、各工序数量、资源模式、有效小时产能和瓶颈工序。
- 正式排程新增 `GET /mes/pro/schedule-calendar/work-order-analysis` 接口。
- 正式排程工单若历史任务跨多条产线，接口会返回显式 `conflict=true` 和冲突文案，不会静默挑线。

## Verification Result

- PASS: `mvn -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest,MesProScheduleCalendarServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `git diff --check -- yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImpl.java yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProScheduleCalendarServiceImpl.java yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/schedule/MesProScheduleCalendarController.java yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProScheduleCalendarService.java yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/schedule/vo/MesProAutoSchedulePreviewRespVO.java yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/schedule/vo/calendar/MesProScheduleCalendarWorkOrderAnalysisRespVO.java yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImplTest.java yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProScheduleCalendarServiceImplTest.java doc/tasks/20260522-auto-schedule-resource-capacity-line-analysis-backend/task.md doc/tasks/20260522-auto-schedule-resource-capacity-line-analysis-backend/execution-log.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-auto-schedule-resource-capacity-line-analysis-backend\backend-api-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-auto-schedule-resource-capacity-line-analysis-backend --mode preview`

## Notes

- `backend-api-evidence.md` 已通过校验，并按 closeout preview 规则作为附属证据文件清理，不纳入提交。
