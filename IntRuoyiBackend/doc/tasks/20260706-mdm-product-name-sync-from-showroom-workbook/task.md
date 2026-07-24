# MDM 产品主数据名称同步

## 任务目标

- 修正“芋道源码”租户产品主数据列表中，`mdm_product` 的中英文名称与桌面底表不一致的问题。
- 匹配链路固定为：`mdm_product.product_code = showroom_product.product_code`，再使用 `showroom_product.legacy_product_code = 底表 产品列表.展品编码`。
- 只更新 `tenant_id=1`、未删除、已能通过展厅旧编号映射到底表的 MDM 产品。
- 不修改测试租户、不修改展厅表、不修改编码、型号、分类、状态、DCC 编码、图片或讲解。

## 经验门禁

- PowerShell/Windows shell：已读取 `docs/powershell-memory.md`，中文、Excel、MySQL 操作使用 UTF-8 aware Python/PyMySQL 路径。
- 项目经验索引：已读取 `docs/experience-index.md`；本次只写本机数据库 `127.0.0.1:23306/ruoyi-vue-pro`，不涉及服务器、发布或真实 E2E。
- No fallback：仅按展厅旧编号与底表展品编码精确匹配；无旧编号或底表缺失不更新。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；修正 MDM 权威产品名称，与底表和展厅当前版本名称保持一致。
- `是否存在临时补丁或绕过`：否。

## 执行范围

- 表：`mdm_product`。
- 租户：`tenant_id=1`。
- 字段：`name_cn`、`name_en`、`update_time`。
- 预计更新：当前 preflight 识别到 24 条名称不一致。

## 当前状态

- 已完成。
- 执行结果：`mdm-product-name-sync-result.json`。
- MDM 可映射产品名称更新后：`mismatch_count=0`。

## Cleanup Keep

- `doc/tasks/20260706-mdm-product-name-sync-from-showroom-workbook/mdm-product-name-sync-result.json`
- `doc/tasks/20260706-mdm-product-name-sync-from-showroom-workbook/mdm-product-name-sync-before-mismatches.csv`
- `doc/tasks/20260706-mdm-product-name-sync-from-showroom-workbook/mdm-product-name-sync-after-mismatches.csv`
