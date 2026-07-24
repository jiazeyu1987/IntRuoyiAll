# 20260611 生产工单一键创建金蝶生产订单

## 任务目标

在 MES 生产工单中新增“创建ERP订单”能力：用户在生产工单行操作点击按钮后，系统基于该 MES 工单向外部金蝶测试账套创建 `PRD_MO` 生产订单，保存后立即提交，不自动审核。

## 里程碑

1. M1 任务记录与现状审计：确认现有金蝶同步、生产工单、配置、权限和前端入口。
2. M2 RED：补后端 client/service/controller 测试、SQL 静态测试和前端静态契约测试，先得到失败。
3. M3 GREEN：实现金蝶 `Save` + `Submit`、本地资格校验、关联记录、配置字段、权限 SQL 和前端按钮。
4. M4 REGRESSION：运行后端目标测试、SQL 静态测试、前端静态/类型检查。
5. M5 E2E/阻塞记录：若本机服务和金蝶配置可用，使用测试租户真实写入验证；否则记录缺失前置与影响。
6. M6 收尾：运行 task-closeout-cleanup 预览，完成后只提交本任务改动。

## 预期验证

- `mvn -pl yudao-module-erp,yudao-module-mes -am "-Dtest=ErpKingdeeProductionOrderClientImplTest,MesKingdeeProductionOrderCreateServiceImplTest,MesProWorkOrderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -m pytest script/tests/test_kingdee_production_order_create_sql.py`
- `node tests/e2e/workorder-create-erp-order-static.spec.js`
- `pnpm ts:check`
- Playwright 真实路径：本机 `http://localhost:8081`、测试租户 `aoteman`、金蝶测试账套。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；金蝶配置、模板单号、工单资格、接口响应任一缺失均 fail fast。
- `是否从根因和长期维护角度解决`：是；新增正式金蝶写回 client/service/API/权限和配置，不做临时脚本或手工 SQL 绕过。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：COMPLETED。
- 已确认：目标为外部金蝶测试账套；点击后保存并提交；重复创建直接失败；模板单号通过 ERP 配置维护；按钮在生产工单行操作。
- 约束：不操作正式服务器，不写正式租户数据，不审核金蝶单据，不使用 mock 成功。
- 完成结果：2026-06-12 通过本机 `http://localhost:8081`、测试租户 `aoteman` 真实前端路径保存模板单号 `881MO090756`，并从生产工单行按钮创建金蝶生产订单；接口返回 `workOrderId=922953`、`erpFid=310120`、`erpBillNo=CODexERP20260610E`、`saved=true`、`submitted=true`。
- 最终核验：本地 `mes_kingdee_production_order_sync_record` 已写入同步记录；金蝶 `PRD_MO` 只读查询返回 FID `310120`、单号 `CODexERP20260610E`、单据状态 `B`。

## Cleanup Keep

- `doc/tasks/20260611-kingdee-production-order-create/backend-api-evidence.md`
