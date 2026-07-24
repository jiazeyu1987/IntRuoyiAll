# 执行日志：分析展柜管理产品每柜仅一个异常

BDD: 展柜管理应返回展柜完整产品映射 -> Given 一个展柜配置了多个产品 / When 管理端打开展柜管理并查看该展柜产品 / Then 每个已配置产品都应可见，不能只剩一个。

INVESTIGATION: 2026-05-24 -> 前序同仓任务 `20260524-showroom-release-dirty-debounce` 状态为 `Completed`，但后端仓库存在该任务未提交改动；本任务仅读取分析，避免混入或覆盖。

INVESTIGATION: 2026-05-24 -> 前端 `HallListTable` 使用 `productMappings.length` 计算产品数量；`HallProductMappingDialog` 使用 `el-select multiple` 选择产品，并通过 `buildHallMappingPayload` 将多个 `productId` 生成有序 `products[]` payload，未发现前端只能保留一个产品的逻辑。

INVESTIGATION: 2026-05-24 -> 后端 `GET /showroom/hall/page` 调用 `ShowroomApiRuntime.listHalls(...)`，再由 `toHallPageRow(...)` 将 `hall.productMappings()` 全量映射为响应，并用 `productMappings.size()` 作为 `productCount`；`replaceHallProductMappings(...)` 会按请求数组全量重建映射，未发现查询层只取首条产品的逻辑。

RED: `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "<hall mapping count query>"` -> FAIL，当前本地运行库 `showroom_hall_product` 有效映射只有 `8` 条；`hall_01` 到 `hall_08` 每个展柜 `product_count=1`，且 `product_ids=1`。

RED: authenticated `GET http://127.0.0.1:48081/admin-api/showroom/hall/page?pageNo=1&pageSize=20` with tenant `1` / `admin/admin123` -> FAIL，接口如实返回 8 个展柜，每个 `productCount=1`、`productMappings=[1]`，与数据库一致。

ROOT CAUSE: `20260524-showroom-local-website-config-live-data-realign` 任务为了恢复本地 `website-config` 严格 live 校验，将本地 `showroom_hall_product` 从 165 条漂移映射主动收缩为 `8 halls / 1 product`，全部指向 product `1`。因此“展柜管理每个展柜只有一个产品”是本地数据被收缩后的结果，不是前端多选或后端列表接口只返回一个。

GREEN: root-cause analysis -> PASS，已确认历史任务 `20260522-showroom-hall-mapping-match-excel` 曾按 Excel 恢复到 `166` 条映射、分布 `26 / 28 / 27 / 17 / 10 / 20 / 11 / 27`；后续 `20260524-showroom-local-website-config-live-data-realign` 为 public display 本地验证重新收缩为 8 条。
