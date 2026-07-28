# Verification Report

## Scope

- 后端服务：`MesProEdhrBatchExecutionServiceImpl`。
- 后端测试：`MesProEdhrBatchExecutionServiceTest`。

## Results

- RED 已复现：新增详情回归测试在修复前失败，实际缺少“产品信息”成员表单。
- GREEN 已通过：修复后目标测试 PASS。
- 相邻回归已通过：新建批次产品信息成员补入、详情恢复、分页恢复和本次部分缺失恢复共 4 个方法 PASS。
- 本轮 80 排序补充验证已通过：产品信息成员任务固定 `batchRecordSort=80`，顺序位于正式工序批记录之后，且前序批记录未完成时门禁提示“前一张批记录未填写完成”。
- 真实页面 E2E 已通过：本机 `8081/48081` 下登录 `芋道源码/admin`，打开批次 `EDHRB-1785224948633` 详情，接口和页面均显示“产品信息”位于粗洗工序正式批记录表单之后，后端排序为 `80`，且前序批记录未完成时不可打开填写。
- `git diff --check` 已执行，退出码 0；仅 PowerShell 输出 CRLF 替换提示。

## Commands

- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_includesProductInfoMemberFromSameBatchRecordVersion+getDetail_shouldRecoverMissingRouteProcessTasksBeforeRendering+getPage_shouldRecoverMissingRouteProcessTasksBeforeRendering+getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `git diff --check`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS after 80 sorting update.
- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_includesProductInfoMemberFromSameBatchRecordVersion+getDetail_shouldRecoverMissingRouteProcessTasksBeforeRendering+getPage_shouldRecoverMissingRouteProcessTasksBeforeRendering+getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS after 80 sorting update.
- Real E2E inline Playwright script -> PASS. Frontend `http://127.0.0.1:8081`, backend `http://127.0.0.1:48081`, tenant/user label `芋道源码/admin`, page `/mes/pro/feedback/edhr-batch-execution/detail?id=900000000900`, screenshot `output/playwright/20260728-product-info-80-e2e.png`.

## Real E2E Evidence

- Runtime: frontend HTTP `200`; backend health `UP`; backend runtime process is the current `int_main` runtime jar under `E:\IntRuoyi\output\runtime\int_main`.
- Target batch: `EDHRB-1785224948633` / DB id `900000000900`.
- API assertion: 粗洗工序表单为 `[粗洗工序生产记录, 损耗单, 过程检验记录, 产品信息]`，排序为 `[1, 2, 3, 80]`。
- Product info assertion: task id `7028`, `batchRecordSort=80`, `formSlotType=MAIN`, `recordCategory=BATCH_RECORD`, `available=false`, `gateMessage=前一张批记录未填写完成`。
- Page assertion: 右侧表单卡片显示产品信息在粗洗工序生产记录之后，且产品信息卡片没有“打开填写”动作；浏览器 console error count `0`。
- Risk note: 只读 DB 扫描发现 `tenant_id=122` 仍有旧存量产品信息任务 `batch_record_sort=1`；如果验收口径包含历史存量批次统一修正，需要独立授权数据修复，本次未改库。

## Remaining

- cleanup 已完成，本任务实现提交 `842850cf` 已创建。
- 本轮 80 排序补充变更尚未提交。
- 推送未完成：`git push origin int_main` 被 non-fast-forward 拒绝；当前分支 `ahead 6, behind 6`，且工作区存在非本任务并行改动，暂不能安全 pull/rebase。
