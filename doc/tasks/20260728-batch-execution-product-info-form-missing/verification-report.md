# Verification Report

## Scope

- 后端服务：`MesProEdhrBatchExecutionServiceImpl`。
- 后端测试：`MesProEdhrBatchExecutionServiceTest`。
- 前端页面：`IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`。
- 前端静态合同：`IntRuoyiFronted/tests/e2e/edhr-batch-product-info-virtual-process-static.spec.js`。

## Results

- RED 已复现：新增详情回归测试在修复前失败，实际缺少“产品信息”成员表单。
- GREEN 已通过：修复后目标测试 PASS。
- 相邻回归已通过：新建批次产品信息成员补入、详情恢复、分页恢复和本次部分缺失恢复共 4 个方法 PASS。
- 本轮 80 排序补充验证已通过：产品信息成员任务固定 `batchRecordSort=80`，顺序位于正式工序批记录之后，且前序批记录未完成时门禁提示“前一张批记录未填写完成”。
- 真实页面 E2E 已通过：本机 `8081/48081` 下登录 `芋道源码/admin`，打开批次 `EDHRB-1785224948633` 详情，接口和页面均显示“产品信息”位于粗洗工序正式批记录表单之后，后端排序为 `80`，且前序批记录未完成时不可打开填写。
- 本轮用户截图复现的 UI 归属问题已修复：后端仍保留产品信息来源 `routeProcessSort=1` / `routeProcessId=第 1 工序`，前端现在按产品信息虚拟工序单独分组为左侧 `80 产品信息`。
- 真实页面 E2E 已再次通过：从批次执行列表按工单 `881MO090889` 进入详情，点击第 1 工序时右侧不含产品信息，点击 `80 产品信息` 时右侧仅显示“产品信息”卡片。
- 后端无需重启：本轮变更是前端展示分组修复；当前 `48081` 后端已返回产品信息任务 `batchRecordSort=80`，`8081` Vite 热更新已在真实页面生效。
- `git diff --check` 已执行，退出码 0；仅 PowerShell 输出 CRLF 替换提示。

## Commands

- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_includesProductInfoMemberFromSameBatchRecordVersion+getDetail_shouldRecoverMissingRouteProcessTasksBeforeRendering+getPage_shouldRecoverMissingRouteProcessTasksBeforeRendering+getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `git diff --check`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS after 80 sorting update.
- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_includesProductInfoMemberFromSameBatchRecordVersion+getDetail_shouldRecoverMissingRouteProcessTasksBeforeRendering+getPage_shouldRecoverMissingRouteProcessTasksBeforeRendering+getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS after 80 sorting update.
- Real E2E inline Playwright script -> PASS. Frontend `http://127.0.0.1:8081`, backend `http://127.0.0.1:48081`, tenant/user label `芋道源码/admin`, page `/mes/pro/feedback/edhr-batch-execution/detail?id=900000000900`, screenshot `output/playwright/20260728-product-info-80-e2e.png`.
- `node tests\e2e\edhr-batch-product-info-virtual-process-static.spec.js` -> RED before page fix, then PASS after page fix.
- `node tests\e2e\edhr-batch-execution-list-sfc-compile-static.spec.js` -> PASS.
- `node tests\e2e\edhr-batch-companion-forms-right-panel-static.spec.js` -> PASS.
- `node tests\e2e\edhr-batch-process-companion-forms-static.spec.js` -> PASS.
- `node tests\e2e\edhr-batch-card-title-draft-marker-static.spec.js` -> PASS.
- `node tests\e2e\edhr-batch-detail-hide-red-box-static.spec.js` -> PASS.
- `node tests\e2e\edhr-batch-process-form-card-fillers-static.spec.js` -> PASS.
- Real E2E inline Playwright script -> PASS. Frontend `http://127.0.0.1:8081`, backend `http://127.0.0.1:48081`, tenant/user label `芋道源码/admin`, list path `/mes/pro/feedback/edhr-batch-execution?workOrderCode=881MO090889`, detail id `900000000900`, screenshot `output/playwright/20260728-product-info-virtual-process-80-e2e.png`.
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit --pretty false --skipLibCheck` -> FAIL on existing unrelated TS errors in BPM, DCC, FormCenter, RouteFlowGraphDesigner and other modules.
- `node tests\e2e\edhr-flow-navigation-filter-static.spec.js` -> FAIL on existing broad-contract anchor drift: `必须能定位 当前工序证据分组 终点`。

## Real E2E Evidence

- Runtime: frontend HTTP `200`; backend health `UP`; backend runtime process is the current `int_main` runtime jar under `E:\IntRuoyi\output\runtime\int_main`.
- Target batch: `EDHRB-1785224948633` / DB id `900000000900`.
- API assertion: 粗洗工序表单为 `[粗洗工序生产记录, 损耗单, 过程检验记录, 产品信息]`，排序为 `[1, 2, 3, 80]`。
- Product info assertion: task id `7028`, `batchRecordSort=80`, `formSlotType=MAIN`, `recordCategory=BATCH_RECORD`, `available=false`, `gateMessage=前一张批记录未填写完成`。
- Page assertion: 右侧表单卡片显示产品信息在粗洗工序生产记录之后，且产品信息卡片没有“打开填写”动作；浏览器 console error count `0`。
- UI process-group assertion: 左侧流程存在独立 `80 产品信息`；第 1 工序右侧卡片为 `[粗洗工序生产记录*, 损耗单, 过程检验记录]`，不包含“产品信息”；点击 `80 产品信息` 后右侧卡片为 `[产品信息]`。
- No-write assertion: Playwright 监听到的 `/admin-api/mes/` 写请求数量为 `0`。
- Risk note: 只读 DB 扫描发现 `tenant_id=122` 仍有旧存量产品信息任务 `batch_record_sort=1`；如果验收口径包含历史存量批次统一修正，需要独立授权数据修复，本次未改库。

## Remaining

- 等待本轮前端展示修复提交、cleanup apply 和 `git push origin int_main`。
- 当前工作区有并行任务未提交改动，本任务只暂存 `BatchExecutionDetailPage.vue` 与本任务文档。
