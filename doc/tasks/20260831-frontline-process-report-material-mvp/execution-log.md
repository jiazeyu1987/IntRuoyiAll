# Execution Log

## User Intent

- 用户要求在合规 worktree 中实现已确认文档，完成开发验证后先提交任务代码，再以合适方式融合进 `int_main`。
- MVP 口径：复用现有产品 BOM 按工序配置物料；物料页签只有灰色和绿色；完成数量被明确填写后绿色；不使用其它字段参与颜色判断；所有物料一次正式提交；进度取完成数量最小值；批号只读系统每日同步表。

## Applicable Skills And Gates

- Behavior-Driven Development：先把用户行为固化为 Given/When/Then，再进入严格 RED/GREEN。
- Database Schema Delivery：多物料持久化结构必须有迁移、回滚和迁移测试，且不得复制系统同步批号。
- Backend API Delivery：正式提交和系统同步批号读取必须使用明确契约、权限、失败行为和同一事务边界。
- Frontend Feature Delivery：物料页签、独立草稿、状态和 API 集成必须以可观察行为和真实页面验证为准。
- Playwright：最终通过真实一线生产页面验证，不使用 API-only 冒充 E2E。
- 项目门禁：冻结路线版本、一次正式提交、无默认物料、配置链路独立、严格 UTF-8、worktree 端口治理和 `int_main` 脏工作区融合交集检查。

## BDD Scenarios

- BDD: 按冻结工序物料生成页签 -> Given 活跃订单冻结路线版本的粗洗工序产品 BOM 包含弹簧和杠杆 / When 一线人员进入粗洗工序 / Then 页面显示两个物料页签且不读取当前最新路线或其它配置链路。
- BDD: 完成数量控制灰绿状态 -> Given 两个物料完成数量均为空 / When 用户填写弹簧完成数量 / Then 只有弹簧页签变绿；填写 `0` 也变绿，清空后恢复灰色，损耗和参数不影响颜色。
- BDD: 物料草稿相互隔离 -> Given 用户分别填写弹簧和杠杆的数量、损耗和参数 / When 在两个页签间往返切换 / Then 每个物料恢复自己的原值且互不覆盖。
- BDD: 一次正式提交全部物料 -> Given 当前工序全部配置物料均已明确填写完成数量 / When 用户完成签名并点击一次正式提交 / Then 只发送一次请求并在同一事务形成全部物料明细。
- BDD: 缺完成数量拒绝整单 -> Given 至少一个配置物料完成数量为空 / When 用户正式提交 / Then 页面指出目标物料且后端不形成任何部分正式事实。
- BDD: 最小值形成工序进度 -> Given 弹簧完成数量 5、杠杆完成数量 3 / When 一次正式提交 / Then 主报工与订单工序进度数量为 3，逐物料事实仍保留 5 和 3。
- BDD: 系统同步批号只读展示 -> Given 每日同步领料单明细存在当前生产订单与物料的非空批号 / When 读取报工物料详情 / Then 返回全部去重批号；系统内没有时返回空集合，不直连 ERP 或读取库存表。
- BDD: 路线升级不改变旧订单 -> Given 订单已冻结旧路线的弹簧和杠杆配置 / When 新路线修改工序 BOM / Then 旧订单仍显示冻结物料，新订单才使用新配置。

## Command Intent

- 先只读核对当前分支、worktree、正式数据模型、迁移机制、接口和测试入口。
- 后续每个生产行为先运行预期失败的聚焦测试并记录 RED，再做最小实现和 GREEN。
- 启动服务前预留 worktree runtime slot；提交和融合前运行 branch runtime port guard。

## TDD Evidence

- RED: `MesProFeedbackMaterialBatchQueryServiceTest` -> FAIL，预期原因：系统同步批号查询服务尚不存在。

- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesFrontlineProcessMaterialServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期原因：新增行为测试引用的 `MesFrontlineProcessMaterialService` 尚不存在；前置 23 个 Reactor 模块均成功，MES 在 testCompile 精确失败。
- GREEN: 同一命令 -> PASS，`MesFrontlineProcessMaterialServiceTest` 3/3 通过，覆盖冻结版本精确筛选、缺配置失败和非正用料比例失败。
- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期原因：运行态构造器、运行态记录和会话快照尚未携带 `materials`。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesFrontlineRuntimeConfigControllerTest,MesFrontlineSessionSnapshotServiceTest,MesFrontlineSubmitAuthorizationTest,MesFrontlineSubmitIdentityTraceTest,MesFrontlineEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，6 个测试类共 30 项通过。
- RED: `node tests/e2e/frontline-production-material-tabs-static.spec.cjs` -> FAIL，预期原因：前端运行态类型和一线页面尚无冻结物料页签、灰绿状态与分物料草稿。
- GREEN: 同一命令 -> PASS，页签来源、两色状态、`0`、清空和草稿切换合同通过。
- RED: `python -X utf8 -m pytest script/tests/test_mes_frontline_feedback_material_sql.py -q` -> FAIL，2 项均因 `20260831_mes_frontline_feedback_material.sql` 尚不存在而失败。
- GREEN: 同一命令 -> PASS，2/2 通过；迁移仅新增物料事实表，不改写既有报工。
- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProFeedbackMaterialServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期原因：物料事实 DO、Mapper、创建命令和服务尚不存在。
- GREEN: 同一命令 -> PASS，3/3 通过；覆盖两物料批量保存、重复物料拒绝和损耗大于完成数量拒绝。
- GREEN: `pnpm.cmd install --frozen-lockfile` -> PASS，锁文件未修改；依赖安装提示已有构建脚本批准策略，未执行未授权脚本。
- GREEN: `pnpm.cmd ts:check` -> PASS。
- GREEN: `node tests/e2e/frontline-formal-submit-static.spec.cjs`、`node tests/e2e/frontline-production-no-device-empty-state-static.spec.cjs` -> PASS。
- BASELINE: `node tests/e2e/edhr-frontline-production-prototype-parity-static.spec.cjs`、`node tests/e2e/edhr-frontline-production-pixel-parity-static.spec.cjs` -> FAIL，均要求旧的 `grid-template-rows: 130px 1fr 126px`；`git show HEAD:...FrontlineFixedTemplatePanel.vue` 证明当前 `int_main` 基线已是 `minmax(130px, auto) minmax(0, 1fr)`，本任务未修改该屏幕根布局，记录为既有相邻合同漂移，不作为本功能 GREEN。
- GREEN: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> PASS，`migrationCount=548`，包含 `20260831_mes_frontline_feedback_material` 及完整依赖闭包。
- CHANGE-RED: 更新迁移合同禁止本地 ERP 回填字段后 -> FAIL，现有未提交迁移仍含 `erp_batch_code` 等字段。
- CHANGE-GREEN: 更新迁移仅保留一线报工事实后，SQL pytest 2/2 PASS。
- RED: `MesProFrontlineFeedbackMaterialSubmissionValidatorTest` -> FAIL，预期原因：逐物料集合与最小值计算服务尚不存在。
- GREEN: 同一测试 -> PASS，3/3；覆盖物料集合一致、完成数量最小值、明确 `0` 和损耗明细守恒。
- RED: 正式提交服务测试加入两物料 `5/3`、缺物料、全 `0` 场景后 -> FAIL，预期原因：正式事务尚未保存逐物料事实，也未按最小值推进。
- GREEN: 正式提交、回滚、详情、路由顺序、原始载荷与闭环合同整组通过。
- RED: 主报工数量守恒合同要求“合格数量=最小进度、总报工=最小进度+逐物料损耗合计”后 -> FAIL，旧实现把总报工直接写成最小进度。
- GREEN: 正式提交聚合修正后，`MesProFrontlineFeedbackSubmitServiceTest`、`MesP0ProductionSubmitClosedLoopContractTest` 和原有 splitter 合同通过。
- GREEN: 后端最终聚焦回归 -> PASS，17 个测试类共 70 项，0 失败。
- GREEN: 前端 `eslint`、`pnpm ts:check`、物料页签/正式提交/损耗身份/订单归属静态合同全部 PASS。
- REGRESSION: 扫描 96 个 frontline 静态合同并与任务基线提交归档对比；当前失败 30、基线失败 54，`CURRENT_ONLY` 为空，本任务未新增相邻静态失败。
- GREEN: 真实 Playwright -> PASS；测试租户任务自有订单显示杠杆/弹簧两页签，初始灰色，填写 `5/3` 后绿色，弹簧显示系统同步批号、杠杆为空，一次提交请求含两条物料，进度为 `3`，目标页面/API 错误为 0。
- GREEN: E2E 数据清理 -> `CLEAN`，`remainingTaskDataCount=0`；本次临时物料、同步批号、账号、角色、订单、报工与工序池事件均已移除。

