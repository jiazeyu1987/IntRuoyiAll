# 一线 PQC QA 项目工序来源执行日志

## User Intent

- 用户确认正确链路为：活跃订单 -> 对应产品 -> 对应工艺路线 -> 对应项目代码 -> QA 检验项目 -> 提取并去重工序。
- 用户明确指出活跃订单工序快照、路线全部工序和待检任务集合都不是“选工序”列表的业务来源，并要求修改和验证。
- 用户在后端回归完成后追加要求：进行真实 E2E 验证。
- 用户根据 E2E 错误进一步澄清：`AW.107.02.01.2010` 只是订单产品代码，必须先用它定位对应工艺路线，再由工艺路线绑定定位项目代码；不得把订单产品代码直接当 DCC 项目代码。
- 用户于 2026-08-09 明确选择“工序列表优先”：历史 QA 展示字段为空不得阻断工序列表，但检验详情和正式提交仍需严格校验。
- 用户于 2026-08-10 反馈当前一线 PQC 工序列表只显示一个，要求显示 QA 对应的全部工序；本轮按该最新口径继续修复并验证。

## BDD

- BDD: 仅展示项目代码下 QA 检验项目工序 -> Given 活跃订单产品绑定正式工艺路线，路线对应项目代码，且项目代码下正式 QA 检验项目只覆盖部分路线工序 / When 一线 PQC 请求该活跃订单工序列表 / Then 只返回 QA 检验项目覆盖的工序，不返回路线或活跃订单快照中的其它工序。
- BDD: QA 检验项目工序去重 -> Given 同一项目代码下多个 QA 检验项目属于同一道工序 / When 一线 PQC 构建工序列表 / Then 按正式 `routeProcessId + processId` 只返回一次该工序。
- BDD: 正式来源缺失快速失败 -> Given 活跃订单无法唯一解析产品路线对应项目代码，或 QA 检验项目缺少正式工序身份 / When 请求工序列表 / Then 服务显式失败，不使用快照、路线全集或默认值补齐。
- BDD: 真实页面展示 QA 工序集合 -> Given 本机 `int_main` 已加载本次后端实现且默认管理员可进入一线 PQC / When Playwright 从真实页面选择待检活跃订单并打开“选工序” / Then 页面工序按钮与该订单 `active-order/processes` 成功响应逐项一致、正式工序身份无重复，且没有调用 PQC 提交或其它持久化写接口。
- BDD: 由订单产品定位路线后解析路线项目代码 -> Given 工单产品代码只用于命中当前路线绑定，当前路线另有物料代码与唯一启用 DCC 项目代码精确对应，且存在未绑定该路线的其它 DCC 项目 / When 一线 PQC 请求该活跃订单工序列表 / Then 使用路线绑定命中的 DCC `productMasterId` 查询正式 QA 规程，不把工单产品代码当项目代码，也不读取未绑定路线的项目。
- BDD: 旧版已发布检验项缺少展示文本仍可选工序 -> Given 当前工艺路线已有正式发布 QA 检验项目且工序身份完整，但历史数据的 `inspectionTool` 或 `samplingPlanText` 为空 / When 一线 PQC 打开“选工序” / Then 仍按正式 QA 检验项目返回该工序并原样保留空字段，不用默认值补齐，也不因非工序身份字段阻断列表。
- BDD: 展示字段完整性在使用边界严格拦截 -> Given 工序列表已返回历史 QA 检验项且 `inspectionTool` 或 `samplingPlanText` 为空 / When 用户打开检验方法详情或发起正式 PQC 提交 / Then 页面按缺失正式字段明确提示并不打开详情、不发送提交请求；后端提交边界继续 fail fast。
- BDD: QA 对应全部工序展示 -> Given 活跃订单已定位到正式工艺路线和对应项目代码，且该 QA 来源下存在多道有检验项目的工序 / When 一线 PQC 打开“选工序” / Then 页面和 `active-order/processes` 返回 QA 对应的全部不重复工序，不得因只有一个待检任务、单个规程头或单个产品候选而缩减为一个工序。

## Command Intent

