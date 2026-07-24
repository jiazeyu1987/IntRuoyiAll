# 执行日志：修复展厅发布产品版本缺失阻塞

BDD: 展厅发布必须找到关联产品的可发布版本 -> Given 展厅 `hall_01` 绑定了产品 `product_001` / When 用户发布该展厅 / Then 发布器必须找到该产品对应 stage 的产品 revision 并写入发布快照。

BDD: 产品版本缺失必须暴露明确阻塞 -> Given 展厅绑定了产品但目标产品没有可发布 revision / When 用户发布展厅 / Then 发布必须失败并明确指出缺失的 hall、product 与 revision 条件，不能跳过产品或生成残缺发布。

## RED

- RED: `POST http://127.0.0.1:48081/admin-api/showroom/release/publish`，请求体 `{"siteKey":"yingtai-showroom","stage":"TEST"}`，请求头 `tenant-id=122`，登录用户 `aoteman/admin123` -> FAIL，返回 `SHOWROOM_RELEASE_PRODUCT_BLOCKED: hallId=10 hallCode=hall_01 productId=252 productCode=product_001 reason=SHOWROOM_TARGET_NOT_FOUND: product revision not found`。
- RED: `SELECT ... FROM showroom_product p JOIN showroom_product_revision r ON r.id=p.current_revision_id WHERE p.id=252` -> FAIL，`product_001` 属于 `tenant_id=122`，`current_revision_id=4574`，但 `showroom_product_revision.id=4574` 的 `tenant_id=0`。
- RED: `SELECT ... FROM showroom_narration_version WHERE source_revision_id=4574` -> FAIL，`id=4203/4204` 的中英文 PUBLIC PUBLISHED narration 同样为 `tenant_id=0`。
- RED: `SELECT COUNT(*) current_revision_tenant_mismatch ...` -> FAIL，存在 1 条 current revision 跨租户不一致，唯一命中为 `productId=252 / revisionId=4574`。

## GREEN

- GREEN: 精确数据修复事务 -> PASS，更新 `showroom_product_revision` 1 行、`showroom_narration_version` 2 行、`showroom_version_bundle` 1 行、`showroom_version_audit` 20 行，均从非法 `tenant_id=0` 移回测试租户 `122`。
- GREEN: `POST http://127.0.0.1:48081/admin-api/showroom/release/publish`，同 RED 请求 -> PASS，返回 `releaseId=20260529T023815Z-2c8e98f943b3`、`documentCount=166`、`assetCount=506`、`status=PUBLISHED`。
- GREEN: `SELECT ... FROM showroom_release_pointer WHERE site_key='yingtai-showroom' AND stage='TEST' AND tenant_id=122` -> PASS，`current` 指针已切到 `20260529T023815Z-2c8e98f943b3`。

## REGRESSION

- REGRESSION: `SELECT COUNT(*) AS current_revision_tenant_mismatch ...` -> PASS，返回 `0`。
- REGRESSION: `SELECT COUNT(*) FROM showroom_product_revision/narration_version/version_bundle/version_audit WHERE tenant_id=0 AND deleted=b'0'` -> PASS，四张表均返回 `0`。
- REGRESSION: `product_001` current revision、`4203/4204` narration、`1353` version bundle 均已归属 `tenant_id=122`。
- REGRESSION: 本次为本地测试租户数据修复，没有生产代码变更；未运行 Maven 单元测试。

## Root Cause

- `product_001(id=252)` 最新发布版本 `revisionId=4574` 及其发布配套记录被写入非法租户 `tenant_id=0`，而产品、展厅、展厅产品映射均在测试租户 `122`。发布器在 `tenant-id=122` 上下文中调用 `contentService.requireCurrentProductRevision(252)`，按租户隔离读取不到 `tenant_id=0` 的 revision，于是 fail-fast 抛出 `SHOWROOM_TARGET_NOT_FOUND: product revision not found`，并被展厅发布器包装为 `SHOWROOM_RELEASE_PRODUCT_BLOCKED`。

## Blockers

- 当前无。
