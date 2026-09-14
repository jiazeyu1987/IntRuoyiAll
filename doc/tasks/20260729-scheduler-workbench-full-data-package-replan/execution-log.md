# Execution Log

## 2026-07-29

- USER: 查看当前手动重排需要哪些数据，并要求这些数据可以通过截图中的导入导出按钮承载；用户随后明确允许将全部数据包扩展到手动重排业务数据。
- READONLY: 已读取前端、后端、PowerShell、任务收尾规则和相关技能。
- READONLY: 已定位截图按钮在 `IntRuoyiFronted/src/views/mes/pro/scheduler-workbench/index.vue`，接口在 `/mes/pro/scheduler-workbench/*-config/*`。
- READONLY: 已确认手动重排直接请求字段为 `scheduleOrderIds/startTime/runtimeCapacityBasis/preserveManualLockedTasks/reason`，后端计算会读取排产工单、生产工单、工序快照、路线、工位产线、日历产能、任务保护、报工、用料、物料和库存。
- BDD: 全部数据包承载手动重排数据 -> Given 源环境存在可手动重排的排产工单、生产工单、路线配置、产能日历、任务保护、用料和库存 / When 用户点击排产员工作台“导出全部数据包”并在目标环境“导入全部数据包” / Then 数据包必须包含并回放手动重排所需业务数据，缺少必要字段或引用时 fail-fast。
- BDD: 路线配置包保持边界 -> Given 用户只点击“导出排产工艺路线” / When 导入路线配置包 / Then 只导入路线排产用途、排产配置和资源引用，不导入生产工单、排产工单、任务、库存或报工业务数据。
- READONLY: 已读取 `IntRuoyiBackend/docs/system/mes-scheduling-domain-contracts.md`，确认手动重排必须保留排产工单快照、任务扩展、日历产能、报工保护与物料库存链路。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增契约证明当前全部数据包导出缺少 `manualReplanDataPackage`，导入缺少手动重排数据包时未 fail-fast。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，full-config 导出包含手动重排数据包，导入缺包 fail-fast，导入结果透传重排数据计数。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchManualReplanDataPackageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，9 tests，覆盖 full-config 接线和手动重排数据包导入字段/行计数。
- GREEN: `node tests/e2e/mes-scheduler-workbench-import-timeout-static.spec.js` -> PASS，前端 full-config 导入超时、响应字段和成功提示计数静态契约通过。
- REGRESSION BLOCKER: `pnpm ts:check` -> FAIL，阻塞在非本任务文件 `src/views/mes/pro/edhr/ExecutionPage.vue(2765,5)` 的既有 `string | number` 传参类型错误；本任务未修改该文件。
- GREEN: `git diff --check` -> PASS，仅有 LF/CRLF 工作区提示，无 whitespace error。
- EXPERIENCE: 已按 `project-experience-consolidation` 合并长期经验到 `IntRuoyiBackend/docs/system/mes-scheduling-domain-contracts.md#手动重排数据包门禁`，并用 `rg -n "manualReplanDataPackage|手动重排数据包门禁" ...` 验证索引可定位。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260729-scheduler-workbench-full-data-package-replan\backend-api-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260729-scheduler-workbench-full-data-package-replan\frontend-feature-evidence.md` -> PASS。
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260729-scheduler-workbench-full-data-package-replan --mode preview` -> ready，无删除、无 blocked、无 warnings。
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260729-scheduler-workbench-full-data-package-replan --mode apply` -> applied，无删除、无 blocked、无 warnings。
- STATUS: 任务保持 `ready_for_closeout`；当前工作区仍存在其它任务/并行改动，本任务未触碰或清理这些文件。