- 只读核对现有服务、Mapper、测试和相关历史任务，保留工作区并行改动。
- 后续只运行目标后端测试及必要的相邻回归，不启动服务、不修改数据库数据。
- E2E 扩展只使用本机 `int_main` 的 `8081/48081`，通过 Playwright 操作真实前端；先核对运行态是否加载本次后端实现，再执行只读页面路径，不创建或修改业务数据。

## Milestone Updates

- 2026-08-09：确认当前未提交实现把 `mes_pro_process_pool_active_order_process_snapshot` 作为候选工序集合，与用户最新明确口径冲突。
- 2026-08-09：确认现有长期门禁要求“全部快照工序”，已在任务文档记录由用户最新指令覆盖，收尾时必须同步长期经验。
- 2026-08-09：新增回归场景，路线/快照包含两道工序，但路线项目代码对应的正式 QA 检验项目仅覆盖一道且重复出现，期望接口只返回该非重复 QA 工序。
- 2026-08-09：服务改为从活跃订单工艺路线的产品代码唯一解析启用的 DCC 项目，再以 DCC `productMasterId + routeId + routeVersionId` 读取正式发布 QA 规程和检验项目；候选工序按 `routeProcessId + processId` 去重，路线当前工序仅用于名称、排序和工位补充。
- 2026-08-09：待检任务只附着到 QA 候选工序，且任务规程查询使用 DCC 项目的 QA `productMasterId`，不再使用 MES 工单产品 ID 或活跃订单工序快照扩展候选集合。
- 2026-08-09：多产品 RED 后将项目代码解析范围收紧为当前工单 `productId` 对应物料代码；同一路线其它产品不参与 DCC 项目匹配，启用项目代码必须唯一且必须具备正数 `productMasterId`。
- 2026-08-09：更新 `docs/backend-development.md#PQC 待检准入与工序选择必须分离` 和 `docs/experience-index.md`，移除与用户最新规则冲突的“展示全部活跃订单快照工序”长期门禁。
- 2026-08-10：重新打开本任务 M9，用户最新口径要求一线 PQC 显示 QA 对应的全部不重复工序；优先排查后端 QA 产品候选和已发布规程集合是否把候选错误收敛成一个。

