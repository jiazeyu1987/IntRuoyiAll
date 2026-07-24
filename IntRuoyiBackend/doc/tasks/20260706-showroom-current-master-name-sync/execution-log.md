# 执行记录

## BDD

- BDD: 当前产品主数据名称同步 -> Given 当前未删除产品存在 `legacy_product_code=product_*` 且桌面底表存在对应展品编码 / When 执行名称同步 / Then 当前版本 `name_cn/name_en` 与底表中英文名称一致。
- BDD: 精确匹配保护 -> Given 底表缺失编码、当前版本缺失或目标记录已变化 / When 执行同步 / Then 停止并回滚，不做模糊匹配或降级。

## 证据

- GREEN: experience-preflight -> PASS, 已读取 PowerShell 和经验索引门禁；本次只写本机数据库，不涉及服务器或发布。
- GREEN: preflight -> PASS, 当前未删除产品按旧编号匹配底表 302 条，缺失底表 0 条，缺失当前版本 0 条，名称不一致 43 条。
- GREEN: update -> PASS, changed_rows=43。
- GREEN: post-check -> PASS, 当前未删除产品按旧编号匹配底表后 mismatch_count=0。
- GREEN: product081-guard -> PASS, 租户 1 的 `INT-82/product_081` 保留，租户 122 的 `INT-82` 旧编号仍为空。

## 输出

- `current-master-name-sync-result.json`
- `current-master-name-sync-before-mismatches.csv`
- `current-master-name-sync-after-mismatches.csv`