## Milestone Updates

- 2026-08-31：从本地 `int_main@58479242435efa1f7eafd6e0a17e36bd9c811e5f` 创建 worktree 与分支，目标路径位于 `D:\IntRuoyiWorktree\`，主工作区既有脏改动未复制、未修改。
- 2026-08-31：将已确认 PRD、用户流程和验收标准复制为实施任务基线。
- 2026-08-31：现状核对确认运行态只返回一套提交上下文，前端只有一套数量/损耗/设备参数草稿，正式请求和 `mes_pro_feedback` 只表达一条物料，工序池初始分配强制接收一个大于零的 `outputQuantity`。
- 2026-08-31：确认冻结路线版本 `routeSnapshotJson.configSnapshots.productBoms` 已保存产品、工序、BOM 物料和用料比例，可作为报工物料唯一来源；运行态当前尚未读取该集合。
- 2026-08-31：确认系统同步生产领料单明细已包含生产订单号、物料编码和批号，可作为唯一批号读取来源；库存表和产品入库批号不能替代。
- 2026-08-31：形成 `design.md`、`development-plan.md` 和 `test-plan.md`，将最小值进度与系统同步批号读取列为生产代码实施前的明确决定。
- 2026-08-31：用户解除阻塞并明确“数量不一致取最小值；不直连 ERP，只从系统每日同步表读取，系统没有就没有”。变更请求记录已通过 change-request validator。
- 2026-08-31：只读核对确认 `erp_kingdee_production_pick_list_item` 是当前唯一同时具备 `production_order_no + material_number + lot_number` 的系统同步正式表；库存表和库存移动表缺生产订单身份，不可替代。
- 2026-08-31：完成数据库、后端、前端和真实浏览器闭环；本地迁移表为空基线执行成功，真实提交保存两条物料事实并按 `5/3 -> 3` 推进，随后清理任务数据为 0。
- 2026-08-31：按 `project-experience-consolidation` 将多物料父子事实/数量守恒经验合并到 `docs/backend-development.md`，将 `vue-tsc` 与 Vite parser 差异补入既有 Vue SFC 泛型箭头门禁，并更新 `docs/experience-index.md` 路由。
- 2026-08-31：停止本任务 worktree 前后端，端口 `8311/48311` 均无监听；任务状态转为 `ready_for_closeout`。
- 2026-08-31：`task-closeout-cleanup` preview/apply 保留正式任务文档并删除 E2E、迁移门禁和基线临时产物；一个深层基线副本因 Python/Windows 长路径删除失败，核对目标仍在任务目录后使用 `\\?\` 扩展路径逐文件、逐空目录删除，最终 `Test-Path=False`。

## Blockers

- 当前无业务口径阻塞。原 blocked audit 已由用户明确决定解除，恢复后续 RED/GREEN。

## Completed Implementation

- 冻结路线产品 BOM 精确解析，并明确阻塞缺配置、重复物料、非正用料比例和主档缺失。
- 同一冻结物料集合进入运行态响应和签名保护的服务端会话快照。
- 一线页面物料页签、灰绿状态、`0`、清空、批号只读展示和分物料草稿隔离。
- 新增物料事实表、批量持久化服务和同一正式提交事务。
- 工序进度取全部物料完成数量最小值；主报工数量保持合格、不合格与总量守恒。
- 批号按生产订单号和物料编码只读系统同步领料单，空数据保持空集合。
