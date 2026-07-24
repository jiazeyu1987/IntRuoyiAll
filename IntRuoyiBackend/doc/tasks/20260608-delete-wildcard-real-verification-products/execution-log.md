# 执行日志：删除通配真实验证产品数据

- BDD: 删除所有通配真实验证产品 -> Given 本机 `芋道源码/admin` 租户 1 中存在中文名称匹配 `^.+真实验证[0-9]+$` 的产品主数据或展厅产品 / When 执行删除 / Then 这些产品主数据和展厅源产品均被逻辑删除，后续产品主数据列表与展厅映射预览不再出现这些中文名称。
- BDD: 非验证产品不受影响 -> Given 产品主数据和展厅产品中存在正常产品 / When 删除 `^.+真实验证[0-9]+$` / Then 仅匹配该正则的记录被删除，正常产品保留。
- BDD: 删除前置条件缺失时失败 -> Given 数据库连接、租户、表结构、匹配范围或引用关系不可确认 / When 准备删除 / Then 任务失败并记录阻塞，不执行部分删除。
- RED: 删除前数据库查询 `^.+真实验证[0-9]+$` -> FAIL，仍存在 `mdm_match_count=2`、`showroom_latest_match_count=2`、`all_matching_revision_count=20`；目标产品主数据为 `176=编辑入口真实验证53`、`177=编辑入口真实验证34`。
- RED: 删除前引用核验 -> PASS 可删除，阻塞引用均为 0：`dcc_controlled_file=0`，`showroom_hall_product=0`，`showroom_product_comment=0`，`showroom_release_document=0`，`showroom_product_revision_attachment=0`；附属项 `showroom_product_cover_batch_task_item=4` 需随源数据逻辑删除。
- GREEN: 事务逻辑删除 -> PASS，更新 `mdm_product=2`、`showroom_product=2`、`showroom_product_revision=20`、`showroom_product_cover_batch_task_item=4`。
- GREEN: 删除后数据库核验 -> PASS，`mdm_wildcard_match_count=0`，`revision_wildcard_match_count=0`，`showroom_active_wildcard_revision_join_count=0`，`active_cover_task_items_for_deleted_products=0`，`mdm_total=164`，`showroom_total=164`。
- GREEN: 负向核验 -> PASS，正常产品 `三通旋塞` 未被删除，`normal_mdm_name_count=2`，`normal_revision_name_count=15`。
- GREEN: 删除后 API 核验 -> PASS，`GET /admin-api/mdm/product/page?pageNo=1&pageSize=10` 返回 `total=164` 且不含 `真实验证`；`keyword=真实验证` 返回 `total=0`；`keyword=编辑入口真实验证` 返回 `total=0`；`GET /admin-api/showroom/product/mdm-mapping-preview` 返回 `totalCount=164`、`createCount=0`、`updateCount=164`、`linkedCount=0`、`failureCount=0`。
