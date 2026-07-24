# Execution Log: 单个封面生成后持久化到产品版本

BDD: 单个 AI 生成后列表应显示封面 -> Given 已发布产品在真实页面点击 `AI生成` 成功 / When 再次请求 `/showroom/product/page` / Then `displayRevision.fields.cover_image` 必须带出最新封面地址，列表不应继续显示“未上传”。

BDD: 单个 AI 生成仍保持真实图片生成与文件代理地址 -> Given 当前已完成 Codex CLI 生成和 `/admin-api/infra/file/...` 代理地址修复 / When 单个产品生成封面 / Then 仍应沿用该链路，只额外补齐版本持久化。

RED: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomApiRuntimeProductCoverPersistenceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，新增回归断言要求 `generateProductCoverImage(...)` 走持久化并发布，但旧方法签名仍不接收 `operatorUserId`，且真实 `/showroom/product/page` 查询显示 `product_001` 的 `revisionCover/displayRevisionCover` 仍为 `null`。

GREEN: `mvn --% -pl yudao-module-showroom -DskipTests -Dmaven.compiler.useIncrementalCompilation=false compile` -> PASS。

GREEN: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomApiRuntimeProductCoverPersistenceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test` -> PASS，1 test green，已验证单个生成会触发 `saveProductDraft + publishProductRevision`。

GREEN: 真实接口复验 -> PASS，重新生成 `product_001` 后，再查 `/showroom/product/page`：
- `revisionCover=/admin-api/infra/file/28/get/showroom/product/cover/20260521/product-product_001-cover.png`
- `displayRevisionCover=/admin-api/infra/file/28/get/showroom/product/cover/20260521/product-product_001-cover.png`
- `displayRevisionNo=15`
