# 执行日志：展厅产品一键翻译并发布

## 2026-06-26

- `BDD: 当前筛选产品一键翻译并发布 -> Given 当前筛选命中多个产品 / When 点击一键翻译 / Then 后端逐个翻译、保存、发布并在前端展示进度`
- `BDD: 关键词翻译优先 -> Given 产品中文含有“上海翰凌医疗器械有限公司”“翰凌”“心血管BU” / When 批量翻译 / Then 英文按关键词表输出而不是音译或传统直译`
- `BDD: 已有英文被重译覆盖 -> Given 产品已有英文名称和英文字段 / When 一键翻译执行成功 / Then 新发布版本使用本次 AI 翻译结果覆盖旧英文`
- `BDD: 单品失败不中断整体任务 -> Given 某个产品缺少发布必填字段或 AI 翻译失败 / When 批量任务执行 / Then 记录该产品失败原因并继续处理后续产品`
- `BDD: 同租户任务互斥 -> Given 当前租户已有运行中一键翻译任务 / When 再次启动 / Then 后端明确报错，不创建第二个任务`
- `RED: python -X utf8 -m pytest script/tests/test_showroom_translate_publish_sql.py -q -> FAIL, 预期原因：新增一键翻译发布任务表 migration 与 showroom baseline 均不存在。`
- `RED: mvn -pl yudao-module-showroom "-Dtest=ShowroomProductTranslatePublishBatchIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, 预期原因：后端 controller/runtime 尚未暴露批量翻译发布接口。`
- RED: backend/sql contract -> FAIL, 新增一键翻译发布任务表 migration、showroom baseline 与后端接口缺失。
- `GREEN: python -X utf8 -m pytest script/tests/test_showroom_translate_publish_sql.py -q -> PASS`
- `GREEN: mvn -pl yudao-module-showroom "-Dtest=ShowroomProductTranslatePublishBatchIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS`
- GREEN: backend/sql contract -> PASS, 后端接口测试与 SQL 合同测试通过。
- `GREEN: local-schema -> PASS, 本机 MySQL ruoyi-vue-pro 已应用 sql/mysql/20260626_showroom_product_translate_publish_batch_task.sql，SHOW TABLES/DESCRIBE 均通过。`
- `GREEN: real-e2e-db-readback -> PASS, showroom_product_translate_publish_batch_task id=1 COMPLETED；item product_id=442 COMPLETED published_revision_id=5183；showroom_product.current_revision_id=5183，revision_no=10，status=PUBLISHED。`
