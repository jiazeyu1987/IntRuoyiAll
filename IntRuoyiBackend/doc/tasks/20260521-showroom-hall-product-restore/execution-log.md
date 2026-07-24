# 执行日志：恢复本地展厅产品映射

BDD: showroom halls should restore historical product mappings -> Given 当前本地运行库的 8 个展厅都被临时映射到单一产品 172 / When 从 `20260520_113715` 历史快照恢复 `showroom_hall_product` / Then hall 1..8 应恢复为历史的 166 条映射分布而不是继续只显示单产品

BDD: restore should fail fast on missing prerequisites -> Given 恢复 SQL 依赖 hall `1..8` 与 product `1..166` 当前仍存在 / When 前置主数据不完整 / Then 恢复必须直接失败并报告阻塞，而不能静默写入部分映射

BDD: restore scope must stay local-only -> Given 用户当前只要求恢复当前本地展厅 / When 执行恢复 / Then 只修改本地运行库 `127.0.0.1:23306/ruoyi-vue-pro`，不改测试服务器

RED: `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "SELECT COUNT(*) AS hall_product_rows FROM showroom_hall_product; SELECT hall_id, COUNT(*) AS hall_count, MIN(product_id) AS min_product_id, MAX(product_id) AS max_product_id FROM showroom_hall_product GROUP BY hall_id ORDER BY hall_id;"` -> FAIL，当前只有 `8` 条映射，且 `hall_id 1..8` 全部只映射到 `product_id = 172`。
INFO: 首次执行恢复 SQL -> FAIL，递归 CTE 版本在当前 MySQL 执行上下文里报语法错误，已改为显式 `INSERT ... VALUES` 版本后重试。
GREEN: `Get-Content -Encoding utf8 -Raw D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260521-showroom-mapping-recovery-compare\restore-showroom-hall-product-from-20260520_113715.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro` -> PASS，本地运行库映射恢复写入成功。
GREEN: `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "SELECT COUNT(*) AS hall_product_rows FROM showroom_hall_product; SELECT hall_id, COUNT(*) AS hall_count, MIN(product_id) AS min_product_id, MAX(product_id) AS max_product_id FROM showroom_hall_product GROUP BY hall_id ORDER BY hall_id;"` -> PASS，`showroom_hall_product = 166`，hall 分布恢复为 `26 / 28 / 27 / 17 / 10 / 20 / 11 / 27`。
GREEN: 样例映射核对 -> PASS，`hall_id=1` 已恢复 `product_id 1..26`，`hall_id=2` 已恢复 `product_id 27..54`。
INFO: `GET http://127.0.0.1:48081/showroom/display/app-config` -> FAIL，返回 `SHOWROOM_TARGET_NOT_FOUND: live product ZH narration not found`，说明 hall-product 恢复完成，但 consumer 所需 live product narration 仍缺。
INFO: 历史快照 `20260520_113715` 可恢复源核对 -> PASS，`product 1..166` 对应的 `PUBLISHED` product preview / ZH narration / EN narration 均为 `0`，因此当前没有可直接回放的完整 consumer 资源源头。
GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-hall-product-restore --mode preview` -> PASS，preview 结果 `ready`，仅保留当前任务 `task.md` 与 `execution-log.md`。
