# 任务：恢复本地展柜产品完整映射

## Task Goal

- 将本地运行库 `ruoyi-vue-pro.showroom_hall_product` 从当前 `8 halls / 1 product` 的最小 public display 验证集恢复为 Excel 事实源中的完整展柜产品映射。
- 目标分布恢复为 `26 / 28 / 27 / 17 / 10 / 20 / 11 / 27`，总计 `166` 条有效映射。
- 保持 fail-fast：只恢复展柜产品关系，不为 `website-config` 增加 fallback，也不伪造 preview/narration live 资源。

## Scope

- 本地 Docker MySQL 容器 `int-ruoyi-mysql`
- 数据库 `ruoyi-vue-pro`
- 表 `showroom_hall_product`
- 事实源：`D:\ProjectPackage\Int\IntRuoyi\resource\展厅产品与描述清单.xlsx`
- 复用脚本：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-hall-mapping-match-excel\scripts\verify_and_restore_hall_mapping.py`
- 本任务记录：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-hall-product-restore-after-realign\**`

## Non-Scope

- 不修改 Java / Vue 业务代码。
- 不修改数据库 schema。
- 不修改 `showroom_product`、`showroom_product_revision`、`showroom_hall`、preview asset、narration 或 release 表。
- 不修复或绕过 `website-config` 可能暴露的 preview/narration 严格校验失败。

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-hall-product-single-analysis\task.md`
- Status before this task: `completed`
- Impact: 前序任务已定位当前单产品状态的根因，不阻塞本次数据恢复。

## BDD Scenario

- BDD: 展柜产品映射恢复到 Excel 事实源 -> Given 本地 `showroom_hall_product` 当前只剩 8 条单产品映射 / When 使用 Excel 事实源事务性重建 `showroom_hall_product` / Then 8 个展柜产品数量恢复为 `26 / 28 / 27 / 17 / 10 / 20 / 11 / 27`，总映射数为 `166`。
- BDD: 恢复不掩盖 public display 资源缺口 -> Given 恢复后的多个产品可能缺少 live preview 或 narration / When 请求 public display 聚合接口 / Then 系统应继续按当前严格校验返回真实结果或明确失败，不得返回 mock、默认成功或 fallback 数据。

## Milestones

- [x] M1：建立任务文档、执行日志和数据库证据骨架。
- [x] M2：运行只读 verify，记录当前 8 条映射与 Excel 不一致的 RED。
- [x] M3：执行 apply，事务性重建 `showroom_hall_product`。
- [x] M4：复核脚本、SQL 和管理端接口，确认恢复为 166 条。
- [x] M5：记录 public display 风险、closeout 预览并提交本任务记录。

## Expected Verification

- `python -X utf8 doc\tasks\20260522-showroom-hall-mapping-match-excel\scripts\verify_and_restore_hall_mapping.py --mode verify`
- `python -X utf8 doc\tasks\20260522-showroom-hall-mapping-match-excel\scripts\verify_and_restore_hall_mapping.py --mode apply`
- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "<hall distribution query>"`
- authenticated `GET http://127.0.0.1:48081/admin-api/showroom/hall/page?pageNo=1&pageSize=20`
- public display probe, result recorded exactly.
- `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260524-showroom-hall-product-restore-after-realign\database-schema-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260524-showroom-hall-product-restore-after-realign --mode preview --worktree-closeout off`

## Current Status

- Status: `completed`
- 当前阶段：任务已完成；本地展柜产品映射已恢复为 Excel 事实源中的 `166` 条。

## Verification Result

- RED verify -> FAIL，恢复前本地 `showroom_hall_product` 只有 `8` 条，每个展柜只映射 `product_001`。
- GREEN apply -> PASS，已按 Excel 事务性重建 `showroom_hall_product`。
- GREEN verify -> PASS，恢复后 `db_hall_mapping_count=166`，分布为 `26 / 28 / 27 / 17 / 10 / 20 / 11 / 27`。
- GREEN SQL -> PASS，`showroom_hall_product` 有效记录数为 `166`，8 个展柜分布正确。
- GREEN admin API -> PASS，租户 `1 / admin` 与测试租户 `122 / aoteman` 的 `/admin-api/showroom/hall/page` 均返回完整产品数量。
- GREEN public display probe -> PASS，`GET http://127.0.0.1:48081/showroom/display/website-config` 返回 `code=0`。
- GREEN Playwright -> PASS，真实前端 `http://127.0.0.1:8081/showroom/hall` 列表显示完整数量，`hall_01` 维护产品弹窗显示 `已选产品 26`。

## Cleanup Keep

- `doc/tasks/20260524-showroom-hall-product-restore-after-realign/task.md`
- `doc/tasks/20260524-showroom-hall-product-restore-after-realign/execution-log.md`
- `doc/tasks/20260524-showroom-hall-product-restore-after-realign/database-schema-evidence.md`
- `doc/tasks/20260524-showroom-hall-product-restore-after-realign/scripts/verify-showroom-hall-restore-page.mjs`
