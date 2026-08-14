# 一线 PQC 正式 QA 工序修复执行日志

## User Intent

- 用户确认球囊扩张压力泵 QA 规程页面没有“粗洗”，但一线 PQC 工序列表出现“粗洗工序”，并要求修改。

## BDD

- BDD: 未配置工序不得进入一线 PQC -> Given 球囊扩张压力泵当前正式 QA 配置不包含粗洗工序，When 一线 PQC 加载该产品当前活跃订单的工序列表，Then 列表不显示粗洗工序。
- BDD: 正式 QA 工序继续可用 -> Given 当前产品路线中的工序已被正式发布 QA 配置覆盖且存在待检项目，When 一线 PQC 加载工序列表，Then 返回该正式工序及其待检任务和检验项目。
- BDD: 旧版本夹具不得冒充当前配置 -> Given 历史任务把旧路线版本规程复制到当前版本且快照身份不一致，When 系统构建当前活跃订单 PQC 上下文，Then 不得把该记录作为当前正式 QA 工序来源。

## Command Intent

- 只读核对 QA 页面、发布接口、一线 PQC 服务、历史任务记录和本地数据身份。
- 后续数据变更仅针对已确认由 `20260808-pressure-pump-active-orders` 创建的污染记录，并先记录精确影响范围。

## Milestone Updates

- 2026-08-09：确认截图订单 `881MO090889` 对应 activeOrderId `49`、产品 `902149`、路线 `922119`、版本 `627`。
- 2026-08-09：确认一线 PQC 从当前版本 `PUBLISHED` 规程生成工序列表；数据库中“粗洗工序”规程由历史任务复制，版本快照仍引用 V21/旧路线工序，和外层 V27 身份不一致。
- 2026-08-09：确认 QA 页面当前显示的球囊扩张压力泵项目首行是“清洗/精洗”，不包含“粗洗”。
- 2026-08-09：在正式 QA 查询与一线 PQC 上下文校验中统一限定 `owner_module=MES_QA`，非正式 `CODX_QA` 数据将显式报错，不会降级消费。
- 2026-08-09：精确退役历史任务创建的 14 条 `CODX_QA` 规程及版本，将引用它们的 112 条未执行 PQC 任务改为 `CANCELLED`；未修改生产工艺路线本身。
- 2026-08-09：真实页面验证中，目标订单 `881MO090889` 不再出现于一线 PQC 待检工单列表；当前有 5 个其他可执行订单。由于该产品当前 QA 页面状态是 `DRAFT`，系统不会把草稿规程当作正式待检任务。
- 2026-08-09：`project-experience-consolidation` 已将“正式 QA owner 必须为 `MES_QA`，任务/夹具 owner 不得进入运行态”并入 `docs/backend-development.md#PQC 待检准入与工序选择必须分离`。
- 2026-08-09：`task-closeout-cleanup` preview/apply 均通过，删除仅属于本任务的一次性 SQL、Playwright 脚本/截图和中间证据，保留三份核心记录。

## Verification Evidence

- RED: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest#shouldRejectNonFormalQaOwnerFromActiveOrderProcessList" test` -> FAIL, expected reason: 修复前非正式 `CODX_QA` 规程未被拒绝，测试报 `Expected ServiceException to be thrown, but nothing was thrown`。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest#shouldRejectNonFormalQaOwnerFromActiveOrderProcessList" test` -> PASS。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest,MesQaInspectionRegulationServiceTest" test` -> PASS，47 tests，0 failures/errors/skipped。
- GREEN: 数据修复后查询 -> `CODX_QA/RETIRED=14`、`CANCELLED=112`、目标活跃订单 `PENDING=0`。
- GREEN: Playwright 1.60 真实登录并访问 `/mes/pro/feedback/edhr-batch-pqc-fill` -> PASS；目标订单在 API 响应和订单选择器中均不可见，无检验业务写入，页面无 JS 错误。
- 相关静态回归 `mes-pqc-task-generation-static.spec.cjs` 未通过，失败点是当前工作区 `MesTeamLeaderActiveOrderServiceImpl` 缺少 `SHIFT_AM="AM"`；该断言与本任务的 QA owner 筛选无关，本任务未修改该并行范围。

## Blockers

- 本任务无阻塞。工作区存在上述与本任务无关的静态断言失败，未跨范围修改。
