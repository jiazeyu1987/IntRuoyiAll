# 主数据删除 Item 名称同步

## 任务目标

- 将主数据删除中 5 个 `product_*` item 的当前版本中英文名称同步为桌面底表 `产品列表` 对应名称。
- 仅更新 `showroom_product.current_revision_id` 指向的 `showroom_product_revision.name_cn/name_en`。
- 不恢复删除状态、不更新历史版本、不修改编码、旧编号、展柜、讲解或图片。

## 经验门禁

- PowerShell/Windows shell：已读取 `docs/powershell-memory.md`，所有中文读取、写入、数据库传参使用 UTF-8 aware Python/PyMySQL 路径。
- 项目经验索引：已读取 `docs/experience-index.md`；本次为本机数据库写入，不涉及服务器、发布、真实 E2E。
- No fallback：preflight 不满足精确 5 条时停止；不做模糊匹配、不猜测其它 item。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按底表权威编码逐条同步，并保留执行报告。
- `是否存在临时补丁或绕过`：否。

## 执行范围

- 租户 `122`：`product_087`、`product_094`、`product_097`、`product_098`、`product_109`。
- 更新版本：仅 `current_revision_id` 对应的 5 条版本记录。

## 当前状态

- 已完成。
- 执行结果：`deleted-master-name-sync-result.json`。

## Cleanup Keep

- `doc/tasks/20260706-showroom-deleted-master-name-compare/deleted-master-name-mismatch-report.csv`
- `doc/tasks/20260706-showroom-deleted-master-name-compare/deleted-master-name-mismatch-report.json`
- `doc/tasks/20260706-showroom-deleted-master-name-compare/deleted-master-product-code-name-mismatch-report.csv`
- `doc/tasks/20260706-showroom-deleted-master-name-compare/deleted-master-product-code-name-mismatch-report.json`
- `doc/tasks/20260706-showroom-deleted-master-name-compare/deleted-master-name-sync-before-mismatches.csv`
- `doc/tasks/20260706-showroom-deleted-master-name-compare/deleted-master-name-sync-after-mismatches.csv`
- `doc/tasks/20260706-showroom-deleted-master-name-compare/deleted-master-name-sync-result.json`
