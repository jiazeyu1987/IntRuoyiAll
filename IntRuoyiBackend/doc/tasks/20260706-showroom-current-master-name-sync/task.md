# 展厅当前产品主数据名称同步

## 任务目标

- 将当前未删除产品主数据中，已配置 `legacy_product_code=product_*` 的产品名称同步为桌面底表 `展厅讲解软件产品资料更新底表.xlsx` 中对应 `展品编码` 的中英文名称。
- 只更新 `showroom_product.current_revision_id` 指向的 `showroom_product_revision.name_cn/name_en`。
- 不更新历史版本、不修改旧编号映射、不修改展柜、讲解、图片、删除状态或产品编码。

## 经验门禁

- PowerShell/Windows shell：已读取 `docs/powershell-memory.md`，中文、Excel、MySQL 操作使用 UTF-8 aware Python/PyMySQL 路径，避免 PowerShell 直接传 SQL 文本。
- 项目经验索引：已读取 `docs/experience-index.md`；本次只写本机数据库 `127.0.0.1:23306/ruoyi-vue-pro`，不涉及服务器、发布或真实 E2E。
- No fallback：仅按 `legacy_product_code = 底表 展品编码` 精确匹配；底表缺失或版本缺失则阻塞，不做名称猜测。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；以底表旧编号为权威来源逐条同步当前版本名称，并保留执行证据。
- `是否存在临时补丁或绕过`：否。

## 执行范围

- 当前未删除产品：`showroom_product.deleted + 0 = 0`。
- 匹配字段：`showroom_product.legacy_product_code = 产品列表.展品编码`。
- 更新字段：`showroom_product_revision.name_cn`、`showroom_product_revision.name_en`、`update_time`。

## 当前状态

- 已完成。
- 执行结果：`current-master-name-sync-result.json`。
- 当前产品主数据更新后：`mismatch_count=0`。

## Cleanup Keep

- `doc/tasks/20260706-showroom-current-master-name-sync/current-master-name-sync-result.json`
- `doc/tasks/20260706-showroom-current-master-name-sync/current-master-name-sync-before-mismatches.csv`
- `doc/tasks/20260706-showroom-current-master-name-sync/current-master-name-sync-after-mismatches.csv`
