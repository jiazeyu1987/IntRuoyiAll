# Execution Log

## User Intent

用户反馈：批次执行里面的批记录表单的“产品信息表单”缺失。

## Initial State

- 已读取 `docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/frontend-development.md`、`docs/backend-development.md`。
- 已读取 bug-regression-fix-loop 技能与 evidence contract。
- `git status --short --branch` 显示当前工作区在本任务开始前已有未提交改动，并且 `int_main` 领先 `origin/int_main` 4 个提交；本任务需避免误混入既有改动。
- 任务执行期间并行基线提交 `3fb50fa6 chore: baseline dirty workspace before edhr switch fix` 推进了 `int_main`，并将本任务早期新增测试与任务文档纳入基线；后续只继续维护本任务剩余服务修复与证据更新。

## BDD

- `BDD: 批次执行展示产品信息表单 -> Given 工序设置中正式逐工序批记录表单绑定包含“产品信息表单” When 用户打开批次执行详情 Then 批记录表单区域必须展示“产品信息表单”，且该结果不得由 formBindings 或工序开始配置推断。`
- `BDD: 产品信息表后置填写 -> Given 同工序存在正式批记录表单和同版产品信息成员表单 When 用户打开批次执行详情 Then 正式批记录表单先显示且可填写，产品信息表固定排序为 80，并在前序正式批记录未完成前被门禁阻塞。`
- `BDD: 产品信息独立 80 工序 -> Given 后端任务因来源批记录绑定仍带有第 1 工序 routeProcessId When 用户打开批次执行详情 Then 前端必须把 MAIN+BATCH_RECORD 的产品信息任务按虚拟 80 工序单独分组，不能显示在第 1 工序右侧表单卡片中。`

