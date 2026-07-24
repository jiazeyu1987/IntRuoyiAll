# 执行记录

## BDD

- BDD: product_081 仅保留芋道源码映射 -> Given `product_081` 同时存在于芋道源码与测试租户的当前产品映射 / When 按用户要求只保留芋道源码 / Then `tenant_id=1 / INT-82` 保留 `product_081`，`tenant_id=122 / INT-82` 的旧编号被清空。
- BDD: 安全保护 -> Given 目标记录不再是测试租户 `INT-82` 或旧编号不再是 `product_081` / When 执行更新 / Then 停止并回滚。

## 证据

- GREEN: experience-preflight -> PASS, 已读取 PowerShell 和经验索引门禁；本次只写本机数据库。
- GREEN: preflight -> PASS, 当前 `product_081` 映射共 2 条：租户 1 与租户 122 各 1 条。
- GREEN: update -> PASS, 清空测试租户 `tenant_id=122 / INT-82` 的 `legacy_product_code=product_081`，changed_rows=1。
- GREEN: post-check -> PASS, 当前仅剩租户 1 的 `INT-82 / product_081` 映射，remaining_product_081_mapping_count=1。

## 输出

- `product081-yudao-only-result.json`
