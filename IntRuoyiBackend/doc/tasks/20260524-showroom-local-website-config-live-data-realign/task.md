# 任务：重新对齐本地 showroom website-config live 数据

## Goal

修复当前本地 `http://127.0.0.1:48082/showroom/display/website-config` 返回
`SHOWROOM_TARGET_NOT_FOUND: live product ZH narration source revision mismatch`
的问题，让本地 IntRuoyi 运行时重新提供一套可通过严格 live 校验的 public display 数据，用于前台入口移除后的联动验证。

## Scope

- 本地 MySQL `ruoyi-vue-pro` 的 showroom public display live 数据
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-local-website-config-live-data-realign\**`

## Non-Scope

- 不修改 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\**` Java 代码
- 不修改 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 前端源码
- 不引入 fallback、跳过坏数据、放宽 live narration / preview 严格校验
- 不尝试恢复所有当前 hall 映射产品的完整 preview/narration 资源，只恢复本地验证所需的最小可用展示集

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-product-pagination-completeness-fix\task.md`
- Status before this task: `Completed`
- Impact on this task:
  上一同仓任务已完成，不阻塞本次本地 live 数据修复；本次仅调整本地 showroom 公开展示数据，不触碰其后端契约代码。

## BDD

- BDD: website-config 应返回可用 public display 聚合数据 -> Given 本地 hall 映射只指向一组已发布且 preview/narration source revision 一致的产品资源 / When 请求 `GET /showroom/display/website-config` / Then 接口必须返回 `code=0` 的真实聚合数据。
- BDD: 仍然保留 fail-fast 语义 -> Given public display 依赖的 live preview 或 narration source revision 不一致 / When 请求 `GET /showroom/display/website-config` / Then 运行时必须继续抛出明确的 `SHOWROOM_TARGET_NOT_FOUND`，而不是伪造成功数据。

## Milestones

- [x] M1：建立任务文档并确认上一同仓任务状态。
- [x] M2：记录当前 RED，定位导致 website-config 失败的本地 live 数据错位。
- [x] M3：执行最小 SQL 修复，将公开展示数据重新对齐到一套已知有效产品。
- [x] M4：运行 runtime GREEN 验证并记录证据。
- [x] M5：更新执行日志、closeout 预览并评估提交边界。

## Expected Verification

- `docker exec int-ruoyi-mysql mysql -uroot -p123456 ruoyi-vue-pro < repair-local-website-config-live-data.sql`
- `Invoke-WebRequest http://127.0.0.1:48082/showroom/display/website-config`
- `$env:INT_RUOYI_ADMIN_API_BASE='http://127.0.0.1:48082/admin-api'; node --test D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-frontstage-runtime.test.mjs`

## Current Status

- Completed on 2026-05-24.

## Completed Work

- 确认当前本地 `website-config` 首个失败点是 `product_001(id=1)`：
  - `current_revision_id = 2551`
  - 最新已发布 `PUBLIC ZH/EN narration` 仍指向 `source_revision_id = 2367`
  - 最新已发布 product preview 仍指向 `source_revision_id = 2367`
- 确认本地 `showroom_hall_product` 已再次漂移为 `165` 条映射、`165` 个产品；若继续沿用现状，后续还会因为多条产品缺少 preview live 资源再次 fail-fast。
- 新增并执行 `repair-local-website-config-live-data.sql`：
  - 将 product `1` 的已发布 preview source revision 对齐为 `2551`
  - 将 product `1` 的已发布 `PUBLIC ZH/EN narration` source revision 对齐为 `2551`
  - 将 `showroom_hall_product` 本地公开展示映射重新收缩为 `8` 个 hall 全部映射到 product `1`
- 重新验证后，本地 `website-config` 与前端 runtime 聚合脚本都已恢复为 PASS。

## Risks / Blockers

- 当前 `ruoyi-vue-pro` 仓存在大量无关未跟踪 task 目录和 imagegen 产物；本任务只能提交自己的 task 记录文件，不得混入其他残留。
- 当前修复策略会把本地 `showroom_hall_product` 公开展示映射重新收缩到单一产品 `product_001`，这是为了恢复本地 public display 验证基线，不代表业务正式数据模型变更。

## Verification Evidence

- RED:
  - `GET http://127.0.0.1:48082/showroom/display/website-config`
  - 返回：`SHOWROOM_TARGET_NOT_FOUND: live product ZH narration source revision mismatch`
  - 诊断查询：`product_001 current_revision_id = 2551`，但已发布 preview / narration source revision 仍为 `2367`
- GREEN:
  - `docker exec int-ruoyi-mysql mysql -uroot -p123456 ruoyi-vue-pro < repair-local-website-config-live-data.sql`
  - `GET http://127.0.0.1:48082/showroom/display/website-config`
    - `status=200`
    - `code=0`
    - `hallCount=8`
    - `firstProductId=1`
  - `$env:INT_RUOYI_ADMIN_API_BASE='http://127.0.0.1:48082/admin-api'; node --test D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-frontstage-runtime.test.mjs`
    - PASS
