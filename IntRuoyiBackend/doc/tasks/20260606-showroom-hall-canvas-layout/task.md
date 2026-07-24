# 任务：展柜画布布局编辑后端持久化

## 任务目标

为展柜产品映射增加矩形画布布局坐标持久化能力，使前端“画布布局”可保存每个产品块的归一化 `x/y/width/height`，并在展柜列表接口中返回。

## Previous Task Check

- 同展厅后端前序任务：`doc/tasks/20260606-showroom-product-attachment-save-preview-fix/task.md`。
- 检查结果：状态为 `completed`。
- 仓库内非展厅 runtime 任务 `20260606-runtime-backup-object-key-archive` 仍为 `in_progress`，本任务不改动 runtime 范围。

## BDD 场景

- BDD: 保存展柜产品布局坐标 -> Given 展柜有真实产品映射 / When 调用保存映射接口并携带 `layoutX/layoutY/layoutWidth/layoutHeight` / Then 后端持久化坐标并按 displayOrder 返回。
- BDD: 缺少布局坐标失败 -> Given 调用保存映射接口 / When 任一产品映射缺少坐标或坐标越界 / Then 后端 fail-fast 返回错误，不保存部分数据。
- BDD: 旧映射读取生成默认布局 -> Given 历史映射没有坐标 / When 查询展柜列表 / Then 返回按产品数量平均生成的布局坐标。

## 里程碑

- [x] M1：检查现有映射模型、DO、Mapper、控制器和测试表结构。
- [x] M2：新增 RED 后端单测和 SQL 契约测试。
- [x] M3：实现模型、DO、接口、服务和迁移。
- [x] M4：运行后端目标测试并记录证据。

## 预期验证

- `mvn -pl yudao-module-showroom -Dtest=ShowroomHallContentTest,ShowroomPersistentContentServiceTest test`
- `python -X utf8 -m pytest script/tests/test_showroom_sql_scripts.py`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。保存接口缺坐标或坐标非法必须失败，不写部分成功。
- `是否从根因和长期维护角度解决`：是。布局坐标归属展柜-产品关系表，接口读写同一份真实数据。
- `是否存在临时补丁或绕过`：否。不使用前端本地缓存替代数据库持久化。

## Current Status

completed

## 进展记录

- 2026-06-06：已确认 `showroom_hall_product` 仅含 `hall_id/product_id/display_order`，需要增加布局坐标列并扩展 API 契约。
- 2026-06-06：已为 `showroom_hall_product` 增加 `layout_x/layout_y/layout_width/layout_height`，并更新 MySQL 迁移、基线 schema 与 H2 测试表结构。
- 2026-06-06：已扩展展柜产品映射模型、DO、控制器与服务，新增 `/admin-api/showroom/hall/update-canvas-layout` 保存路径。
- 2026-06-06：已增加布局完整性校验，缺坐标、越界、宽高非法、重叠或未铺满均 fail-fast；历史空坐标读取按产品数量生成默认平均布局。
- 2026-06-06：后端目标单测、SQL 契约测试和前端真实 E2E 均已通过；测试租户认证读回 `tenantId=122`、`hall_05`、`mappingCount=10`。
