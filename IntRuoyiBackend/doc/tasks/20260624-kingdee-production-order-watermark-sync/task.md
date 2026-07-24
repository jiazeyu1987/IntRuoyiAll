# 20260624 金蝶生产工单水位线同步

## 任务目标

修正生产工单“同步金蝶”逻辑：首次同步按生产订单业务日期同步到点击当天；后续同步按上次成功同步后的 ERP 创建/修改时间窗口同步，避免未来业务日期但已创建的生产订单被漏掉。

## 经验门禁

- ERP / 金蝶 / OpenAPI：已阅读 `docs/integrations/kingdee-erp-official-docs.md`，金蝶对接不得在接口不可用时静默降级为 mock、默认成功或非官方数据源；增量字段、字段映射和权限必须以真实接口能力为准。
- 高风险动作：本轮仅做本机代码与单元测试，不执行服务器写入、发布、真实 E2E、数据库 schema 变更或远端联调；若后续发布/联调，必须先在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。统一手动按钮、定时任务和同步服务的窗口语义，使用已有金蝶同步运行记录/水位线框架。
- 是否存在临时补丁或绕过：否。

## 里程碑

- M1：用 BDD/TDD 固化首次同步与增量同步口径。
- M2：改造同步运行上下文，支持首次同步标识与初始窗口。
- M3：改造生产工单同步服务、定时任务和手动按钮，统一走运行时水位线。
- M4：运行目标单元测试并记录结果。

## 预期验证

- `mvn -pl yudao-module-erp -Dtest=ErpKingdeeSyncRuntimeServiceImplTest test`
- `mvn -pl yudao-module-mes -Dtest=MesKingdeeProductionOrderSyncServiceImplTest,KingdeeProductionOrderSyncJobTest,MesProWorkOrderControllerTest test`

## 当前状态

- 已完成。

## 完成记录

- M1：完成。已用 BDD 固化首次同步、增量同步、手动按钮与定时任务一致性。
- M2：完成。金蝶同步运行上下文支持 `initialSync` 和 `initialWindowStart`。
- M3：完成。生产工单同步服务支持首次按业务日期窗口、后续按 ERP 修改时间窗口；手动按钮和定时任务统一走运行时水位线。
- M4：完成。目标单元测试已通过。

## 最终验证

- `mvn -pl yudao-module-erp -Dtest=ErpKingdeeSyncRuntimeServiceImplTest test` -> PASS。
- `mvn -pl yudao-module-mes -am "-Dtest=MesKingdeeProductionOrderSyncServiceImplTest,KingdeeProductionOrderSyncJobTest,MesProWorkOrderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
