# 生产工单名称去除误追加工单编号执行日志

BDD: 去除工单名称误追加编码 -> Given 本机生产工单名称末尾误追加自身工单编号 / When 执行精确数据修正 / Then 仅命中这些异常记录，名称尾部编号被去掉，正常工单名称不受影响。

RED: `SELECT COUNT(*) FROM mes_pro_work_order WHERE name = CONCAT(TRIM(TRAILING code FROM name), code)` -> FAIL，存在 5 条名称末尾追加自身工单编号的数据。

GREEN: `UPDATE mes_pro_work_order ... WHERE name = prefix + ' ' + code` -> PASS，仅更新 5 条异常工单名称。

GREEN: `SELECT COUNT(*) ...` -> PASS，名称末尾追加自身工单编号的异常记录数为 0。
