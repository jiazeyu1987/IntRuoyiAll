# 执行日志：20260524-showroom-local-website-config-live-data-realign

BDD: website-config 应返回可用 public display 聚合数据 -> Given 本地 hall 映射只指向一组已发布且 preview/narration source revision 一致的产品资源 / When 请求 `GET /showroom/display/website-config` / Then 接口必须返回 `code=0` 的真实聚合数据。
BDD: 仍然保留 fail-fast 语义 -> Given public display 依赖的 live preview 或 narration source revision 不一致 / When 请求 `GET /showroom/display/website-config` / Then 运行时必须继续抛出明确的 `SHOWROOM_TARGET_NOT_FOUND`，而不是伪造成功数据。

PRECHECK: previous same-repo task `20260524-showroom-product-pagination-completeness-fix` -> COMPLETED，不阻塞当前本地 live 数据修复。
RED: authenticated `GET http://127.0.0.1:48082/showroom/display/website-config` -> FAIL, returns `SHOWROOM_TARGET_NOT_FOUND: live product ZH narration source revision mismatch`.
RED: local live-data probe -> FAIL, `showroom_hall_product` had drifted back to `165` live mappings across `165` products; `product_001` current revision was `2551`, while the latest published preview and PUBLIC ZH/EN narration still pointed at `source_revision_id = 2367`.
GREEN: `repair-local-website-config-live-data.sql` -> PASS, local public display mapping was realigned to `8` halls / `1` product, and product `1` live preview plus PUBLIC ZH/EN narration source revisions now all point at `2551`.
GREEN: authenticated `GET http://127.0.0.1:48082/showroom/display/website-config` -> PASS, returned `status=200 code=0 hallCount=8 firstProductId=1`.
GREEN: `$env:INT_RUOYI_ADMIN_API_BASE='http://127.0.0.1:48082/admin-api'; node --test D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-frontstage-runtime.test.mjs` -> PASS.
