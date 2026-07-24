# product_081 仅保留芋道源码映射

## 任务目标

- 将 `product_081` 的当前旧编号映射只保留在“芋道源码”租户。
- 保留 `tenant_id=1 / INT-82 / legacy_product_code=product_081`。
- 清空测试租户 `tenant_id=122 / INT-82` 上的 `legacy_product_code=product_081`。
- 不修改产品名称、版本、展柜、讲解、图片或删除状态。

## 经验门禁

- PowerShell/Windows shell：已读取 `docs/powershell-memory.md`，数据库写入使用 UTF-8 aware Python/PyMySQL，避免 PowerShell 管道传 SQL。
- 项目经验索引：已读取 `docs/experience-index.md`；本次只写本机数据库，不涉及服务器、发布或真实 E2E。
- No fallback：preflight 不满足“精确两条映射、其中测试租户一条可清空”时停止。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按租户边界修正旧编号权威映射。
- `是否存在临时补丁或绕过`：否。

## 执行范围

- 只更新 `showroom_product` 一行：
  - `tenant_id=122`
  - `product_id=521`
  - `product_code=INT-82`
  - 原 `legacy_product_code=product_081`

## 当前状态

- 已完成。
- 执行结果：`product081-yudao-only-result.json`。

## Cleanup Keep

- `doc/tasks/20260706-showroom-product081-yudao-only/product081-yudao-only-result.json`