## RED / GREEN

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，期望 `[RPT-DETAIL-PRODUCT-INFO-MEMBER, RPT-DETAIL-PRODUCT-INFO-PROCESS]`，实际仅 `[RPT-DETAIL-PRODUCT-INFO-PROCESS]`。
- GREEN: 同一命令复跑 -> PASS，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_includesProductInfoMemberFromSameBatchRecordVersion+getDetail_shouldRecoverMissingRouteProcessTasksBeforeRendering+getPage_shouldRecoverMissingRouteProcessTasksBeforeRendering+getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 初次 FAIL，暴露产品信息与源表单同工序 `batch_record_sort=1` 唯一键冲突；修正产品信息排序为源表单前一位后复跑 PASS，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，期望顺序 `[工序生产记录, 产品信息]`，实际 `[产品信息, 工序生产记录]`。
- GREEN: 同一命令在固定产品信息排序 `80` 后复跑 -> PASS，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_includesProductInfoMemberFromSameBatchRecordVersion+getDetail_shouldRecoverMissingRouteProcessTasksBeforeRendering+getPage_shouldRecoverMissingRouteProcessTasksBeforeRendering+getDetail_shouldRecoverMissingProductInfoMemberTaskWhenOtherRouteTaskExists" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 初次受并行填写人规则改动 testCompile 阻塞；并行字段补齐后复跑 PASS，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- RED: `node tests\e2e\edhr-batch-product-info-virtual-process-static.spec.js` -> FAIL，`BatchExecutionDetailPage.vue` 缺少 `isProductInfoProcessTask` / `buildProcessTaskGroupKey`，产品信息任务仍会按原 `routeProcessId` 合并到第 1 工序。
- GREEN: `node tests\e2e\edhr-batch-product-info-virtual-process-static.spec.js` -> PASS，详情页按 `MAIN + BATCH_RECORD + 产品信息/80` 识别独立虚拟 80 工序。
- REGRESSION: `node tests\e2e\edhr-batch-execution-list-sfc-compile-static.spec.js`、`node tests\e2e\edhr-batch-companion-forms-right-panel-static.spec.js`、`node tests\e2e\edhr-batch-process-companion-forms-static.spec.js`、`node tests\e2e\edhr-batch-card-title-draft-marker-static.spec.js`、`node tests\e2e\edhr-batch-detail-hide-red-box-static.spec.js`、`node tests\e2e\edhr-batch-process-form-card-fillers-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\edhr-flow-navigation-filter-static.spec.js` -> FAIL，宽合同无法定位既有“当前工序证据分组”终点，属于既有合同锚点漂移；本次用聚焦产品信息合同和相邻卡片合同覆盖目标行为。
- REGRESSION: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit --pretty false --skipLibCheck` -> FAIL，仍阻塞在既有全量 TS 问题，包括 `bpm/model/index.vue` 未使用方法、DCC/FormCenter/RouteFlowGraphDesigner 等历史类型错误；本次新增代码未出现独立类型错误。

## Milestone Updates

- 2026-07-28: 创建任务目录并记录适用门禁，准备定位详情接口与页面展示链路。
- 2026-07-28: 定位根因：新建批次已有同版产品信息补入逻辑，但历史/活跃批次只要存在任一 `ROUTE_FORM` 任务，读取恢复逻辑直接返回，导致只缺“产品信息”成员表单时不会补齐。
- 2026-07-28: 新增详情读取回归测试，覆盖已有工序生产记录任务但缺同版“产品信息”任务的活跃批次。
- 2026-07-28: 修复读取恢复逻辑：对已有正式 `MAIN + BATCH_RECORD` 任务按 `batchRecordDefinitionId + batchRecordVersionId` 查找同版产品信息成员报表，缺失时插入等待任务并重建初始填写任务；不读取 `formBindings`。
- 2026-07-28: 修复产品信息成员表单排序，确保插入排序在源表单之前，避免同批次、同工序、同 `batch_record_sort` 唯一键冲突。
- 2026-07-28: 根据用户确认调整产品信息成员表单排序口径：不再排在源表单之前，统一固定 `batchRecordSort/reportSort=80`，确保所有正式批记录表单填完后再填产品信息表。
- 2026-07-28: 更新长期门禁 `docs/backend-development.md` 与 `docs/experience-index.md`，记录产品信息固定排序 `80`，禁止按源表单排序 `-1` 推算。
- 2026-07-28: 执行 bug evidence 校验：`python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260728-batch-execution-product-info-form-missing\bug-regression-evidence.md` -> PASS。
- 2026-07-28: 执行 project-experience-consolidation，已将“已有 ROUTE_FORM 但产品信息成员表单部分缺失”的门禁沉淀到 `docs/backend-development.md`，并更新 `docs/experience-index.md` 关键词。
- 2026-07-28: cleanup preview/apply 已执行，保留 `task.md`、`execution-log.md`、`verification-report.md` 和 `bug-regression-evidence.md`，无删除项、无 blocked、无 warnings。
- 2026-07-28: 本任务实现提交 `842850cf fix: restore product info batch record task` 已创建。
- 2026-07-28: `git push origin int_main` -> FAIL，远端 non-fast-forward；当前分支 `ahead 2, behind 6`，且存在非本任务并行前端改动，不能安全 pull/rebase。
- 2026-07-28: 真实 E2E 运行态预检：`8081` 前端 HTTP 200，`48081/actuator/health` status `UP`，前端 PID 9040 归属 `E:\IntRuoyi\IntRuoyiFronted` Vite，后端 PID 26592 归属 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260728-212450.jar`。
- GREEN: 真实 Playwright 页面验证 -> PASS。登录本机 `芋道源码/admin`，打开 `/mes/pro/feedback/edhr-batch-execution/detail?id=900000000900`，详情接口返回批次 `EDHRB-1785224948633`；粗洗工序批记录表单顺序为 `[粗洗工序生产记录, 损耗单, 过程检验记录, 产品信息]`，排序 `[1, 2, 3, 80]`，产品信息任务 `formSlotType=MAIN`、`recordCategory=BATCH_RECORD`、`available=false`、门禁为“前一张批记录未填写完成”；页面右侧卡片同样显示产品信息在正式批记录之后，且未显示“打开填写”。截图：`output/playwright/20260728-product-info-80-e2e.png`。
- 2026-07-28: 只读 DB 扫描补充风险：`tenant_id=122` 仍存在旧存量产品信息任务 `batch_record_sort=1`，说明当前真实 E2E 已证明新运行态/目标批次链路正确，但若验收口径要求所有历史存量批次也统一改为 80，需要另开受控数据修复或恢复任务；本次未直接改库。
- 2026-07-28: 用户补充截图指出产品信息仍作为第一个工序下的表单显示，期望它是第 80 个工序的表单。
- 2026-07-28: 复核根因：后端任务已持久化 `batchRecordSort=80`，但为了保留来源批记录绑定仍返回 `routeProcessSort=1` / `routeProcessId=第 1 工序`；前端 `processTaskGroups` 仅按 `routeProcessId || routeProcessSort || id` 分组，导致产品信息在 UI 被合并到第 1 工序右侧。
- 2026-07-28: 修复 `BatchExecutionDetailPage.vue`：新增 `PRODUCT_INFO_PROCESS_SORT=80`、`PRODUCT_INFO_PROCESS_NAME='产品信息'`、`isProductInfoProcessTask`、`buildProcessTaskGroupKey`、`resolveProcessTaskGroupName`、`resolveProcessTaskGroupSort`，让 `MAIN + BATCH_RECORD` 产品信息任务独立为 `product-info:<reportId/taskId>` 工序组。
- 2026-07-28: 补充选择归属保护：产品信息任务不再沿用当前第 1 工序的填写人上下文，避免右侧卡片显示第 1 工序填写人兜底。
- 2026-07-28: 真实 E2E 复验 -> PASS。登录本机 `芋道源码/admin`，从列表按工单 `881MO090889` 进入详情；详情批次 `EDHRB-1785224948633` / id `900000000900`；产品信息任务 id `7028`，API `batchRecordSort=80`、`routeProcessSort=1`；页面左侧存在 `80 产品信息` 独立工序；点击第 1 工序时右侧仅 `[粗洗工序生产记录*, 损耗单, 过程检验记录]`；点击 `80 产品信息` 时右侧仅 `[产品信息]`；无 MES 写请求、无 console error。截图：`output/playwright/20260728-product-info-virtual-process-80-e2e.png`。
- 2026-07-28: 后端不需要重启；本轮是前端展示分组修复，现有 `48081` 后端已返回产品信息任务与 `batchRecordSort=80`，前端 Vite 热更新已在 `8081` 真实页面生效。

## Blockers

- 当前实现与真实 E2E 无阻塞。
- 工作区存在非本任务并行改动，本任务提交必须选择性暂存，避免混入 `RouteFlowGraphDesigner.vue`、`edhr-visual-fill-config*`、`node-chain-route-filter-local-sync` 等并行任务文件。
