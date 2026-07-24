# 20260611 生产工单一键创建金蝶生产订单执行日志

## BDD 场景

BDD: 生产工单创建并提交金蝶生产订单 -> Given 测试租户存在已确认、未冻结、自行生产的 MES 生产工单且 ERP 配置包含模板单号 / When 用户点击“创建ERP订单” / Then 系统在金蝶 `PRD_MO` 保存并提交同编码生产订单，返回 ERP FID 和单号，并写入本地关联记录。

BDD: 缺少模板单号时失败 -> Given ERP 配置缺少 `productionOrder.templateBillNo` / When 用户点击“创建ERP订单” / Then 系统拒绝调用金蝶写入并提示缺少模板单号。

BDD: 不合格生产工单失败 -> Given 生产工单不存在、未确认、非自行生产、已冻结、数量为空或日期为空 / When 用户点击“创建ERP订单” / Then 系统 fail fast，不调用金蝶写入。

BDD: 重复创建失败 -> Given 本地已有关联记录或金蝶已存在同编码 `PRD_MO` / When 用户再次点击“创建ERP订单” / Then 系统失败提示已存在，不更新金蝶也不写默认成功。

BDD: 保存成功提交失败不写本地关联 -> Given 金蝶 Save 成功但 Submit 失败 / When 用户点击“创建ERP订单” / Then 系统抛出真实提交错误，不写入 `mes_kingdee_production_order_sync_record`。

## 执行证据

- 2026-06-11：读取前后端工作区状态，发现已有非本任务未提交改动，本任务只做增量修改，不回退用户改动。
- 2026-06-11：补齐上一 eDHR 审批上下文任务缺失的 `task.md`，按 evidence 阻塞记录标为 BLOCKED。
- RED: `mvn -pl yudao-module-erp,yudao-module-mes -am "-Dtest=ErpKingdeeProductionOrderClientImplTest,MesKingdeeProductionOrderCreateServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，缺少 `ErpKingdeeProductionOrderCreateRequest` 等生产订单创建接口类型。
- RED: `python -m pytest script/tests/test_kingdee_production_order_create_sql.py` -> FAIL，缺少 `sql/mysql/20260611_mes_work_order_create_erp_order.sql` 权限与唯一约束脚本。
- RED: `.\restart-ruoyi-backend.bat` -> FAIL，后端 jar 构建在 `yudao-module-erp` 测试编译阶段失败，`ErpKingdeeProductionOrderClientImplTest.java:[192,20]` 找不到 `ErpKingdeeProductionOrderCreateRequest`。
- RED: `mvn -pl yudao-module-erp -DskipTests test-compile` -> FAIL，`ErpKingdeeProductionOrderClientImpl` 未覆盖 `createAndSubmitProductionOrder(ErpKingdeeProperties, ErpKingdeeProductionOrderCreateRequest)`。
- GREEN: `mvn -pl yudao-module-erp "-Dtest=ErpKingdeeProductionOrderClientImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，4 个 Kingdee `PRD_MO` 客户端用例通过。
- GREEN: `.\restart-ruoyi-backend.bat` -> PASS，构建后端 jar 并启动本机后端。
- GREEN: `Invoke-RestMethod http://localhost:48081/actuator/health` -> PASS，返回 `{"status":"UP"}`。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence ruoyi-vue-pro\doc\tasks\20260611-kingdee-production-order-create\backend-api-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260611-kingdee-production-order-create --mode preview` -> PASS，后端 `keep` 包含 task、execution-log、backend-api-evidence，`delete/blocked/warnings` 均为 `<none>`。
- RED: Playwright 真实行按钮创建 `workOrderId=922953` -> FAIL，后端返回 `PRD_MO View template failed: 表单标识为空，请确认接口参数中是否已给单据标识赋值。`，原因是 `View/Save/Submit` 未按金蝶动态表单协议传 `formid=PRD_MO`。
- GREEN: `mvn -pl yudao-module-erp,yudao-module-mes -am -DskipTests compile` -> PASS，后端目标模块编译通过。
- GREEN: `mvn -pl yudao-module-erp "-Dtest=ErpKingdeeProductionOrderClientImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，5 个金蝶生产订单 client 测试通过，覆盖查重、模板读取、Save、Submit 和失败响应。
- GREEN: `mvn -pl yudao-module-erp,yudao-module-mes -am "-Dtest=ErpKingdeeProductionOrderClientImplTest,MesKingdeeProductionOrderCreateServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，ERP 5 个、MES 5 个目标测试通过。
- GREEN: `python -m pytest script/tests/test_kingdee_production_order_create_sql.py` -> PASS，3 个 SQL 静态检查通过，覆盖权限菜单、租户套餐/角色绑定、唯一约束和无危险 SQL。
- GREEN: `docker exec int-ruoyi-mysql mysql ... < /tmp/20260611_mes_work_order_create_erp_order.sql` -> PASS，本机测试库写入按钮权限、测试租户角色绑定和唯一索引。
- GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> PASS，后端重启后 Tomcat 监听 `48081` 且健康检查为 UP。
- GREEN: Playwright 配置页真实路径 `/erp/kingdee-config` -> PASS，测试租户 `aoteman` 在“生产同步”页签填写 `productionOrder.templateBillNo=881MO090756` 并保存，`/erp/kingdee-config/save` 返回 `code=0`。
- GREEN: Playwright 生产工单行按钮真实路径 -> PASS，点击“创建ERP订单”并确认后返回 `workOrderId=922953`、`erpFid=310120`、`erpBillNo=CODexERP20260610E`、`saved=true`、`submitted=true`。
- GREEN: 本地 MySQL 只读核验 -> PASS，`mes_kingdee_production_order_sync_record` 存在 `tenant_id=122`、`work_order_id=922953`、`source_fid=310120`、`source_bill_no=CODexERP20260610E`。
- GREEN: Kingdee `ExecuteBillQuery` 只读核验 -> PASS，`PRD_MO` 返回 FID `310120`、单号 `CODexERP20260610E`、单据状态 `B`、物料 `YXN.037.011.1007`、数量 `128`、单位 `zhi`。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260611-kingdee-production-order-create\backend-api-evidence.md` -> PASS，后端 API evidence 有效。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260611-kingdee-production-order-create --mode preview` -> PASS，后端任务目录 `delete/blocked/warnings` 均为 `<none>`。
