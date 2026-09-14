# Verification Report

## Bug

一线 PQC 选择订单 `CODX-AO5-20260807-05` 的 `3. 清洗工序` 时，页面提示 `当前工序缺少发布态QA检验项目`。用户确认部分 QA 检验项目可无设备，且认为按压式压力泵清洗工序应存在 QA 项目。

## Expected

PQC 工序选择必须返回当前生产工单产品正式路线的全部工序；只有同一当前产品、路线、路线版本、路线工序和工序存在正式 `PENDING` PQC 任务时，才附着 `pqcTaskId`、发布规程和 `inspectionItems`。无设备 QA 项目在 `equipment_required=false` 且未选择设备时应允许提交并保存空设备快照。

## Reproduction

只读核对本地库 `ruoyi-vue-pro`：订单 `CODX-AO5-20260807-05` 对应产品 `924008 / IDI / 按压式球囊扩充压力泵`，活跃订单 `39`，路线 `980091`，路线版本 `622`；该路线第 3 道为 `980633 / 922987 / 清洗工序`。当前 `PENDING` PQC 任务 `211-214` 全部绑定 `980631 / 922985 / 粗洗工序`，当前产品/路线/版本发布态 QA 规程也只有粗洗规程 `36`。

## Root Cause

截图工序 `980633 / 922987 / 清洗工序` 在当前订单的精确产品/路线/路线版本下没有发布态 QA 规程，也没有 `PENDING` PQC 任务；系统里存在的 `清洗工序` QA 规程属于另一产品 `902149 / 球囊扩张压力泵`、路线 `922119`，不能作为当前订单的 QA 项目来源。代码侧同时修复了工序选择不应被活跃订单快照或单一 PQC 任务裁剪的问题。

## RED

RED: 当前接手状态已包含代码修复，未保留可重复的 pre-change 失败命令。缺陷复现证据来自截图和只读数据库链路；新增回归测试锁定旧风险：路线有多道工序但仅一道有 `PENDING` PQC 任务时，`active-order/processes` 仍必须返回全路线工序，并只给待检工序附着任务上下文。

## GREEN

GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldDisplayProductLineRouteProcessesEvenWhenOnlyOneProcessHasPqcTask" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS, 1 test.

GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS, 32 tests.

GREEN: `git diff --check -- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceTest.java doc/tasks/20260808-pqc-cleaning-qa-items-missing` -> PASS except Git LF-to-CRLF normalization warnings.

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260808-pqc-cleaning-qa-items-missing\verification-report.md` -> PASS.

GREEN: `task-closeout-cleanup --mode preview` and `--mode apply` -> PASS; deleted only the 5 current-task temporary artifacts listed in `task.md`.

## Verification

- `MesFrontlinePqcContextServiceTest#shouldDisplayProductLineRouteProcessesEvenWhenOnlyOneProcessHasPqcTask` 覆盖：正式产品路线有多道工序时，PQC 工序选择返回全路线工序；无待检任务工序可展示但没有 `pqcTaskId` / `inspectionItems`。
- `MesFrontlinePqcContextServiceTest#shouldSubmitPqcInspectionWithoutEquipmentForOptionalQaItems` 覆盖：`equipment_required=false` 的 QA 项目未选择设备时可提交，逐件明细保存空设备快照。
- 只读 SQL 证明没有把其它产品/路线的清洗 QA 项目借给当前订单；没有执行任何数据库写入。
- Project experience consolidation checked existing `docs/backend-development.md#MES PQC 项目级检验快照门禁`; no new long-term experience file was needed.

## Blockers

无代码阻塞。若业务上确实要求当前订单的 `980633 / 922987 / 清洗工序` 可填写 QA 项目，需要在 QA 配置/发布流程中为产品 `924008`、路线 `980091`、路线版本 `622`、路线工序 `980633`、工序 `922987` 发布正式 QA 规程，并生成对应 `PENDING` PQC 任务；本次未做数据库修复或跨产品复制。
