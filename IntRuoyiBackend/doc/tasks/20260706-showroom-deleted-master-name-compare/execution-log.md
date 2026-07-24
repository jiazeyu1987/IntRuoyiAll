# 执行记录

## BDD

- BDD: 主数据删除名称同步 -> Given 删除 item 的 `product_code` 可在底表 `展品编码` 中找到 / When 执行当前版本名称同步 / Then 当前版本 `name_cn/name_en` 与底表完全一致，历史版本和删除状态不变。
- BDD: 安全保护 -> Given 任一目标记录不再是租户 122 的删除 item 或 current_revision_id 已变化 / When 执行同步 / Then 停止执行并回滚。

## 证据

- GREEN: experience-preflight -> PASS, 已读取 PowerShell 和经验索引门禁；本次只写本机数据库。
- GREEN: preflight -> PASS, 精确命中 5 条删除 item 当前版本。
- GREEN: update -> PASS, changed_rows=5。
- GREEN: post-check -> PASS, 5 条当前版本中英文名称与底表一致。
- GREEN: final-compare -> PASS, mismatch_count=0。

## 输出

- `deleted-master-name-sync-result.json`
- `deleted-master-name-sync-before-mismatches.csv`
- `deleted-master-name-sync-after-mismatches.csv`
