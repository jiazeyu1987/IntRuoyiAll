# 任务：分析展柜管理产品每柜仅一个异常

## Task Goal

- 分析后台“展柜管理”下产品列表为什么表现为每个展柜只有一个产品。
- 定位异常发生在前端展示、管理端接口、后端查询/保存逻辑、release 投影，还是数据库关联数据。
- 本任务先做原因分析，不修改生产代码；若需要修复，另起 RED/GREEN 修复步骤。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\**`
- 相关展柜/产品接口、Mapper、SQL 与测试证据。

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-release-dirty-debounce\task.md`
- Status before this task: `Completed`
- Note: 后端仓库仍存在该前序任务的未提交改动，本任务只读取分析，不覆盖、不提交这些既有改动。

## BDD Scenario

- BDD: 展柜管理应返回展柜完整产品映射 -> Given 一个展柜配置了多个产品 / When 管理端打开展柜管理并查看该展柜产品 / Then 每个已配置产品都应可见，不能只剩一个。

## Milestones

- [x] M1：确认前序任务状态，建立本任务文档和执行日志。
- [x] M2：梳理前端展柜管理产品展示入口和接口参数。
- [x] M3：梳理后端展柜产品查询、保存、release 投影与测试覆盖。
- [x] M4：用现有接口/数据证据定位根因。
- [x] M5：记录结论、风险、建议修复路径与验证结果。

## Expected Verification

- `rg` 检索展柜管理前端入口、API、后端 Controller/Service/Mapper。
- 如本地服务可用，使用真实接口读取展柜配置与产品映射。
- 如需要数据库证据，仅做只读查询，不改 live 数据。
- 本任务不通过 mock、默认值或 fallback 掩盖异常。

## Current Status

- Status: `completed`
- 当前阶段：分析完成。
- 结论：当前本地运行库 `showroom_hall_product` 只有 8 条有效映射，8 个展柜全部只映射到 `product_id=1`。展柜管理页面和后端接口只是如实展示该表数据；根因是 `20260524-showroom-local-website-config-live-data-realign` 任务为了恢复本地 public display 严格校验，将本地公开展示映射主动收缩为“8 个 hall / 1 个 product”的最小可用集。

## Final Verification Result

- `docker exec int-ruoyi-mysql mysql ... SELECT COUNT(*) ... showroom_hall_product` -> PASS，本地有效映射数为 `8`，每个展柜 `product_count=1`，`product_ids=1`。
- `GET http://127.0.0.1:48081/admin-api/showroom/hall/page?pageNo=1&pageSize=20` with tenant `1` and `admin/admin123` -> PASS，管理端接口返回 8 个展柜，每个 `productCount=1`、`productMappings=[1]`。
- 代码检查 -> PASS，前端 `HallProductMappingDialog` 使用多选并按数组保存；后端 `ShowroomApiRuntime.toHallPageRow` 按 `hall.productMappings().size()` 计算数量，未发现只取首个产品的展示或查询逻辑。

## Risk / Follow-up

- 这是本地运行库数据状态，不是正式业务模型变更。若需要恢复“展柜管理”的完整产品数量，应按 Excel/历史映射重建 `showroom_hall_product`，并同时处理 public display 所依赖的 preview/narration live 资源一致性，否则 `website-config` 会继续 fail-fast。
