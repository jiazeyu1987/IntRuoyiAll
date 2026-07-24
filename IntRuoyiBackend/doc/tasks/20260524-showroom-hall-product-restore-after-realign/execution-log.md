# 执行日志：恢复本地展柜产品完整映射

BDD: 展柜产品映射恢复到 Excel 事实源 -> Given 本地 `showroom_hall_product` 当前只剩 8 条单产品映射 / When 使用 Excel 事实源事务性重建 `showroom_hall_product` / Then 8 个展柜产品数量恢复为 `26 / 28 / 27 / 17 / 10 / 20 / 11 / 27`，总映射数为 `166`。

BDD: 恢复不掩盖 public display 资源缺口 -> Given 恢复后的多个产品可能缺少 live preview 或 narration / When 请求 public display 聚合接口 / Then 系统应继续按当前严格校验返回真实结果或明确失败，不得返回 mock、默认成功或 fallback 数据。

PRECHECK: previous same-repo task `20260524-showroom-hall-product-single-analysis` -> completed，不阻塞本次恢复。

RED: `python -X utf8 doc\tasks\20260522-showroom-hall-mapping-match-excel\scripts\verify_and_restore_hall_mapping.py --mode verify` -> FAIL，Excel 期望 `166` 条、分布 `26 / 28 / 27 / 17 / 10 / 20 / 11 / 27`；当前数据库只有 `8` 条，`hall_01` 到 `hall_08` 均为 `1` 条且都映射 `product_001`。

GREEN: `python -X utf8 doc\tasks\20260522-showroom-hall-mapping-match-excel\scripts\verify_and_restore_hall_mapping.py --mode apply` -> PASS，脚本已按 Excel 事务性重建 `showroom_hall_product`；恢复后 `db_hall_mapping_count=166`，分布为 `26 / 28 / 27 / 17 / 10 / 20 / 11 / 27`。

GREEN: `python -X utf8 doc\tasks\20260522-showroom-hall-mapping-match-excel\scripts\verify_and_restore_hall_mapping.py --mode verify` -> PASS，当前数据库映射与 Excel 完全一致，映射产品样本恢复为 `product_001 ... product_012`。

GREEN: SQL distribution check -> PASS，`showroom_hall_product` 有效记录数为 `166`，8 个展柜产品数量为 `26 / 28 / 27 / 17 / 10 / 20 / 11 / 27`。

GREEN: authenticated admin API `GET http://127.0.0.1:48081/admin-api/showroom/hall/page?pageNo=1&pageSize=20` -> PASS，租户 `1 / admin` 与测试租户 `122 / aoteman` 均返回 8 个展柜完整产品数量，`hall_01` 前 5 个产品为 `1,2,3,4,5`。

GREEN: public display probe `GET http://127.0.0.1:48081/showroom/display/website-config` -> PASS，返回 `HTTP 200` 且 body `code=0`，恢复完整映射后当前未触发 preview/narration 严格校验失败。

RED: first Playwright script run -> FAIL，复用旧脚本时 `page.waitForResponse` 等待 `/admin-api/showroom/hall/page` 响应超时；该失败是验证脚本时序问题，不是接口或页面数据失败。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-hall-restore-page run-code --filename doc\tasks\20260524-showroom-hall-product-restore-after-realign\scripts\verify-showroom-hall-restore-page.mjs` -> PASS，真实前端 `http://127.0.0.1:8081/showroom/hall` 列表显示 `26 / 28 / 27 / 17 / 10 / 20 / 11 / 27`，`hall_01` 维护产品弹窗显示 `已选产品 26`。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260524-showroom-hall-product-restore-after-realign\database-schema-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260524-showroom-hall-product-restore-after-realign --mode preview --worktree-closeout off` -> PASS，`status=ready`，delete/blocked/warnings 均为空。
