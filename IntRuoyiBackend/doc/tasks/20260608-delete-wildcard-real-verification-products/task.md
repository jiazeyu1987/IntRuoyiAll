# 任务：删除通配真实验证产品数据

## 任务目标

按用户最新要求，删除中文名称符合 `XXXX真实验证XX` 形式的验证条目。本任务将 `XXXX` 解释为任意非空前缀，`XX` 解释为数字后缀，严格匹配正则 `^.+真实验证[0-9]+$`。删除范围同时覆盖产品主数据和展厅源产品，避免后续展厅映射再次生成这些验证数据。

## Previous Task Check

- 上一个数据清理任务：`doc/tasks/20260608-delete-publish-entry-verification-products/task.md`
- 状态：`completed`
- 处理：上一任务已逻辑删除 `发布入口真实验证[0-9]+`；本任务继续清理仍活跃的其他 `XXXX真实验证XX` 条目。

## BDD 场景

- BDD: 删除所有通配真实验证产品 -> Given 本机 `芋道源码/admin` 租户 1 中存在中文名称匹配 `^.+真实验证[0-9]+$` 的产品主数据或展厅产品 / When 执行删除 / Then 这些产品主数据和展厅源产品均被逻辑删除，后续产品主数据列表与展厅映射预览不再出现这些中文名称。
- BDD: 非验证产品不受影响 -> Given 产品主数据和展厅产品中存在正常产品 / When 删除 `^.+真实验证[0-9]+$` / Then 仅匹配该正则的记录被删除，正常产品保留。
- BDD: 删除前置条件缺失时失败 -> Given 数据库连接、租户、表结构、匹配范围或引用关系不可确认 / When 准备删除 / Then 任务失败并记录阻塞，不执行部分删除。

## Milestones

- [x] M1：建立任务文档、执行日志和 BDD。
- [x] M2：查询匹配产品主数据、展厅产品及引用关系。
- [x] M3：在事务中执行逻辑删除并验证匹配记录为 0。
- [ ] M4：记录证据、运行收尾预览并提交本任务文档。

## Expected Verification

- 删除前查询：匹配 `^.+真实验证[0-9]+$` 的活跃产品主数据或展厅产品数量大于 0。
- 删除事务：只更新租户 1、`deleted=0` 且中文名称严格匹配该正则的数据。
- 删除后查询：产品主数据、展厅产品最新版本和展厅映射预览中匹配数量均为 0。
- 负向核验：正常产品例如 `三通旋塞` 等非验证产品仍保留。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。数据库连接、租户、表结构、匹配范围或引用关系异常时直接失败。
- `是否从根因和长期维护角度解决`：是。同步清理展厅源产品和产品主数据，避免源数据下次映射重新生成。
- `是否存在临时补丁或绕过`：否。不改产品主数据映射逻辑，不绕过租户边界。

## 数据风险与回滚说明

- 本任务为用户明确要求的数据删除，采用逻辑删除，不物理删除。
- 删除范围限定为租户 1、中文名称严格匹配 `^.+真实验证[0-9]+$` 的验证数据。
- 回滚策略：删除前记录匹配产品主数据 ID、展厅产品 ID 和关联 ID；如需恢复，可在同一租户内把这些 ID 的 `deleted` 改回 `0`，并重新确认展厅映射。

## 当前状态

completed: 已按用户要求删除中文名称严格匹配 `^.+真实验证[0-9]+$` 的活跃验证条目。删除采用逻辑删除，范围为租户 1 的 2 条产品主数据、2 条展厅产品、20 条展厅产品版本和 4 条展厅封面批处理附属项；DCC、展柜、评论、发布文档和附件引用均为 0。删除后产品主数据与展厅产品总数均为 164，匹配 `^.+真实验证[0-9]+$` 的活跃记录为 0。

## 最终验证结果

- 删除前查询：`mdm_match_count=2`，`showroom_latest_match_count=2`，`all_matching_revision_count=20`，`active_revision_count_for_matched_products=20`。
- 删除前目标产品主数据：`176=编辑入口真实验证53`，`177=编辑入口真实验证34`。
- 删除前目标展厅产品：`249=编辑入口真实验证53`，`250=编辑入口真实验证34`。
- 引用核验：`dcc_controlled_file=0`，`showroom_hall_product=0`，`showroom_product_comment=0`，`showroom_release_document=0`，`showroom_product_revision_attachment=0`，`showroom_product_cover_batch_task_item=4`。
- 删除事务：PASS，更新 `mdm_product=2`、`showroom_product=2`、`showroom_product_revision=20`、`showroom_product_cover_batch_task_item=4`。
- 删除后数据库核验：PASS，`mdm_wildcard_match_count=0`，`revision_wildcard_match_count=0`，`showroom_active_wildcard_revision_join_count=0`，`active_cover_task_items_for_deleted_products=0`，`mdm_total=164`，`showroom_total=164`。
- 负向核验：PASS，正常产品 `三通旋塞` 仍保留，`normal_mdm_name_count=2`，`normal_revision_name_count=15`。
- API 核验：PASS，`GET /admin-api/mdm/product/page?pageNo=1&pageSize=10` 返回 `total=164` 且当前页不含 `真实验证`；`keyword=真实验证` 返回 `total=0`；`keyword=编辑入口真实验证` 返回 `total=0`；`GET /admin-api/showroom/product/mdm-mapping-preview` 返回 `totalCount=164`、`createCount=0`、`updateCount=164`、`linkedCount=0`、`failureCount=0`。

## Cleanup Keep

- doc/tasks/20260608-delete-wildcard-real-verification-products/database-schema-evidence.md
