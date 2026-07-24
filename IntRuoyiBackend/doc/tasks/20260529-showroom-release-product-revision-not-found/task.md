# 任务：修复展厅发布产品版本缺失阻塞

## 任务目标

修复展厅发布失败：

`SHOWROOM_RELEASE_PRODUCT_BLOCKED: hallId=10 hallCode=hall_01 productId=252 productCode=product_001 reason=SHOWROOM_TARGET_NOT_FOUND: product revision not found`

目标是在不引入 fallback、不静默跳过产品的前提下，定位 `hall_01` 发布时 `product_001` 找不到产品 revision 的真实原因，并按最小范围修复发布链路或数据前置条件。

## BDD 场景

- BDD: 展厅发布必须找到关联产品的可发布版本 -> Given 展厅 `hall_01` 绑定了产品 `product_001` / When 用户发布该展厅 / Then 发布器必须找到该产品对应 stage 的产品 revision 并写入发布快照。
- BDD: 产品版本缺失必须暴露明确阻塞 -> Given 展厅绑定了产品但目标产品没有可发布 revision / When 用户发布展厅 / Then 发布必须失败并明确指出缺失的 hall、product 与 revision 条件，不能跳过产品或生成残缺发布。

## 里程碑

- [x] M1：创建任务文档并记录 BDD。
- [x] M2：复现 `hallId=10 / productId=252` 发布失败并定位查询条件。
- [x] M3：补充 RED 回归测试或真实数据证据。
- [x] M4：实施最小数据修复。
- [x] M5：运行 GREEN/回归验证，记录证据。
- [ ] M6：运行 task-closeout-cleanup 预览并提交本任务改动。

## 预期验证

- 真实数据查询确认 `hall_01` 与 `product_001` 的展厅、产品、版本中心记录状态。
- 复现发布接口或同等服务方法返回当前错误。
- RED/GREEN 证据记录到 `execution-log.md`。
- 若为代码缺陷，运行对应 Maven 测试；若为数据前置条件缺失，记录缺失条件和影响，不提交伪修复。
- 修复后真实发布路径不再报 `SHOWROOM_TARGET_NOT_FOUND: product revision not found`，且不会跳过产品。

## 当前状态

completed

## 约束

- 不引入默认产品、默认 revision、跳过产品、静默成功或兼容 fallback。
- 未经用户明确批准，不修改正式环境数据；本地与测试租户数据修复必须限定范围并记录。
- 芋道源码/admin 仅可做只读验证，测试租户用于修复和 E2E。

## 修复摘要

- 本地测试租户 `122` 的 `showroom_product.id=252` 当前版本指向 `revisionId=4574`，但该 revision、对应中英文 narration、version bundle 和 audit 行错误落在 `tenant_id=0`。
- 已精确更新这些行回 `tenant_id=122`：产品 revision 1 行、narration 2 行、version bundle 1 行、version audit 20 行。
- 修复后发布接口返回新 release `20260529T023815Z-2c8e98f943b3`，并切换 `tenant_id=122 / yingtai-showroom / TEST` 的 current 指针。

## Cleanup Keep

- `doc/tasks/20260529-showroom-release-product-revision-not-found/bug-regression-evidence.md`
