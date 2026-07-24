# 执行日志：展柜画布布局编辑后端持久化

- BDD: 保存展柜产品布局坐标 -> Given 展柜有真实产品映射 / When 调用保存映射接口并携带 `layoutX/layoutY/layoutWidth/layoutHeight` / Then 后端持久化坐标并按 displayOrder 返回。
- BDD: 缺少布局坐标失败 -> Given 调用保存映射接口 / When 任一产品映射缺少坐标或坐标越界 / Then 后端 fail-fast 返回错误，不保存部分数据。
- BDD: 旧映射读取生成默认布局 -> Given 历史映射没有坐标 / When 查询展柜列表 / Then 返回按产品数量平均生成的布局坐标。

- RED: `mvn.cmd -pl yudao-module-showroom "-Dtest=ShowroomHallContentTest,ShowroomPersistentContentServiceTest" test` -> FAIL，保存映射请求/返回模型缺少 `layoutX/layoutY/layoutWidth/layoutHeight`，历史映射读取无法返回画布坐标。
- RED: `python -X utf8 -m pytest script/tests/test_showroom_sql_scripts.py` -> FAIL，迁移脚本缺少 `showroom_hall_product` 布局列契约；早期 `ADD COLUMN IF NOT EXISTS` 方案与本地 MySQL 8.0.39 不兼容。
- GREEN: `python -X utf8 -m pytest script/tests/test_showroom_sql_scripts.py` -> PASS，12 个 SQL 契约用例通过；布局迁移改为 `information_schema.COLUMNS` + dynamic `PREPARE/EXECUTE` 幂等模式。
- GREEN: `mvn.cmd -pl yudao-module-showroom "-Dtest=ShowroomHallContentTest,ShowroomPersistentContentServiceTest" test` -> PASS，14 个展厅目标单测通过，覆盖坐标保存、缺坐标失败、历史空坐标默认布局和 10/23 产品默认布局无重叠。
- GREEN: 测试租户真实 E2E 保存后认证读回 `/admin-api/showroom/hall/page?pageNo=1&pageSize=20` -> PASS，`tenantId=122`、`hallCode=hall_05`、`mappingCount=10`，首三个坐标为 `(0,0,0.28681,0.333333)`、`(0.28681,0,0.21319,0.333333)`、`(0.5,0,0.25,0.333333)`。
