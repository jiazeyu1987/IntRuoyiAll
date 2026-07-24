# 排产测试租户数据底座补齐执行日志

- BDD: 测试租户具备排产订单物料前置数据 -> Given admin 租户存在生产工单和工单 BOM / When 脚本按自然键平移代表性样本到测试租户 / Then 测试租户存在可用于排产校验的生产工单 BOM，且 admin 数据不被修改。
- BDD: 测试租户具备报工归属前置样本 -> Given admin 租户存在外部 MES 报工导入记录和正式报工样本 / When 脚本平移必要样本到测试租户 / Then 测试租户出现报工导入样本和正式报工样本，后续可改造为待归属流程验证数据。
- BDD: 自然键映射失败时快速失败 -> Given admin 源数据引用的产品、工位、工序、任务或工单在测试租户找不到匹配自然键 / When 执行补数脚本 / Then 脚本停止并输出缺失映射，不写入半成品数据。
- RED: `python -X utf8 doc\tasks\20260610-scheduling-test-tenant-data-baseline\scripts\scheduling_test_tenant_baseline.py --mode check` -> FAIL，预期原因：测试租户产品路线关系 `4/15`，`WS-B040` 人工绑定 `0`，目标样本工单 BOM `0`，baseline 报工与导入记录均为 `0`。
- GREEN: `python -X utf8 doc\tasks\20260610-scheduling-test-tenant-data-baseline\scripts\scheduling_test_tenant_baseline.py --mode apply` -> PASS，事务内补齐测试租户数据：新增物料/产品 `35` 条、产品路线关系 `11` 条、工位人工绑定 `1` 条、生产工单 `1` 条、工单 BOM `54` 条、正式报工 `3` 条、报工导入记录 `3` 条。
- GREEN: `python -X utf8 doc\tasks\20260610-scheduling-test-tenant-data-baseline\scripts\scheduling_test_tenant_baseline.py --mode check` -> PASS，测试租户产品路线关系 `15/15`、`WS-B040` 人工绑定 `1`、样本工单 `2`、样本工单 BOM `54`、baseline 报工和导入记录均为 `3`。
- GREEN: 只读明细校验 -> PASS，admin 租户关键计数保持 `route_product=15`、`work_order=1034`、`work_order_bom=58`、`feedback=123`、`feedback_import_record=123`；测试租户样本工单 `881MO090863`、`881MO090880` 各有 `27` 条 BOM，报工样本 `FB-SCHED-BL-001..003` 关联 `ROUTE-XLSX-00001` 的 `B010/B030/PROC-XLSX-00001`。
