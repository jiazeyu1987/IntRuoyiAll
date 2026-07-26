# Execution Log

## User Intent

用户确认当前业务规则：一个工序可能有多个人可以填写，但系统当前没有负责人概念。默认由过程检验记录的填写人完成工序并进入下一步；如果当前工序没有过程检验记录填写人，例如灭菌记录工序，则由该工序解析出的所有填写人都可以进行下一步。要求按此规则优化当前流程，并完成完整数据真实 E2E 验证。

## BDD

- `BDD: 多填写人表单待办可见 -> Given` 当前工序有多张表单且每张表单配置不同填写人集合，`When` 任一填写人打开个人工作台，`Then` 系统应展示该用户可填写的表单任务，并通过正式入口进入对应普通批记录或 FormCenter 动态表单。
- `BDD: 过程检验记录填写人优先推进 -> Given` 当前工序存在过程检验记录且配置了填写人集合，`When` 当前工序必填表单已满足完成条件，`Then` 只有过程检验记录填写人集合内用户可以完成工序并推进下一步。
- `BDD: 无过程检验记录时所有解析填写人可推进 -> Given` 当前工序没有过程检验记录填写人集合，`When` 当前工序必填表单已满足完成条件，`Then` 当前工序解析出的全部表单填写人并集内用户可以完成工序并推进下一步。
- `BDD: 非填写人或非推进人 fail-fast -> Given` 当前用户不属于表单填写人或工序推进人集合，`When` 尝试打开填写、保存提交或推进工序，`Then` 后端返回明确权限错误，前端展示错误，不静默降级。
- `BDD: 动态表单工作台入口统一 -> Given` 个人工作台待办对应 FormCenter 动态表单且无传统 `executionId`，`When` 用户点击进入处理，`Then` 前端应调用正式批次任务打开接口并打开动态表单抽屉或统一填写工作区，不再强制要求 `executionId`。

## Milestone Updates

- completed: M1 审计确认当前流程没有工序负责人概念；个人工作台、任务打开、FormCenter 提交和下一工序创建均应以工作任务 assignee/candidate 快照与当前工序表单填写人集合为准。
- completed: M2 记录多填写人待办可见、过程检验填写人优先推进、无过程检验时全部解析填写人可推进、非填写人 fail-fast、动态表单工作台入口统一等 BDD。
- completed: M3 后端已实现 candidateUserSnapshot 工作台可见、提交人 assignee/candidate 校验、过程检验填写人优先推进、无过程检验时工序所有填写人可推进、非推进人 fail-fast。
- completed: M4 前端已统一个人工作台 FormCenter 动态表单入口，允许无传统 executionId 的任务从正式 openTask 响应进入批次详情并自动打开表单抽屉。
- completed: M5 后端目标测试、前端静态合同和完整真实数据 E2E 均 PASS。
- ready_for_closeout: 已清理 `EDHR-ADV-%` 任务自有调试数据，剩余工作为证据校验、经验沉淀和 closeout 清理。

## TDD Evidence

- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrWorkTaskServiceImplTest#..." "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，旧逻辑仅按 `assigneeUserId` 展示工作台任务，且默认推进人规则不能表达过程检验优先。
- RED: `node tests\e2e\edhr-work-task-formcenter-navigation-static.spec.js` -> FAIL，旧前端入口对 FormCenter 工作任务仍强依赖传统 `executionId`。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrWorkTaskServiceImplTest#getMyPage_includesCandidateFillTaskForNonAssignee+completeFillAndCreateNextFill_doesNotAdvanceWhenInspectionFillerExistsAndActorIsOnlyMainFiller+completeRouteFormFillAndCreateNextFill_advancesWhenActorIsInspectionFiller+completeRouteFormFillAndCreateNextFill_allowsAnyProcessFillerWhenNoInspectionFiller+completeRouteFormFillAndCreateNextFill_rejectsActorOutsideProcessFillerSnapshot" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，5 tests, 0 failures。
- GREEN: `node tests\e2e\edhr-work-task-formcenter-navigation-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-work-task-notify-workbench-fill-navigation-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-work-task-board-unified-navigation-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-work-task-process-advance-real.e2e.js` -> PASS，runKey `EDHR-ADV-6T182008199Z`。

## Verification Evidence

- Verification: 完整真实 E2E 使用本机 `http://localhost:8081` 与 `http://127.0.0.1:48081`，测试租户 `测试租户`，用户 `aoteman` 与 `admin`，三组任务自有批次夹具均走真实个人工作台“处理”按钮和 FormCenter 提交按钮。
- Verification: `noInspection` 场景中 `aoteman` 不是 assignee 但在 candidate 快照内，可看到并填写任务，提交后下一工序 fill count = 1。
- Verification: `mainBlockedByInspection` 场景中主表填写人 `aoteman` 可完成主表，但因为当前工序存在过程检验填写人集合，不创建下一工序 fill count = 0。
- Verification: `inspectionAdvances` 场景中过程检验填写人 `admin` 完成过程检验后创建下一工序 fill count = 1。
- Verification: 数据库清理 SQL 将 `EDHR-ADV-%` 任务自有 `batch_execution/work_task/work_order/form_instance` 活跃残留从 `6/10/0/6` 清理为 `0/0/0/0`。
- Verification: `git diff --check` -> PASS。

## Blockers

- GREEN: evidence validators -> PASS，backend API、frontend feature、bug regression、QA 四个证据脚本均有效。
- GREEN: project-experience-consolidation -> PASS，新增 `docs/e2e-rules.md#edhr-工作任务-formcenter-动态表单夹具门禁`，并在 `docs/experience-index.md` 增加关键词路由。
- GREEN: task-closeout-cleanup preview -> PASS，keep 仅包含正式任务记录和证据文件，delete/blocked/warnings 均为 none。
- GREEN: task-closeout-cleanup apply -> PASS，linked worktree=false，delete/blocked/warnings 均为 none。
- GREEN: final cleanup -> PASS，`runtime-patch-20260727014422` 临时目录和空 `task-implementation.patch` 已清理。
- GREEN: final git status -> PASS，`int_main...origin/int_main` 无未提交差异。
- Blockers: none。
