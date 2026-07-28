# Backend API Evidence

## Scope

服务范围：`MesProEdhrWorkTaskServiceImpl#createInitialFillTask` 创建 eDHR 初始填写工作任务时的 `EDHR_PROCESS_FORM_FILLER` 责任范围快照生成。

## API Contract And Data Contract

外部接口形态不变。内部数据契约调整为：普通批记录填写人规则 `scopeKey=ALL` 且 `fillableScopeJson` 为空时，必须基于 `MesProEdhrBatchExecutionTaskDO.batchRecordDefinitionId`、`batchRecordVersionId` 查询 `mes_pro_batch_record_report` 正式报表成员，并生成 `responsibilityScopeJson.scopes[].fillableScope.ranges`。

## Auth, Permissions, Validation, And Error Behavior

候选填写人仍由 `MesProEdhrCandidateResolver#resolveProcessFormRule` 解析，候选池为空、用户无效、角色/部门组为空等错误行为不变。责任范围缺少定义、版本、报表成员或 `sourceTableIndex` 时继续抛出 `PRO_EDHR_WORK_TASK_RESPONSIBILITY_SCOPE_INVALID`，不返回默认成功。

## Required Config, Services, Fixtures, And Migrations

无需新增配置、服务或数据库迁移。测试 fixture 使用现有 H2/MyBatis 测试环境，新增正式批记录报表成员数据由 `MesProBatchRecordReportMapper` 插入。

## BDD Scenarios

- BDD: 普通整表填写人规则生成责任范围快照 -> Given 批次工序任务绑定正式批记录表单且填写人规则为 `scopeKey=ALL`、未显式保存单元格范围 When 创建初始填写工作任务 Then 系统应从正式批记录报表生成整表可填写范围快照并创建任务。
- BDD: 责任范围缺少正式来源仍失败 -> Given 任务无法解析批记录报表布局或动态表单范围 When 创建工作任务 Then 系统应 fail fast 并暴露责任范围快照无效。

## RED:

- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_buildsAllScopeSnapshotFromReportMembersWhenRuleScopeIsBlank" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `ServiceException: eDHR 工作任务责任范围快照无效：scopeKey=ALL`

## GREEN:

- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_buildsAllScopeSnapshotFromReportMembersWhenRuleScopeIsBlank" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test
- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 71 tests, 0 failures, 0 errors

## Contract Or Integration Verification

`MesProEdhrWorkTaskServiceImplTest` 覆盖任务创建、候选人快照、责任范围 JSON 中多表 `ranges`、缺少正式报表成员 fail-fast，以及相邻工序任务规则。

## Observability Touchpoints

未新增日志或指标。错误继续通过既有 `ServiceException` 错误码 `PRO_EDHR_WORK_TASK_RESPONSIBILITY_SCOPE_INVALID` 暴露。

## Blockers And Downstream Skills

当前分支存在非本任务本地提交领先 `origin/int_main`，最终 closeout/push 需先处理无关提交风险；不需要数据库 schema、前端或 E2E 下游技能。
