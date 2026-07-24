# 任务：MES 重排 latest-start 工单仍需生成活动任务

## 任务目标

- 修复手动重排 `replanApply` 在多工单范围内“部分工单生成新任务、部分工单因 `LATEST_START` 被整单跳过而零活动任务”的问题。
- 保持重排预览现有语义：`LATEST_START` 仍作为风险 warning 展示，但在重排场景不再阻止该工单生成活动任务。
- 保持普通自动排产既有合同：非重排场景下，纯 `LATEST_START` 且零有效任务时仍按原规则阻断发布。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个相关后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-replan-latest-start-active-task-regression\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务仅修复“预览阶段 latest-start 暂缓误报 ACTIVE_TASK”。本次根据用户最新实测，继续修复“应用重排后 latest-start 工单被整单跳过，导致只有一个工单有活跃任务”的根因。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：无
- 适用强制门禁：
  - 本轮只做本机后端代码、只读运行态排查与定向单元测试，不执行真实 E2E、服务器写入或发布动作。
  - 如后续进入真实登录写入、远端联调或长链路验证，必须先在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS` 或明确阻塞原因。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。不会通过前端候选兜底或静默放过零任务工单来掩盖重排缺陷。
- `是否从根因和长期维护角度解决`：是。直接修正 `scheduleTasks(...)` 对重排 latest-start 工单的调度策略，让重排结果与后续报工链路保持一致。
- `是否存在临时补丁或绕过`：否。不会改反馈归属候选查询去掩盖重排阶段遗留的零活动任务结果。

## BDD 场景

- `BDD: 混合重排中命中 latest-start 风险的工单仍应生成活动任务 -> Given 选中范围内两张工单都存在剩余报工量，其中一张计划开工晚于最晚开工时间 / When 用户执行重排预览并应用 / Then 系统仍为两张工单生成活动任务，并仅把 latest-start 工单标记为 LATEST_START warning。`
- `BDD: latest-start 风险在重排预览中继续只作为 warning 展示 -> Given 某工单计划开工晚于最晚开工时间 / When 用户执行重排预览 / Then 预览继续展示 LATEST_START warning，不额外补报 ACTIVE_TASK blocking。`
- `BDD: 普通自动排产的 latest-start 零任务阻断合同保持不变 -> Given 非重排自动排产范围内某工单仅因 latest-start 风险导致没有任何有效任务 / When 用户尝试应用自动排产 / Then 系统仍以 PRO_AUTO_SCHEDULE_LATEST_START_ZERO_TASK_BLOCKED 阻断发布。`
- `BDD: 受保护旧任务 + 剩余量续排场景不回退 -> Given 某工序存在受保护旧任务且 remainingQuantity 仍大于 0 / When 用户执行重排预览并应用 / Then 系统仍保留旧任务并为剩余量生成新的活动任务，不被本次发布门禁误伤。`

## 里程碑

1. M1：创建任务包并记录真实运行态根因。`COMPLETED`
2. M2：补 RED 回归测试，复现“重排 latest-start 工单被整单跳过，只生成一个工单活动任务”。`COMPLETED`
3. M3：最小修复重排 latest-start 调度逻辑。`COMPLETED`
4. M4：运行 GREEN 定向回归并回写证据。`COMPLETED`

## 预期验证

- `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#replanApply_shouldKeepLatestStartRiskOrderAsActiveTaskDuringReplan -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldKeepLatestStartRiskOrderSchedulableDuringReplan+replanApply_shouldKeepLatestStartRiskOrderAsActiveTaskDuringReplan+replanApply_shouldKeepFinishedTaskAndCreateNewActiveTaskForRemainingQuantity,MesProAutoScheduleAlgorithmContractTest#previewAndApply_shouldHoldScheduleOrderWhenPlanStartsAfterLatestStart -Dsurefire.failIfNoSpecifiedTests=false test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-replan-apply-partial-active-task-block\bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-replan-apply-partial-active-task-block\backend-api-evidence.md`

## 最终验证结果

- `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#replanApply_shouldKeepLatestStartRiskOrderAsActiveTaskDuringReplan -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `mvn --% -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldKeepLatestStartRiskOrderSchedulableDuringReplan+replanApply_shouldKeepLatestStartRiskOrderAsActiveTaskDuringReplan+replanApply_shouldKeepFinishedTaskAndCreateNewActiveTaskForRemainingQuantity,MesProAutoScheduleAlgorithmContractTest#previewAndApply_shouldHoldScheduleOrderWhenPlanStartsAfterLatestStart" -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-replan-apply-partial-active-task-block\bug-regression-evidence.md` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-replan-apply-partial-active-task-block\backend-api-evidence.md` -> PASS

## 阻塞与影响

- 当前无外部阻塞。
- 已确认真实运行态最近一次 `REPLAN_APPLY` 实际范围为 `scheduleOrderIds=[13,47]`；其中 `47/903200` 命中 `LATEST_START` 后被整单跳过、未生成任何任务，导致归属页只剩 `13/903245` 的真实工单候选。
- 已完成修复：重排场景下命中 `LATEST_START` 的工单仍会生成活动任务；普通自动排产 latest-start 零任务阻断合同保留。
