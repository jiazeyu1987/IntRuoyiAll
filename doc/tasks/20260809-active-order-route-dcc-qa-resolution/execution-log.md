# Execution Log

## User Intent

- 用户确认按“工艺路线绑定 DCC 项目代码”的方向修改并验证。
- 工单物料编号必须正式对应工艺路线；由路线找到 DCC 项目代码和精确 QA，禁止按名称猜测。

## BDD

- BDD: 同名物料通过正式路线与 DCC 项目解析 QA -> Given 生产工单物料与唯一 ACTIVE 路线版本正式绑定，且路线绑定唯一 DCC 项目、该路线版本存在已发布 QA；When 查询可加入活跃订单；Then 返回可加入候选并使用该精确路线版本，不要求 QA 直接绑定工单的 MES 产品 ID。
- BDD: 已废止路线规程不造成版本歧义 -> Given 同一产品上下文的旧路线版本已 SUPERSEDED 且其 QA 仍为 PUBLISHED，同时新路线版本 ACTIVE；When 查询候选；Then 只使用 ACTIVE 路线版本 QA。
- BDD: 正式路线或 DCC 项目上下文不唯一时阻塞 -> Given 工单物料无法唯一解析 ACTIVE 路线版本或路线缺少唯一 DCC 项目绑定；When 查询候选；Then 返回明确不可加入原因，不按名称或默认值选择。
- BDD: 已取消工单先行排除 -> Given 生产工单状态为已取消；When 查询候选；Then 不进入 QA 解析且不能加入活跃订单。

## Milestone Evidence

- M1：完成。租户 1 真实数据确认路线 `922119` 的唯一 ACTIVE 版本为 `627`，路线产品中代码 `ID` 精确匹配启用 DCC 项目 `147`（`product_master_id=11`）；目标四个未加入工单均通过正式产品绑定落到该路线。
- M2：完成。已先记录四个 BDD 场景并获得旧构造器缺少 DCC 正式解析依赖的 RED。
- M3：完成。候选与加入链路已统一为工单物料 -> 唯一路线 -> 唯一 ACTIVE 路线版本 -> 唯一 DCC 项目代码 -> 精确路线版本 QA；取消工单先行阻断。
- M4：完成。当前 `int_main` 运行 Jar 已包含正式路线/DCC/QA 解析类，后端 health 为 `UP`、前端 HTTP 200；Playwright 通过真实“新增活跃订单”弹窗逐单搜索完成只读验收。
- M5：完成。`task-closeout-cleanup` preview 返回 `status: ready`、无 blocked/warnings；apply 返回 `status: applied`，任务目录仅保留 `task.md`、`execution-log.md`、`verification-report.md`。另精确删除本任务生成的 7 个根目录 Playwright 快照/控制台临时文件，复核剩余 0。

## RED / GREEN / REGRESSION

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，测试要求注入 `DccProjectCodeMapper`，旧服务构造器不存在该正式路线项目解析依赖；MES testCompile 明确失败。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldIgnoreDeletedRouteBindingWhenOneFormalRouteStillExists" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，孤儿路线绑定仍参与唯一性判断，期望 `eligible=true`、实际为 `false`。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderActiveOrderErpPlannedStartTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，28 tests，0 failures/errors/skips，`BUILD SUCCESS`。
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderActiveOrderErpPlannedStartTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，45 tests，0 failures/errors/skips，`BUILD SUCCESS`。
- REGRESSION: `node --test src/test/js/mes-pqc-task-generation-static.spec.cjs` -> FAIL，既有静态合同要求 `SHIFT_AM="AM"`，但 `HEAD` 中服务原本即为 `SHIFT_FIRST="FIRST"`；与本次路线/DCC/QA 修改无关，本任务未修改该测试或班次逻辑。
- DATA: 对租户 1 目标工单执行只读关联查询 -> `881MO090935/090972/090973/090974` 均为状态 1、唯一路线版本 `922119/627`、唯一 DCC 上下文 `ID#11`、已发布 `MES_QA` 数量 1、现有活跃订单数量 0；`881MO090889` 同链路但现有活跃订单数量 1；`881MO100066/100524` 状态均为 3（已取消）。
- RUNTIME: `48081` PID `52880`，运行 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260809-140430.jar`，Jar SHA256 `538EBF0502CE0CE76B179B8A576AFD1BB7EBE30C9CC6BF543D4CD854ED8C2ED6`，health `UP`；内嵌 MES 服务类包含 `DccProjectCodeMapper`、取消工单、已删除路线和 DCC 绑定歧义四项正式判断，伴随类共 12 个。
- E2E: Playwright 通过 `http://127.0.0.1:8081/mes/pro/process-pool/team-leader` 登录并打开“新增活跃订单”弹窗；`881MO090935/090972/090973/090974` 均显示“符合要求”，`881MO100066/100524` 均显示“生产工单已取消”，`881MO090889` 显示“符合要求”但数据库确认已有活跃订单，新增服务按原幂等契约返回既有记录；未点击“加入活跃订单”，业务写请求为 0。
- UI EVIDENCE: 截图 `active-order-candidate-881MO090935.png` SHA256 `D1907BC9D2D374EB373C606AA87D160AD1AC2B821AD8E7C808D5E9DA1262166F`，页面显示 `881MO090935 / 符合要求`；本次干净浏览会话控制台 error 0，前端入口 HTTP 200。

## Blockers

- 本任务无阻塞项。
- 非目标基线：`mes-pqc-task-generation-static.spec.cjs` 仍因既有 `SHIFT_AM`/`SHIFT_FIRST` 合同不一致失败，本任务未修改该逻辑，且不影响本次路线/DCC/QA 验收结论。
