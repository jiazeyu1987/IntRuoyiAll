# 任务：生产工单补齐 ERP 截图字段（后端）

- Task ID: `20260630-erp-production-order-field-alignment`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

补齐 ERP 生产订单同步到本地 `mes_pro_work_order` 的截图字段，并让生产工单分页、详情、导出接口返回这些字段；更新已有工单时只覆盖 ERP 映射字段，不清理本地扩展字段。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - PowerShell 中文读写统一显式 UTF-8；不使用 `&&`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\integrations\kingdee-erp-official-docs.md`
  - 字段映射以当前 `PRD_MO` 正式链路和代码证据为准，未证实字段不得伪造。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 同步写入 ERP 截图字段并保留本地扩展 -> Given 本地工单已有排产属性和生产用料清单关联 / When ERP 同步返回车间/BOM版本/冲领料/图号/备注1助记码/排产状态 / Then 本地工单新增 ERP 字段被写入，现有本地扩展字段不被置空。`
- `BDD: 分页详情导出返回 ERP 新字段 -> Given 本地工单已落库 ERP 新字段 / When 查询分页、详情或导出 / Then 返回字段可用于前端展示。`
- `BDD: ERP 空字段保持真实空值 -> Given ERP 记录该字段为空 / When 同步并查询 / Then 对应返回值为空。`

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes,yudao-module-erp "-Dtest=MesKingdeeProductionOrderSyncServiceImplTest,MesProWorkOrderControllerTest,ErpKingdeeProductionOrderClientImplTest" -Dsurefire.failIfNoSpecifiedTests=false test`
- `python -X utf8 -m pytest script/tests/test_mes_work_order_erp_snapshot_fields_sql.py -q`

## Final Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes,yudao-module-erp "-Dtest=MesKingdeeProductionOrderSyncServiceImplTest,MesProWorkOrderControllerTest,ErpKingdeeProductionOrderClientImplTest" -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `docker exec -e MYSQL_PWD=123456 int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -D ruoyi-vue-pro -e "SHOW COLUMNS FROM mes_pro_work_order WHERE Field IN (...)"` -> PASS，本机开发库新增列已应用。
- `python -X utf8 -m pytest script/tests/test_mes_work_order_erp_snapshot_fields_sql.py -q` -> PASS
