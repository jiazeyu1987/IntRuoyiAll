# 任务：重排预览 latest-start 暂缓误报 ACTIVE_TASK 修复

## 任务目标

- 修复自动重排预览在“某排产工单仅因计划开工晚于最晚开工时间而暂缓生成任务”时，被后置 `ACTIVE_TASK` 覆盖校验误升级为“剩余报工量没有活动任务承接”的问题。
- 保持既有 fail-fast 合同：单工单纯 `LATEST_START` 且无任何有效任务时，发布阶段仍应由 `PRO_AUTO_SCHEDULE_LATEST_START_ZERO_TASK_BLOCKED` 阻断，不得静默放行。
- 保持既有“存在受保护旧任务且 remainingQuantity > 0 时仍可续排”的修复行为，不回退已完成回归。

## 当前状态

COMPLETED

## 上一任务检查

- 上一相关任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-mes-feedback-attribution-inline-edit\task.md`
- 当前状态：`已完成`
- 处理说明：已修复“受保护旧任务 + 剩余报工量续排”场景；本次继续补“LATEST_START 暂缓时误报 ACTIVE_TASK”分支。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 适用强制门禁：
  - 本轮只做本机后端代码、单测与本地接口重放诊断，不做服务器写入与真实浏览器 E2E。
  - 若后续进入真实登录写入、远端联调或发布动作，必须先在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。仅收敛误报分支，不新增兜底路径。
- `是否从根因和长期维护角度解决`：是。通过让 `ACTIVE_TASK` 覆盖校验识别 `LATEST_START` 暂缓工单，避免同一工单被 warning 结果再误升级成无关阻断。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 多工单重排中 latest-start 暂缓工单不应误报 ACTIVE_TASK -> Given 重排范围内一张工单能正常生成任务，另一张工单因计划开工晚于最晚开工时间而只保留分析结果、不生成 preview 任务 / When 用户执行重排预览 / Then 暂缓工单只暴露 LATEST_START warning，不得再为其每道剩余工序补报 ACTIVE_TASK blocking。`
- `BDD: latest-start 单工单空结果发布仍需 fail-fast -> Given 单张工单因计划开工晚于最晚开工时间且预览中没有任何有效任务 / When 用户尝试发布自动排产或重排 / Then 系统仍以 PRO_AUTO_SCHEDULE_LATEST_START_ZERO_TASK_BLOCKED 阻断发布，不得因本次修复而放行。`

## 里程碑

1. M1：补任务文档、定位真实运行态根因与 RED 场景。
2. M2：补 latest-start + ACTIVE_TASK 误拦回归测试。
3. M3：最小修复后端覆盖校验逻辑。
4. M4：运行定向 GREEN 与相关回归。
5. M5：回写执行证据、缺陷证据与最终结论。

## 预期验证

- `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldNotAddActiveTaskBlockingForLatestStartDeferredWorkOrder -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest,MesProAutoScheduleAlgorithmContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-replan-latest-start-active-task-regression\bug-regression-evidence.md`

## 最终验证结果

- `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldNotAddActiveTaskBlockingForLatestStartDeferredWorkOrder -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleAlgorithmContractTest#previewAndApply_shouldHoldScheduleOrderWhenPlanStartsAfterLatestStart -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldKeepFinishedTaskAndGenerateNewActiveTaskForRemainingQuantity+replanApply_shouldKeepFinishedTaskAndCreateNewActiveTaskForRemainingQuantity -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- `mvn --% -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest,MesProAutoScheduleAlgorithmContractTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，43 个定向/合同回归全部通过。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-replan-latest-start-active-task-regression\bug-regression-evidence.md` -> PASS。

## 阻塞与影响

- 当前无外部阻塞；真实根因已通过本地接口重放确认并完成代码修复与回归验证。
