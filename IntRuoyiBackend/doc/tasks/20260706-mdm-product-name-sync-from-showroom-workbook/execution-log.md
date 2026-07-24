# 执行记录

## BDD

- BDD: 芋道源码 MDM 产品名称同步 -> Given MDM 产品编码能通过展厅当前产品旧编号映射到底表 `product_*` / When 执行名称同步 / Then MDM `name_cn/name_en` 与底表中英文名称一致。
- BDD: 租户与匹配保护 -> Given 目标不是租户 1、缺少展厅旧编号、底表缺失编码或记录已变化 / When 执行同步 / Then 不更新该记录并保持 fail-fast。

## 证据

- GREEN: experience-preflight -> PASS, 已读取 PowerShell 和经验索引门禁；本次只写本机数据库，不涉及服务器或发布。
- GREEN: preflight -> PASS, tenant_id=1 的 MDM INT 产品 164 条，其中 154 条可通过展厅旧编号匹配到底表，24 条名称不一致，10 条缺少展厅旧编号不更新。
- GREEN: update -> PASS, changed_rows=24。
- GREEN: post-check -> PASS, tenant_id=1 的 MDM 可映射产品更新后 mismatch_count=0。
- GREEN: int1-check -> PASS, `INT-1` 已更新为 `一次性使用三通旋塞 / Manifold`。

## 输出

- `mdm-product-name-sync-result.json`
- `mdm-product-name-sync-before-mismatches.csv`
- `mdm-product-name-sync-after-mismatches.csv`
