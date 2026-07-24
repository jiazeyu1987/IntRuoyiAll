# 执行日志：删除发布入口真实验证产品数据

- BDD: 删除发布入口验证产品 -> Given 本机 `芋道源码/admin` 租户 1 中存在中文名称匹配 `发布入口真实验证[0-9]+` 的产品主数据或展厅产品 / When 执行删除 / Then 这些产品主数据和展厅源产品均被逻辑删除，后续产品主数据列表与展厅映射预览不再出现这些中文名称。
- BDD: 非匹配产品不受影响 -> Given 产品主数据中存在正常产品和其他验证名称 / When 删除 `发布入口真实验证[0-9]+` / Then 仅匹配该正则的记录被删除，其他产品主数据和展厅产品保留。
- BDD: 删除前置条件缺失时失败 -> Given 数据库连接、租户、表结构或匹配范围不可确认 / When 准备删除 / Then 任务失败并记录阻塞，不执行部分删除。
- RED: 删除前数据库查询 `^发布入口真实验证[0-9]+$` -> FAIL，仍存在 `mdm_match_count=12`、`showroom_latest_match_count=12`、`all_matching_revision_count=113`；DCC 引用为 0，展柜引用为 0，展厅封面批处理附属项为 16。
- GREEN: 事务逻辑删除 -> PASS，更新 `mdm_product=12`、`showroom_product=12`、`showroom_product_revision=113`、`showroom_product_cover_batch_task_item=16`。
- GREEN: 删除后数据库核验 -> PASS，`mdm_publish_match_count=0`，`revision_publish_match_count=0`，`showroom_active_publish_revision_join_count=0`，`active_cover_task_items_for_deleted_products=0`，`mdm_total=166`，`showroom_total=166`。
- GREEN: 负向核验 -> PASS，`编辑入口真实验证[0-9]+` 未被删除，`negative_control_edit_mdm_count=2`，`negative_control_edit_revision_count=20`。
- GREEN: 删除后 API 核验 -> PASS，`GET /admin-api/mdm/product/page?pageNo=1&pageSize=10` 返回 `total=166` 且不含 `发布入口真实验证`；`keyword=发布入口真实验证` 返回 `total=0`；`GET /admin-api/showroom/product/mdm-mapping-preview` 返回 `totalCount=166`、`createCount=0`、`updateCount=166`、`linkedCount=0`、`failureCount=0`。
