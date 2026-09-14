# Bug Regression Evidence

## Bug

批次执行详情中已有正式工序批记录任务时，如果该批记录版本包含固定“产品信息”成员表单但历史批次任务中缺少该成员，读取批次详情不会补齐“产品信息”，导致批记录表单区域缺失该表单。

用户后续截图继续暴露 UI 归属问题：后端已补齐产品信息并固定 `batchRecordSort=80` 后，详情页仍把“产品信息”作为第 1 工序下的一张表单显示，而不是左侧独立的第 80 工序。

## Expected

当正式 `MAIN + BATCH_RECORD` 批记录任务所属 `batchRecordDefinitionId + batchRecordVersionId` 存在唯一“产品信息”成员报表时，打开批次执行详情必须展示并持久化该“产品信息”任务；该任务必须固定 `batchRecordSort=80`，在正式批记录表单之后填写，且不得从 `formBindings`、工序开始配置或默认 `MAIN` 推断。

详情页左侧工序列表也必须把“产品信息”展示为独立虚拟 `80 产品信息` 工序；即使后端为了来源追溯保留第 1 工序的 `routeProcessId/routeProcessSort`，也不能把产品信息合并进第 1 工序右侧表单列表。

## Reproduction

`mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists" "-Dsurefire.failIfNoSpecifiedTests=false" test`

`node tests\e2e\edhr-batch-product-info-virtual-process-static.spec.js`

## Root Cause

`ensureRouteFormTasksPresent` 在发现批次已存在任一 `ROUTE_FORM` 任务后直接返回，只能恢复“完全缺失工序批记录任务”的历史批次，不能恢复“已有工序表单但缺同版产品信息成员表单”的部分缺失状态。相邻创建路径曾将产品信息成员表单排在源表单之前；用户确认产品信息表应统一固定在 `80`，等正式批记录表单填完后再填。

前端 `BatchExecutionDetailPage.vue` 的 `processTaskGroups` 原来按 `routeProcessId || routeProcessSort || id` 分组。产品信息任务来自第 1 工序正式批记录绑定，后端保留来源 `routeProcessId=第 1 工序`，导致 UI 分组把它合并进第 1 工序，未按产品信息固定排序 `80` 创建独立工序。

## RED:

目标测试先失败：期望批次详情第一工序任务为 `[RPT-DETAIL-PRODUCT-INFO-MEMBER, RPT-DETAIL-PRODUCT-INFO-PROCESS]`，实际仅 `[RPT-DETAIL-PRODUCT-INFO-PROCESS]`。

前端静态合同先失败：详情页缺少 `isProductInfoProcessTask` 和产品信息专用分组 key，不能证明“产品信息”会独立显示为 80 工序。

## GREEN:

修复后目标测试通过：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。

前端修复后静态合同通过，并通过真实页面 E2E：工单 `881MO090889` 的批次详情左侧存在独立 `80 产品信息`；第 1 工序右侧卡片不再包含产品信息；点击 `80 产品信息` 后右侧仅显示“产品信息”。

## Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，断言产品信息固定 `batchRecordSort=80` 且在前一张批记录未完成前不可填写。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_includesProductInfoMemberFromSameBatchRecordVersion+getDetail_shouldRecoverMissingRouteProcessTasksBeforeRendering+getPage_shouldRecoverMissingRouteProcessTasksBeforeRendering+getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- `node tests\e2e\edhr-batch-product-info-virtual-process-static.spec.js` -> PASS。
- 真实 Playwright E2E -> PASS，`芋道源码/admin`，`http://127.0.0.1:8081`，批次 `EDHRB-1785224948633` / id `900000000900`，截图 `output/playwright/20260728-product-info-virtual-process-80-e2e.png`。

## Blockers

当前分支仍存在 `origin/int_main` 同步阻塞，且工作区存在非本任务并行未提交改动。本轮目标回归与相邻 4 方法回归已通过；需要按项目提交/推送门禁完成后才能将任务标记为 `completed`。
