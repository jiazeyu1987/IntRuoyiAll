# Execution Log: MES 生产工单测试用随机 ERP 建单

## BDD 场景

BDD: 测试建单复制主体但随机编码和数量 -> Given 用户对可建 ERP 的生产工单点击测试建单 / When 后端构建金蝶创建请求 / Then billNo 必须随机生成且不等于原工单编码，quantity 必须是 10~1000 的整数，其他主体字段继续复用当前工单。

BDD: 同一工单允许重复触发测试建单 -> Given 同一条工单已成功创建过测试 ERP 订单 / When 用户再次发起测试建单 / Then 后端不得因已有 `workOrderId` 同步记录直接阻断。

BDD: 随机编码可用于后续回同步新增 -> Given 测试 ERP 订单的 billNo 与基础工单编码不同 / When 用户后续执行同步金蝶 / Then 该订单可按新编码走新增路径，而不是覆盖原工单。

## 执行证据

- 2026-06-23：创建任务，命中 `docs/integrations/kingdee-erp-official-docs.md`，本轮只做本机代码与测试，不执行服务器/真实 E2E/租户写入。
- RED: `mvn -pl yudao-module-mes -Dtest=MesKingdeeProductionOrderCreateServiceImplTest test` -> FAIL，新增测试已要求 `generateTestBillNo/generateTestQuantity` 和“禁止本地关联阻断/落同步记录”的新语义，但生产代码仍是原工单直推；同时模块 `target` 里存在截断 class 文件，需要后续 `clean` 重新编译。
- GREEN: `mvn -pl yudao-module-mes clean -Dtest=MesKingdeeProductionOrderCreateServiceImplTest test` -> PASS，5 个定向用例通过，已验证随机编码、随机数量、同基础工单可重复测试建单、重复编码 fail-fast 和失败不写本地关联。
- GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，后端整包编译与装配链路通过。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence ruoyi-vue-pro\doc\tasks\20260623-test-randomized-erp-order-create\backend-api-evidence.md` -> PASS，后台证据文档格式通过。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260623-test-randomized-erp-order-create --mode preview` -> PASS，预览仅建议删除中间证据文档。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260623-test-randomized-erp-order-create --mode apply` -> PASS，已删除 `backend-api-evidence.md`，保留 `task.md` 与 `execution-log.md`。
