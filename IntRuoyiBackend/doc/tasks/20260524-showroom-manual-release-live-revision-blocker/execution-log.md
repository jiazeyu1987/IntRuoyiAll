# 执行日志：排查手动发布展厅 live product revision 缺失阻塞

BDD: 手动发布展厅应在坏产品存在时继续组装可发布 release -> Given 用户点击“手动发布展厅”且某个展厅内存在缺少 live revision 或缺少必需发布素材的产品 / When 后端组装 showroom release source snapshot / Then 坏产品应被跳过，其他可发布产品继续进入 release

BDD: 数据异常必须 fail-fast 暴露真实 blocker -> Given 某个 live 产品的 `current_revision_id` 指向不存在的 revision / When 执行 `publishRelease()` / Then 后端必须返回真实错误，不能静默跳过该产品

BDD: 某个展厅被过滤后没有可发布产品时应跳过该展厅 -> Given 某个展厅下所有产品都因为缺 live revision 或缺发布素材被过滤 / When 继续组装 release / Then 该展厅本身应被跳过，其他展厅继续发布

INVESTIGATION: 2026-05-24 -> 已确认点击前端“手动发布展厅”时，后端日志多次在 `/admin-api/showroom/release/publish` 期间抛出 `SHOWROOM_TARGET_NOT_FOUND: live product revision not found`。
INVESTIGATION: 2026-05-24 -> 堆栈落点为 `ShowroomReleasePublisherService.publishRelease(...) -> ShowroomApiRuntime.publishRelease(...) -> ShowroomAdminController.publishRelease(...)`。
INVESTIGATION: 2026-05-24 -> 已通过数据库核查定位唯一阻塞对象为 `hall_01 / 心内介植入展厅` 下的 `product_166 / 一次性使用射频房间隔穿刺针`，其 `current_revision_id = NULL` 且 `status = DRAFT_ONLY`。
INVESTIGATION: 2026-05-24 -> 已确认当前 Website 仍指向旧 release `20260524T100623Z-316b86ad1758`，该 release source snapshot 只包含 1 个 product revision，因此各展厅都只显示 1 个产品。
RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseAdminPublishIntegrationTest#publishReleaseShouldSkipProductsWithoutLiveRevisionAndDropEmptyHalls" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，原逻辑在 `ShowroomReleaseAssembler.resolveProduct()` 调用 `requireCurrentProductRevision()` 时直接抛 `SHOWROOM_TARGET_NOT_FOUND: live product revision not found`。
GREEN: 同上命令 -> PASS，坏产品 `P-SKIP-001` 被记录为 `SHOWROOM_RELEASE_SKIP_PRODUCT`，仅挂坏产品的展厅被记录为 `SHOWROOM_RELEASE_SKIP_HALL`，发布继续成功。
GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseAdminPublishIntegrationTest,ShowroomReleaseAutoPublishServiceTest,ShowroomVersionCenterServiceTest,ShowroomPersistentContentServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，手动发布、自动发布、版本中心重发与内容服务回归通过。
GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，修复已编入最新 `yudao-server.jar`。
RED: Playwright 真实前端发布 `http://127.0.0.1:8081/showroom/company` -> FAIL，请求已发到 `/admin-api/showroom/release/publish`，但后端返回 `Duplicate entry 'product-2-preview-cd5aaa69f4a0d84fd14b0d63fcceb5e1a9602fa71371cf...' for key 'showroom_release_asset.uk_showroom_release_asset'`。
INVESTIGATION: 2026-05-25 -> 已确认冲突 asset 行实际存在于 `showroom_release_asset`，但处于逻辑删除状态，`selectByAssetIdAndContentHash(...)` 因逻辑删除过滤返回空，导致重新插入命中唯一键。
GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseAdminPublishIntegrationTest#publishReleaseShouldReuseLogicallyDeletedAssetRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，逻辑删除 asset 现在会被复活复用，不再重新插入失败。
GREEN: Playwright 真实前端发布 `http://127.0.0.1:8081/showroom/company` -> PASS，请求 `/admin-api/showroom/release/publish` 最终成功完成；`GET /showroom/release/current` 从 `20260524T100623Z-316b86ad1758` 切换为 `20260524T163916Z-e03a7b68bf1a`。
