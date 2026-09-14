# Execution Log

## User Intent

用户确认：一线 PQC 的工序卡片显示的不是工艺路线里的工序；应显示 QA 中对应产品的检验项目列表里“工序”列的不重复数据，产品来自当前选择的生产工单。

## BDD Scenarios

- BDD: PQC 工序卡片来自 QA 项目工序列 -> Given 操作员选择一个生产工单且该工单产品已有发布态 QA 检验规程 / When 一线 PQC 展示工序卡片 / Then 卡片集合等于该产品 QA 检验项目列表“工序”列的去重集合
- BDD: 不显示路线无 QA 项目的工序 -> Given 产品路线存在多个工艺路线工序但 QA 检验项目只配置其中部分工序 / When 一线 PQC 展示工序卡片 / Then 未出现在 QA 检验项目“工序”列中的路线工序不显示
- BDD: 同一 QA 工序去重显示 -> Given QA 检验项目列表多条项目引用同一个工序 / When 一线 PQC 展示工序卡片 / Then 该工序只显示一张工序卡片，并聚合该工序下正式 PQC 任务选项

## Milestone Updates

- in_progress: 已建立任务目录和 BDD 场景，准备读取经验索引并定位前后端候选来源。
- completed: 已定位旧实现 `MesFrontlinePqcContextServiceImpl.listProcessesByActiveOrder` 从 active order 路线工序快照生成候选，和用户确认的 QA 检验项目工序列来源冲突。
- completed: 已新增 `MesQaInspectionRegulationMapper.selectPublishedListByProductRouteVersion`，按生产工单产品、路线、路线版本读取发布态 QA 规程工序集合。
- completed: 已将一线 PQC 工序候选改为发布态 QA 检验规程 `routeProcessId/processId` 去重集合；路线中存在但 QA 项目未配置的工序不再生成卡片。
- completed: 已保留待检任务聚合逻辑，存在 `PENDING` PQC 任务时附着任务选项、检验项目和提交候选；待检任务不属于 QA 工序集合时 fail fast。
- completed: 已更新 `docs/backend-development.md#mes-pqc-项目级检验快照门禁` 与 `docs/experience-index.md`，移除旧“全路线工序”经验口径。
- completed: 已运行 task-closeout-cleanup preview/apply；保留 `task.md`、`execution-log.md`、`verification-report.md`，删除本任务临时 evidence 文件。

## Verification Evidence

- INDEPENDENT VERIFY: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldDisplayOnlyQaInspectionItemProcessesWhenRouteHasExtraProcesses+shouldAttachPqcTaskFromQaRegulationProcessWhenCurrentRouteProcessIdsDrift" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Surefire 执行 2 个目标用例，验证路线额外工序不显示、QA 规程 routeProcess/process 身份漂移仍按 QA 工序源附着任务。
- INDEPENDENT VERIFY: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldLoadProcessesFromSelectedActiveOrderProductRoute+shouldExposeFirstAndPatrolTaskOptionsForSameProcess+shouldHideRouteProcessesWithoutQaInspectionItemsAndAttachPqcTaskToPendingProcess+shouldPreparePqcPieceDetailContextWithBulkQueriesOnly+shouldDisplaySubmittedPqcTaskProcessWithoutTaskContextAndKeepPendingProcess+shouldFailFastWhenPendingPqcTaskMissingFormalProcessIdentity" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Surefire 执行 6 个相邻回归用例，覆盖候选列表、首检/巡检任务选项、待检任务挂载、已提交任务上下文保留和缺正式身份 fail-fast。
- INDEPENDENT VERIFY: `git diff --check -- <task-owned backend/docs files>` -> PASS，退出码 0；仅有既有 LF/CRLF warning，无 whitespace error。
- INDEPENDENT VERIFY: 源码核对 -> PASS，`MesFrontlinePqcContextServiceImpl.listProcessesByActiveOrder` 通过 `MesQaInspectionRegulationMapper.selectPublishedListByProductRouteVersion` 读取发布态 QA 规程项目工序集合，路线工序表不再作为 PQC 工序卡片候选源。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest" test` -> FAIL，类级验证在实现前后均暴露既有 PQC 提交流程脏基线失败；第一次单方法命令因 Maven/Surefire 目标选择返回 `Tests run: 0`，未作为有效 RED 证据使用。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest#shouldDisplayOnlyQaInspectionItemProcessesWhenRouteHasExtraProcesses" test` -> PASS，路线额外工序不再显示。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest#shouldDisplayOnlyQaInspectionItemProcessesWhenRouteHasExtraProcesses+shouldAttachPqcTaskFromQaRegulationProcessWhenCurrentRouteProcessIdsDrift" test` -> PASS，QA 工序源和 ID 漂移场景通过。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest#shouldLoadProcessesFromSelectedActiveOrderProductRoute+shouldExposeFirstAndPatrolTaskOptionsForSameProcess+shouldHideRouteProcessesWithoutQaInspectionItemsAndAttachPqcTaskToPendingProcess+shouldPreparePqcPieceDetailContextWithBulkQueriesOnly+shouldDisplaySubmittedPqcTaskProcessWithoutTaskContextAndKeepPendingProcess+shouldFailFastWhenPendingPqcTaskMissingFormalProcessIdentity" test` -> PASS，候选列表、任务选项、提交候选和异常身份相邻回归通过。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260808-frontline-pqc-process-cards-qa-items/backend-api-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260808-frontline-pqc-process-cards-qa-items/bug-regression-evidence.md` -> PASS。
- GREEN: `git diff --check -- <task-owned files>` -> PASS，仅有 CRLF 提示，无 whitespace error。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-frontline-pqc-process-cards-qa-items --mode preview` -> PASS，blocked/warnings 均为 none。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-frontline-pqc-process-cards-qa-items --mode apply` -> PASS，仅删除本任务临时 evidence 文件。

## Blockers

- 非本次阻塞：`mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest" test` 仍因既有 PQC 提交流程脏基线失败（如 submit command 缺字段、生产提交事件设备上下文、Mockito unnecessary stubbing）而失败；本次工序候选源相关目标用例已单独通过。
