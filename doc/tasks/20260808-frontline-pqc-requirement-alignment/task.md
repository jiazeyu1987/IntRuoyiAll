# 一线 PQC 需求口径对齐

## Task Goal

按用户确认的 PQC 填写流程对齐当前实现：检验项 Tab 显示检验项名称；首检/巡检按 QA 项目和工序匹配后计算检验数量；选择工序后允许按正式任务可用类型切换首检/巡检；PQC 组长管理列表只展示 PQC 提交记录。

## Milestones

- [x] 记录 BDD 场景、当前偏差和 RED 证据。
- [x] 修复一线 PQC 检验项 Tab 名称、首检/巡检选择和数量展示逻辑。
- [x] 修复后端巡检抽样率数量计算和 PQC 组长管理列表事件类型过滤。
- [x] 运行目标前后端回归、静态检查和 diff 检查。
- [x] 更新验证报告并完成任务收尾记录。

## Expected Verification

- `node tests/e2e/pqc-requirement-alignment-static.spec.cjs`
- `node tests/e2e/pqc-tab-item-name-display-static.spec.cjs`
- `node tests/e2e/pqc-tab-method-display-static.spec.cjs`
- `node tests/e2e/pqc-active-title-method-display-static.spec.cjs`
- `pnpm ts:check`
- `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldAddWorkOrderWithoutScheduleFromActiveRouteSnapshot,MesTeamLeaderActiveOrderServiceTest#shouldKeepScheduledPqcBusinessDateFromProcessPlanDateWhenErpPlannedStartMissing,MesTeamLeaderActiveOrderServiceTest#shouldGenerateFormalPqcTasksFromPublishedRegulationWhenAddingActiveOrder,MesTeamLeaderActiveOrderServiceTest#shouldResolvePatrolInspectionQuantityFromQaPercentageRatio,MesTeamLeaderActiveOrderErpPlannedStartTest#shouldUseJoinedDateForUnscheduledPqcTasksWhenErpPlannedStartMissing,MesFrontlinePqcContextServiceTest#shouldExposeFirstAndPatrolTaskOptionsForSameProcess,MesTeamLeaderWorkbenchServiceImplTest#shouldRestrictPqcLeaderManagementPageToPqcInspectionEvents" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/pqc-requirement-alignment-static.spec.cjs IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderWorkbenchServiceImpl.java IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceTest.java IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderWorkbenchServiceImplTest.java doc/tasks/20260808-frontline-pqc-requirement-alignment`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按正式 QA/PQC 任务、工序和 PQC 事件类型链路修正。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- 用户可见描述与内部编码隔离门禁：检验项 Tab 可见名称必须使用正式 `itemName`，编码只作为 key/提交身份。
- 前端提交前严格验证与草稿态计算隔离门禁：数量展示可随选择计算，但正式提交仍保留提交前严格校验。
- MES PQC 项目级检验快照门禁：PQC 项目、方法和数量来自正式 QA/PQC 项目快照，不使用 `formBindings` 或前端文案替代。
- MES PQC组长人员范围与管理数据可见性门禁：PQC 管理列表必须同时满足人员范围和 PQC 事件类型。
