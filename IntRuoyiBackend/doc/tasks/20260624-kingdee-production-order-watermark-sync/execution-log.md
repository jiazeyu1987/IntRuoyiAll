# 执行日志：金蝶生产工单水位线同步

## BDD

- BDD: 首次同步到点击当天 -> Given 当前生产订单同步没有成功水位线 When 用户点击“同步金蝶” Then 系统按生产订单业务日期从初始窗口同步到点击当天，不同步点击当天之后的业务日期订单。
- BDD: 后续同步按 ERP 修改时间增量 -> Given 已存在生产订单同步成功水位线 When 用户点击“同步金蝶” Then 系统按 ERP 修改时间从上次成功同步时间到本次点击时间拉取订单，即使订单业务日期在未来也可以同步新增。
- BDD: 手动按钮与定时任务一致 -> Given 用户手动点击“同步金蝶”或定时任务触发 When 同步开始 Then 二者都通过金蝶同步运行记录和水位线计算窗口。

## TDD 证据

- RED: `mvn -pl yudao-module-mes "-Dtest=MesKingdeeProductionOrderSyncServiceImplTest,KingdeeProductionOrderSyncJobTest,MesProWorkOrderControllerTest" test` -> FAIL，MES 单模块使用旧 ERP 依赖，编译期找不到新增 `initialWindowStart` / `initialSync` 上下文字段。
- GREEN: `mvn -pl yudao-module-erp -Dtest=ErpKingdeeSyncRuntimeServiceImplTest test` -> PASS，6 个测试通过。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesKingdeeProductionOrderSyncServiceImplTest,KingdeeProductionOrderSyncJobTest,MesProWorkOrderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，18 个 MES 相关测试通过。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260624-kingdee-production-order-watermark-sync --mode preview` -> PASS，无删除项、无阻塞。

## 里程碑记录

- M1：完成。新增同步窗口 BDD 和单元测试覆盖。
- M2：完成。同步运行上下文支持首次同步标识和初始窗口。
- M3：完成。按钮与 Job 均走运行时水位线，生产工单服务根据上下文选择业务日期或修改时间窗口。
- M4：完成。目标测试通过，无高风险服务器写入动作。
