# 执行日志：20260627-release-required-sql-showroom-product-current-bu-normalization-guard

- 2026-06-27 17:58:00 `BDD: 非业务探针记录不应阻塞 showroom product current BU 发布归一化 -> Given 当前 revision 中存在 tenant_id=0 且 BU 字段为 Null value probe 的探针记录 / When required SQL 执行 showroom product current BU normalization / Then 该记录应被明确排除在业务归一化与 unknown guard 之外，不得阻塞测试服发布。`
- 2026-06-27 17:58:00 `BDD: 真实业务未知 BU 仍必须 fail fast -> Given 当前 revision 中存在 tenant_id<>0 且 BU 字段为未识别非空值的业务记录 / When required SQL 执行 showroom product current BU normalization / Then unknown guard 仍必须保留并阻断发布，不得放宽为静默跳过。`
- 2026-06-27 17:58:00 `证据: 测试服失败 operation=op-2026-06-27T094707808570600Z-965ba79d-53d0-4848-8ee2-479dfc61b761；失败 SQL=20260626_showroom_product_current_bu_normalization.sql；错误=ERROR 1048 (23000) at line 75: Column 'must_be_empty' cannot be null。`
- 2026-06-27 17:58:00 `证据: 只读核对未知值仅 1 条，revision_id=4574, product_id=252, tenant_id=0, pipeline_layout=NULL, pipeline_layout_en='Null value probe'。`
- 2026-06-27 17:58:00 `状态: 已创建任务文档并进入 RED 阶段，待补充 SQL 契约测试。`
- RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_showroom_product_bu_normalization_sql.py -q` -> FAIL, 新增探针排除契约断言未命中，证明当前 SQL 尚未排除 tenant_id=0 / Null value probe。
- 2026-06-27 18:00:00 `RED: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_showroom_product_bu_normalization_sql.py -q -> FAIL, 新增探针排除契约断言未命中，证明当前 SQL 尚未排除 tenant_id=0 / Null value probe。`
- 2026-06-27 18:03:00 `修复: 在 20260626_showroom_product_current_bu_normalization.sql 的当前 revision 筛选条件中新增精确排除，仅排除 tenant_id=0、pipeline_layout 为空且 pipeline_layout_en='Null value probe' 的探针记录；未放宽真实业务 unknown guard。`
- GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_showroom_product_bu_normalization_sql.py -q` -> PASS
- GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql` -> PASS
- 2026-06-27 18:05:00 `GREEN: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_showroom_product_bu_normalization_sql.py -q -> PASS`
- 2026-06-27 18:05:00 `GREEN: python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql -> PASS`
