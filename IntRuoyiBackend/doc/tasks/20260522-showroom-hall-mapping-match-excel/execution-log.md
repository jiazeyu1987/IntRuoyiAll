# 执行日志：展柜分配按 Excel 恢复为正式 166 条映射

BDD: 展柜管理里的每个展厅产品数量应与 Excel 一致 -> Given 当前后台 `showroom_hall_product` 只剩 8 条单产品映射 and Excel `产品明细` sheet 明确给出 8 个展厅共 166 条正式映射 / When 按 Excel 重建本地运行库 `showroom_hall_product` / Then 8 个展厅的产品数量必须恢复为 `26 / 28 / 27 / 17 / 10 / 20 / 11 / 27`，且后台真实页面产品数量列与维护产品弹窗不再只显示 1 条。

INFO: 当前任务只恢复本地运行库 `showroom_hall_product`；若匿名前台 `GET /showroom/display/app-config` 因 preview / narration live 资源缺失而失败，应作为已接受风险显式暴露，不做 fallback。

RED: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-hall-mapping-match-excel\scripts\verify_and_restore_hall_mapping.py --mode verify` -> FAIL，脚本对账结果显示 `db_hall_mapping_count=8`，8 个 hall 当前分布均为 `1`，且当前映射产品编码为 `E2E-PUBLISH-*`，与 Excel 期望 `166` 条映射完全不一致。

GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-hall-mapping-match-excel\scripts\verify_and_restore_hall_mapping.py --mode apply` -> PASS，脚本已按 Excel 事务性重建 `showroom_hall_product`，重建后 `db_hall_mapping_count=166`，hall 分布恢复为 `26 / 28 / 27 / 17 / 10 / 20 / 11 / 27`。

GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-hall-mapping-match-excel\scripts\verify_and_restore_hall_mapping.py --mode verify` -> PASS，脚本复核当前 hall-product 集合与 Excel 完全一致，映射产品编码样本已恢复为 `product_001 ... product_012`。

GREEN: `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "SELECT h.hall_code, COUNT(*) AS product_count FROM showroom_hall_product hp JOIN showroom_hall h ON h.id = hp.hall_id WHERE hp.deleted = 0 AND h.deleted = 0 GROUP BY h.hall_code, h.display_order ORDER BY h.display_order;"` -> PASS，8 个展厅产品数量返回 `26 / 28 / 27 / 17 / 10 / 20 / 11 / 27`。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-hall-mapping-match-excel open http://127.0.0.1:8081/showroom/hall --headed` + `run-code --filename D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-hall-mapping-match-excel\scripts\verify-showroom-hall-mapping-match-excel.mjs` -> PASS，真实测试租户 `122 / aoteman / admin123` 登录后，展柜管理列表 8 行产品数量列已显示 `26 / 28 / 27 / 17 / 10 / 20 / 11 / 27`，且 `hall_01` 的“维护产品”弹窗标题显示 `已选产品 26`，不再是单产品状态。

INFO: `curl.exe -i http://127.0.0.1:48081/showroom/display/app-config` -> `HTTP 200` 但响应体为 `{"code":401,"msg":"账号未登录","data":null}`；已按要求记录为当前已接受风险，不在本任务中补前台或匿名访问链路。
