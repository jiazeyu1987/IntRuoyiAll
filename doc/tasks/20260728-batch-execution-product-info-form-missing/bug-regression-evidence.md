# Bug Regression Evidence

## Bug

批次执行详情中已有正式工序批记录任务时，如果该批记录版本包含固定“产品信息”成员表单但历史批次任务中缺少该成员，读取批次详情不会补齐“产品信息”，导致批记录表单区域缺失该表单。

## Expected

当正式 `MAIN + BATCH_RECORD` 批记录任务所属 `batchRecordDefinitionId + batchRecordVersionId` 存在唯一“产品信息”成员报表时，打开批次执行详情必须展示并持久化该“产品信息”任务；该任务必须固定 `batchRecordSort=80`，在正式批记录表单之后填写，且不得从 `formBindings`、工序开始配置或默认 `MAIN` 推断。

## Reproduction

`mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Root Cause

`ensureRouteFormTasksPresent` 在发现批次已存在任一 `ROUTE_FORM` 任务后直接返回，只能恢复“完全缺失工序批记录任务”的历史批次，不能恢复“已有工序表单但缺同版产品信息成员表单”的部分缺失状态。相邻创建路径曾将产品信息成员表单排在源表单之前；用户确认产品信息表应统一固定在 `80`，等正式批记录表单填完后再填。

## RED:

目标测试先失败：期望批次详情第一工序任务为 `[RPT-DETAIL-PRODUCT-INFO-MEMBER, RPT-DETAIL-PRODUCT-INFO-PROCESS]`，实际仅 `[RPT-DETAIL-PRODUCT-INFO-PROCESS]`。

## GREEN:

修复后目标测试通过：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。

## Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，断言产品信息固定 `batchRecordSort=80` 且在前一张批记录未完成前不可填写。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_includesProductInfoMemberFromSameBatchRecordVersion+getDetail_shouldRecoverMissingRouteProcessTasksBeforeRendering+getPage_shouldRecoverMissingRouteProcessTasksBeforeRendering+getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。

## Blockers

当前分支仍存在 `origin/int_main` 同步阻塞，且相邻 Maven 回归被既有无关测试编译错误阻塞：`MesProEdhrProcessFormPermissionRuleServiceImplTest` 引用缺失的 `FillAssignment#getCandidateSourceNames()`。需要按项目提交/推送门禁完成后才能将任务标记为 `completed`。