## Verification Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldListDistinctQaInspectionItemProcessesFromRouteProjectCode" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，测试编译明确指出 `MesFrontlinePqcContextServiceImpl` 构造器缺少新增的 `DccProjectCodeMapper` 参数；证明现有服务没有正式路线项目代码解析依赖。
- RED: 注入 `DccProjectCodeMapper` 后运行 `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest#shouldListDistinctQaInspectionItemProcessesFromRouteProjectCode" test` -> FAIL，期望工序 `[4001]`，实际返回活跃订单快照工序 `[4001, 4002]`；证明现有业务实现仍错误使用快照全集。
- RED: 在目标用例增加“同一路线绑定当前产品与其它产品、两个产品代码均存在 DCC 项目”的场景后，直接运行已定向编译的目标测试 -> ERROR，服务报 `matchedProjectIds=[9011, 9012]`；证明解析范围错误扩大到路线全部产品，而不是活跃订单当前产品。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest#shouldListDistinctQaInspectionItemProcessesFromRouteProjectCode" test` -> PASS，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`；确认路线额外工序被排除、重复 QA 规程/检验项目工序只返回一次，并使用 DCC 项目的 QA `productMasterId` 查询规程。
- REGRESSION: 常规 Maven 相邻回归在测试编译阶段被本任务外、于目标 GREEN 后新建的未跟踪文件 `MesReportSharedAllocationSchemaTest.java:49` 语法错误阻断；本任务不修改该并行任务文件。将基于目标 GREEN 同次成功编译的测试字节码直接运行 Surefire 相邻回归，并保留该限制。
- GREEN: 定向编译更新后的服务与测试后，运行 `mvn surefire:test "-Dtest=MesFrontlinePqcContextServiceTest#shouldListDistinctQaInspectionItemProcessesFromRouteProjectCode"` -> PASS，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`；额外锁定同路线多产品时只读取当前活跃订单产品代码，并验证不调用 `selectListByRouteId` 扫描路线全部产品。
- GREEN: `mvn surefire:test "-Dtest=MesFrontlinePqcContextServiceTest#shouldListDistinctQaInspectionItemProcessesFromRouteProjectCode+shouldLoadProcessesFromSelectedActiveOrderProductRoute+shouldDisplayOnlyQaInspectionItemProcessesWhenRouteHasExtraProcesses+shouldHideRouteProcessesWithoutQaInspectionItemsAndAttachTaskToConfiguredProcess+shouldAttachPqcTaskFromQaRegulationProcessWhenCurrentRouteProcessIdsDrift+shouldExposeFirstAndPatrolTaskOptionsForSameProcess+shouldPreparePqcPieceDetailContextWithBulkQueriesOnly+shouldDisplaySubmittedPqcTaskProcessWithoutTaskContextAndKeepPendingProcess+shouldFailFastWhenPendingPqcTaskMissingFormalProcessIdentity+shouldRejectNonFormalQaOwnerWhenAttachingPendingTaskContext"` -> PASS，`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`。
- GREEN: `mvn -pl yudao-module-mes -DskipTests compile` -> PASS；生产类在前一步定向 `javac` 已编译，Maven 确认主模块编译状态有效。
- GREEN: 并行任务修复其测试语法错误后，重新运行标准 Maven 生命周期的同一组 10 个相关测试 -> PASS，`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`；此前常规测试编译限制已解除。
- E2E PREFLIGHT: `8081` 由 `E:\IntRuoyi\IntRuoyiFronted` Vite 占用且 HTTP 200；`48081` 由 `E:\IntRuoyi\output\runtime\int_main` 稳定 Jar 占用且 health `UP`。运行 Jar 字节码包含 `DccProjectCodeMapper`、当前产品参数的 `resolveQaProjectProcessSource` 与 `resolveQaInspectionItemRouteProcesses`，确认已加载本次实现。
- E2E PREFLIGHT: 官方 `scripts/preflight/login-preflight.mjs` 使用本机 Chrome、`芋道源码/admin` 和真实一线 PQC 页面 -> PASS；未记录密码或 token。
- E2E TOOLING: `playwright-cli --version` 输出 `0.1.18` 后触发 Windows `UV_HANDLE_CLOSING` 断言；按 `docs/e2e-rules.md#Playwright 快照与 daemon 收尾门禁` 归因为 CLI 工具链失败，改用仓库现有 Playwright Node 运行方式承载同一真实页面路径，不降级为 API-only。
- E2E RUNTIME: 继续验证时 `8081` 已停止、`48081` health 仍为 `UP`；标准 frontend 重启脚本曾把 Vite 拉起后由 `pnpm` 包装进程以 `4294967295` 退出，随后使用项目正式 Vite 入口和相同 `env.local/8081` 配置启动。冷启动首轮浏览器加载超时后，模块预热完成，第二轮进入真实登录与目标页面。
- E2E BLOCKED: Playwright 使用 `芋道源码/admin` 登录 `http://127.0.0.1:8081/mes/pro/feedback/edhr-batch-pqc-fill`，`active-orders` HTTP 200/业务码 0；选择真实活跃订单 `881MO090889` 后，`active-order/processes?workOrderId=923889&routeId=922119` HTTP 200/业务码 `1040760103`，错误为当前产品 `902149` 的项目代码 `AW.107.02.01.2010` 未匹配任何启用 DCC 项目，`matchedProjectIds=[]`。
- E2E SUPPORTING CHECK: 使用同一已登录浏览器上下文只读请求 `GET /admin-api/dcc/project-codes/page?pageNo=1&pageSize=100&projectCode=AW.107.02.01.2010` -> HTTP 200/业务码 0、`total=0`、`records=[]`；证明该代码不是“存在但停用”，而是当前租户 DCC 项目代码记录不存在。该查询未使用 SQL，也未修改数据。
- ROOT CAUSE REVISION: 用户确认上述查询目标本身错误；`AW.107.02.01.2010` 是订单产品物料代码，不要求其本身存在于 `dcc_project_code.project_code`。源码 `MesFrontlinePqcContextServiceImpl.resolveQaProjectProcessSource` 错在直接用 `workOrder.productId -> mes_md_item.code` 匹配 DCC；项目已有 `MesTeamLeaderActiveOrderServiceImpl.resolveDccProjectsByRouteId` 正式口径，会读取路线全部 `mes_pro_route_product` 绑定物料代码后匹配启用 DCC 项目。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest#shouldListDistinctQaInspectionItemProcessesFromRouteProjectCode" test` -> ERROR，业务码 `1040760103`，错误详情 `projectCode=ORDER-ITEM, matchedProjectIds=[]`；测试路线已绑定 `ROUTE-PROJECT` 且存在对应启用 DCC 项目，证明现有实现仍把订单产品代码直接当项目代码，并没有读取路线绑定。
- GREEN: 同一目标 Maven 用例 -> PASS，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`；服务已从当前 `routeId` 的全部 `mes_pro_route_product` 绑定物料代码解析唯一启用 DCC 项目，并使用其 `productMasterId` 查询正式 QA 规程。
- REGRESSION: `mvn surefire:test` 定向运行项目代码来源、QA 工序去重、任务附着、上下文和失败门禁共 10 个一线 PQC 场景 -> PASS，`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`。
- REAL DATA READBACK: 使用已登录真实前端只读调用 `GET /mes/pro/route-product/list-by-route?routeId=922119`，路线绑定代码为 `ID`、`AW.107.02.01.2010`、`AW.107.02.01.1009`、`AW.107.02.01.2036`；逐个只读查询 DCC 项目后，唯一启用精确匹配是 `projectCode=ID`、`projectId=147`、`productMasterId=11`。证明订单 `AW.*` 代码只负责定位路线，路线再定位正式项目代码 `ID`。
- E2E RUNTIME BLOCKER: 目标测试完成后，`48081` 监听由并行任务接管；最终复核时运行的是 `E:\IntRuoyi\output\runtime\int_main\backend-report-shared-allocation-20260809-v3.jar`（PID `36444`，启动时间 `21:43:25`），仍返回旧的“订单产品代码直接匹配 DCC 项目代码”行为。按共享运行态归属规则，本任务不得停止或覆盖该进程，也不得改用随机端口冒充 `int_main` E2E；因此尚未把修复加载到真实页面做最终工序列表断言。
- E2E RECHECK: `node doc/tasks/20260809-frontline-pqc-qa-project-process-source/frontline-pqc-process-source.e2e.cjs` -> `BLOCKED`；真实页面选择工单 `923834`、路线 `922119` 后，当前 `v3` 运行态返回业务码 `1040760103`，详情仍为 `productId=902101, projectCode=AW.107.02.01.1009, matchedProjectIds=[]`。这直接证明共享后端尚未加载本轮“路线绑定解析项目代码”修复；脚本记录 `pqcContextPosts=[]`、`persistentMesWrites=[]`。
- E2E WRITE GUARD: 本轮 `pqcContextPosts=[]`、`persistentMesWrites=[]`，没有调用 `/pqc/switch-employee`、`/pqc/submit` 或其它 MES 持久化写接口；`pageErrors=[]`。由于业务前置已失败，未打开工序选择器，不能把本轮记为 E2E PASS。
- EXPERIENCE CONSOLIDATION: 按 `project-experience-consolidation` 将可复用规则合并到现有 `docs/backend-development.md#PQC 待检准入与工序选择必须分离` 和 `docs/experience-index.md`：订单产品代码只定位正式工艺路线，必须再从该路线全部 `mes_pro_route_product` 绑定物料代码中精确解析唯一启用 DCC 项目；禁止把订单产品代码直接当项目代码。真实路线 ID、订单号和进程 PID 属于一次性证据，不写入长期规则，也不新建经验文档。
- RUNTIME UPDATE: 安全替换本任务此前启动的 `48081` 进程并加载路线项目代码修复后，真实 E2E 不再返回 `matchedProjectIds=[]`；选中工单 `923834`、路线 `922119` 后已进入正式 QA 规程解析。
- REAL DATA READBACK: 当前路线/版本最新发布规程实际归属 `productId=902149`，规程版本 `54`、工序 `清洗工序`、检验项 `ID-001-WASH-APP`；DCC 项目 `ID` 的 `productMasterId=11` 本身没有该路线发布规程。源码已有正式团队长口径会把“路线全部绑定物料 ID + DCC productMasterId”共同作为 QA 产品候选，再要求当前路线/版本只唯一命中一个 QA 产品。本服务已对齐该正式规则，不静默偏好任一候选。
- GREEN: 路线 QA 产品候选修正后的目标 Maven 用例 -> PASS，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`；相关一线 PQC 回归 -> PASS，`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`。
- E2E RED: 加载路线 QA 产品候选修正后，真实页面工序接口返回业务码 `1040760109`，详情为 `inspectionItem.inspectionTool itemCode=ID-001-WASH-APP`；证明项目代码和 QA 产品来源已正确，下一阻塞来自历史发布检验项展示字段为空。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldListQaProcessWhenLegacyPublishedItemDisplayFieldsAreBlank" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> ERROR，业务码 `1040760109`，精确缺失字段为 `inspectionItem.inspectionTool itemCode=legacy-item`。
- GREEN: 移除“选工序”构造阶段对 `inspectionTool/samplingPlanText` 的额外必填检查、原样保留正式空值后，同一目标用例 -> PASS；包含项目代码来源、QA 工序去重、任务附着和失败门禁的相关回归 -> PASS，`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`。
- CONCURRENT FILE CONFLICT: 在上述 GREEN 运行期间，并行任务 `20260809-frontline-qa-inspection-detail-fields` 于 `2026-08-09 22:50:39` 修改同一 `MesFrontlinePqcContextServiceImpl.java`，重新加入 `inspectionTool/samplingPlanText` 的 fail-fast 校验；该任务文档明确要求历史字段为空时整接口失败，并记录与本任务新增测试的产品口径冲突。按共享文件冲突规则，本任务不再次覆盖。
- RUNTIME VERIFY: 当前 `48081` 运行 `backend-report-shared-allocation-20260809-v4-pqc-route-qa-legacy-fields-20260809-225651.jar`，SHA-256 `CC6B60E98EA85BB7F8BCB10D3A00FE101C6F6352CF8210B404489D88F335E488`，内嵌 MES Jar 为 STORED，7 个服务 class 与当时共享 `target/classes` 哈希一致；PID `30636`，health `UP`。
- E2E BLOCKED: 当前共享最终代码真实复跑仍返回业务码 `1040760109`、缺失字段 `inspectionItem.inspectionTool`；`pqcContextPosts=[]`、`persistentMesWrites=[]`、`pageErrors=[]`、`consoleErrors=[]`。需要用户明确选择“工序列表优先、空展示字段不阻断”或“详情完整性优先、先通过正式 QA 保存/发布补齐数据”，才能继续。
- PRODUCT DECISION: 用户确认“工序列表优先”。实现方向固定为列表使用不补值的原样映射，详情打开和正式提交使用严格完整性校验；这不是 fallback，而是将“工序身份发现”和“检验详情可用性”拆到各自正式边界。
- IMPLEMENTED: `resolvePendingPqcTaskContexts` 改用独立 `toProcessListInspectionItem`，只校验检验项对象存在并原样映射 `inspectionTool/samplingPlanText`；提交使用的 `toInspectionItem` 继续按两个正式字段 fail fast，共用 `buildInspectionItem` 避免重复映射。
- IMPLEMENTED: 前端 API 和页面类型明确允许两个列表字段为 `null`；`openPqcMethodDialog` 在打开详情前校验正式原文，`handleValidate` 与 `buildPqcInspectionSubmitPayload` 在签名和请求前再次校验，缺失时使用现有错误消息组件提示，不打开详情/签名弹窗。
- RED: `node tests/e2e/frontline-pqc-process-list-display-field-boundary-static.spec.cjs` -> FAIL，当前列表仍调用严格 `toInspectionItem`。
- GREEN: 同一静态合同 -> PASS；相邻 `frontline-pqc-sampling-equipment-dialog-static.spec.cjs` 与 `frontline-pqc-fact-dialog-static.spec.cjs` -> PASS，证明提交严格校验和字段直接展示合同仍保留。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest#shouldListQaProcessWhenLegacyPublishedItemDisplayFieldsAreBlank" test` -> PASS，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- GREEN: 一线 PQC 项目代码来源、QA 工序去重、任务附着、历史空展示字段和失败门禁共 11 个回归 -> PASS，`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`。
- GREEN: `pnpm ts:check` -> PASS。
- BUILD ARTIFACT REPAIR: 一次被中断的 Maven 编译留下唯一 0 字节生成类 `MesTeamLeaderActiveOrderAddReqVO.class`；只删除该已确认位于 `target/classes` 的空生成文件后运行 `mvn -pl yudao-module-mes -DskipTests compile` -> BUILD SUCCESS，未修改对应业务源码，复查目标测试正常。
- RUNTIME UPDATE: 生成并校验 `backend-report-shared-allocation-20260809-v4-pqc-process-list-priority-20260809-234242.jar`，SHA-256 `9CE2C05A6F951B17250362CEF10F6BB64E6F1D50D96213C9A6148471BFCA9713`；内嵌 MES Jar 为 STORED，7 个服务 class 哈希全部一致。当前 PID `22540`、端口 `48081`、health `UP`。
- E2E GREEN: 真实 Playwright 登录一线 PQC，选择工单 `881MO090935`，`active-orders` 与 `active-order/processes` 均 HTTP 200/业务码 0；页面“选工序”与接口逐项一致，只显示 `1. 清洗工序`，正式身份 `980647:922987` 且无重复。
- E2E GREEN: 点击缺正式原文的检验方法时详情弹窗不打开；点击提交时签名弹窗不打开。结果记录 `displayFieldBoundary.detailBlocked=true`、`submitBlockedBeforeSignature=true`。
- E2E WRITE GUARD: `/pqc/switch-employee` 仅用于运行上下文；`persistentMesWrites=[]`、`targetFailures=[]`、`pageErrors=[]`、`consoleErrors=[]`。
- E2E GREEN: 2026-08-10 当前最终运行态复验通过。`48081` 运行 `backend-report-shared-allocation-20260810-final-20260810-002331.jar`、PID `47284`、health `UP`；真实页面订单 `881MO090935`、工单 ID `923834`、路线 `922119`，接口工序为 `["清洗工序"]`，页面工序为 `["1. 清洗工序"]`，正式身份 `980647:922987`。历史展示字段缺失边界仍为 `detailBlocked=true`、`submitBlockedBeforeSignature=true`；`persistentMesWrites=[]`、`targetFailures=[]`、`pageErrors=[]`、`consoleErrors=[]`。
- EXPERIENCE CONSOLIDATION: 将“路线绑定物料 ID + DCC productMasterId 共同作为 QA 产品候选并要求唯一命中”“工序发现不被展示原文缺失阻断、详情/提交严格校验”合并到现有 `docs/backend-development.md` 和 `docs/experience-index.md`，未新建长期经验文档。
- INDEPENDENT VERIFICATION: 2026-08-10 按 `independent-verification-gate` 重新审计当前状态。代码核对确认列表路径使用 `toProcessListInspectionItem` 且不校验展示原文，详情/提交路径仍使用 `toInspectionItem` / `assertPqcInspectionDisplayFieldsReady` 严格拦截；`48081` 当前运行 `backend-report-shared-allocation-20260810-final-20260810-002331.jar`、PID `47284`、health `UP`。
- INDEPENDENT VERIFICATION: `node tests/e2e/frontline-pqc-process-list-display-field-boundary-static.spec.cjs` -> PASS；`node tests/e2e/frontline-pqc-sampling-equipment-dialog-static.spec.cjs` -> PASS；`node tests/e2e/frontline-pqc-fact-dialog-static.spec.cjs` -> PASS；`pnpm ts:check` -> PASS。
- INDEPENDENT VERIFICATION: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest#shouldListDistinctQaInspectionItemProcessesFromRouteProjectCode+shouldLoadProcessesFromSelectedActiveOrderProductRoute+shouldDisplayOnlyQaInspectionItemProcessesWhenRouteHasExtraProcesses+shouldHideRouteProcessesWithoutQaInspectionItemsAndAttachTaskToConfiguredProcess+shouldAttachPqcTaskFromQaRegulationProcessWhenCurrentRouteProcessIdsDrift+shouldExposeFirstAndPatrolTaskOptionsForSameProcess+shouldListQaProcessWhenLegacyPublishedItemDisplayFieldsAreBlank+shouldPreparePqcPieceDetailContextWithBulkQueriesOnly+shouldDisplaySubmittedPqcTaskProcessWithoutTaskContextAndKeepPendingProcess+shouldFailFastWhenPendingPqcTaskMissingFormalProcessIdentity+shouldRejectNonFormalQaOwnerWhenAttachingPendingTaskContext" test` -> PASS，`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`，完成时间 `2026-08-10T01:06:51+08:00`。
- M9 DIAGNOSIS: 真实登录 `芋道源码/admin` 后只读请求订单 `881MO090935`，`active-order/processes` HTTP 200/业务码 0，仅返回 `清洗工序`，正式身份 `980647:922987`；路线 `922119` 绑定物料代码为 `ID`、`AW.107.02.01.2010`、`AW.107.02.01.1009`、`AW.107.02.01.2036`。
- M9 DIAGNOSIS: 同一只读诊断显示路线候选产品和精确 DCC 项目候选中，只有 `productId=902149` 的 QA 配置状态为 `configured=true`，`regulationCount=1`，`currentVersionId=54`，规程 `PQC-ID-001-RP980647`，发布版本仅对应 `routeProcessId=980647` / `processId=922987` / `清洗工序`；其它候选产品 `924005`、`902101`、`901965`、`11`、`12`、`13`、`14` 均未配置 QA 规程。
- M9 DIAGNOSIS: 只读请求历史订单 `881MO090889` 得到相同结论：接口只返回 `清洗工序`，对应 QA 配置仍只有 `productId=902149` 的 1 条已发布规程。
- M9 REGRESSION CHECK: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest#shouldPreparePqcPieceDetailContextWithBulkQueriesOnly+shouldDisplaySubmittedPqcTaskProcessWithoutTaskContextAndKeepPendingProcess" test` -> PASS，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`；证明当正式 QA 已发布数据包含两个工序时，当前后端列表路径能返回两个 QA 工序。

