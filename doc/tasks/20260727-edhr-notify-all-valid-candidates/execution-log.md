# 执行日志

## 用户意图

用户明确要求：站内信应发给所有有效候选人，并按该口径进行设计、开发和验证。

## 初始状态

- 工作目录：`E:\IntRuoyi`
- 后端模块：`IntRuoyiBackend\yudao-module-mes`
- 分支：`int_main`
- 既有脏工作区已按项目门禁独立保存为基线提交：`868893b0`
- 基线包含既有发布脚本、前端改动、其他任务文档和静态 E2E 文件；不包含本任务文件。
- `origin`：`https://github.com/jiazeyu1987/IntRuoyiAll.git`

## BDD 场景

BDD: 填写任务通知全部有效候选人 -> Given 一个待办填写任务的候选快照包含多个有效账号，When 创建该工作任务，Then 每个有效候选账号各收到一条填写任务站内信。

BDD: 审核任务通知全部有效候选人 -> Given 一个待办审核任务的候选快照包含多个有效账号且当前任务有一个实际 assignee，When 创建该审核任务，Then 候选快照中的每个有效候选账号各收到一条审核任务站内信。

BDD: 同一任务候选账号去重 -> Given 一个任务候选快照重复包含同一账号，When 发送任务通知，Then 该账号只收到一条站内信。

BDD: 候选来源不混淆 -> Given 填写任务和审核任务拥有不同候选快照，When 分别创建任务，Then 每个任务只按自己的候选快照通知，不把两个任务的候选人合并。

## 里程碑 1：现状与影响范围确认

### 命令意图

- 定位批次创建、工作任务创建和站内信发送入口。
- 核对工艺路线表单槽位、批记录表单和候选快照的现有优先级。
- 核对 Git 状态并保存既有脏改动基线。

### 结果

- 批次创建最终调用 `createInitialFillTask`。
- 工作任务统一在 `MesProEdhrWorkTaskServiceImpl#createTask` 中调用 `sendNotify`。
- 原实现只把 `task.assigneeUserId` 设置为通知收件人。
- 任务已创建，既有脏改动已独立提交为 `868893b0`。

## 里程碑 2：测试先行

状态：completed

RED: mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask+createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, 3 个用例均因 `sendNotify` 仍只按单一 `assigneeUserId` 调用 `sendSingleMessageToAdmin` 失败：填写任务期望 2 次实际 1 次；审核多候选任务期望 3 次实际 2 次；重复候选去重场景期望 2 次实际 1 次。

## 里程碑 3：后端实现

状态：completed

### 完成内容

- `MesProEdhrWorkTaskServiceImpl#sendNotify` 改为从 `candidateUserSnapshot` 解析候选账号集合。
- 复用既有 `parseCandidateUserIds` / `MesProEdhrWorkTaskAuthorization.parseRequiredCandidateSnapshotUserIds` 逻辑，保持任务内去重和候选快照缺失时 fail-fast。
- 每个候选账号单独调用既有 `notifyMessageSendApi.sendSingleMessageToAdmin`，模板编码、模板参数和 `workTaskId/actionUrl` 保持不变。

## 里程碑 4：验证

状态：blocked

GREEN: mvn -pl yudao-module-mes org.apache.maven.plugins:maven-surefire-plugin:3.5.3:test "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask+createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients" "-Dsurefire.failIfNoSpecifiedTests=true" -> PASS, 3 tests run, 0 failures, 0 errors。

REGRESSION: mvn -pl yudao-module-mes org.apache.maven.plugins:maven-surefire-plugin:3.5.3:test "-Dtest=MesProEdhrWorkTaskServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=true" -> PASS, 66 tests run, 0 failures, 0 errors。

COMPILE: mvn -pl yudao-module-mes -am "-DskipTests" compile -> PASS, MES 及依赖模块生产代码编译通过。

CHECK: git diff --check -- IntRuoyiBackend\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\MesProEdhrWorkTaskServiceImpl.java IntRuoyiBackend\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\batchrecord\MesProEdhrWorkTaskServiceImplTest.java doc\tasks\20260727-edhr-notify-all-valid-candidates -> PASS，仅输出 Git 行尾转换 warning，无 whitespace error。

BLOCKED: mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createReviewTasks_createsOneTodoPerSignatureCellAndCompletesSubmitTask+createReviewTasks_deduplicatesRepeatedFrozenCandidateNotifyRecipients" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL at `testCompile` before执行本任务测试，非本任务文件 `MesProEdhrBatchExecutionServiceTest` 引用 `WorkbenchReleaseSummary#getReleaseOwnerConfigured/getReleaseOwnerSourceType/getReleaseOwnerLabel`，而当前 `EdhrBatchWorkbenchRespVO.WorkbenchReleaseSummary` 不存在这些 getter。

## 里程碑 5：收尾

状态：blocked

## 阻塞项

- MES 标准 Maven test 生命周期被非本任务 releaseOwner 测试源码编译错误阻断，无法完成模块回归、证据脚本后的提交和推送。
- 本任务未修改 `MesProEdhrBatchExecutionServiceTest` 或 `EdhrBatchWorkbenchRespVO`，避免混入并发/无关任务范围。