## Blockers

- M9 blocker：当前真实本机数据只存在一个正式已发布 QA 工序（清洗工序），代码没有可读取的其它 QA 工序；继续修改代码会变成用路线全集或待检任务集合扩展候选，违反用户先前确认的“QA 检验项目工序来源”口径和 no-fallback 门禁。
- 未执行整个 MES 模块全量测试；验证范围为目标场景及 10 个相邻一线 PQC 场景。

## Closeout

- Bug regression evidence validator -> PASS；Frontend feature evidence validator -> PASS；关键结果已合并保留在本日志与 `verification-report.md`。
- `task-closeout-cleanup` preview -> READY，无 blocked/warnings；明确保留核心三份任务记录、当前运行 Jar 及其 stdout/stderr。
- 首次 apply 在 Windows 长路径临时 class 上因 Python `shutil.rmtree` 报 `FileNotFoundError`，已部分完成删除。随后仅对预览中剩余、绝对路径已验证位于 `E:\IntRuoyi\output\runtime\int_main` 的两个任务自有 hotpatch 目录使用长路径删除，再次运行 apply -> PASS。
- 最终清理删除本任务一次性 evidence、E2E 脚本、Playwright 截图/结果、旧热补丁 Jar、旧日志及解包目录；保留正式后端/前端回归测试、生产代码、核心任务记录和当前运行 Jar。
- 2026-08-10 最终运行态复验使用任务目录临时 Playwright 脚本完成；复验 PASS 后该临时脚本删除，仅保留本日志中的结果摘要。
- 2026-08-10 独立验收未发现新增 blocker；复跑验证仅产生命令输出，无新增临时文件需要清理。
- 当前为主工作区 `int_main`，不是 linked worktree；用户未要求 Git 操作，未提交、未合并、未推送。
