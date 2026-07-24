# Execution Log：清理电子批记录报表名称前缀

BDD: 清理电子批记录名称前缀 -> Given 报表设计器中存在名称以 `电子批记录[A]-表x-` 开头的电子批记录 / When 执行名称清理 / Then 这些报表名称只保留实际表名部分。

BDD: 不影响无前缀名称 -> Given 部分电子批记录名称已经没有该前缀 / When 执行名称清理 / Then 这些名称保持不变。

- SETUP: 创建任务文档 -> PASS，任务目录 `doc/tasks/20260608-edhr-report-name-prefix-cleanup`。

- DIAGNOSIS: `python -X utf8` 初始只读预检 -> FAIL，SQL `LIKE ... ESCAPE` 写法不兼容当前 MySQL；未执行数据更新，改用 `code LIKE 'EBR%'` 重新预检。

RED: `python -X utf8` 查询未删除 `jimu_report` 且 `code LIKE 'EBR%'` 的报表名称 -> FAIL，期望不存在冗余前缀，实际 `active_ebr_reports=30`、`prefix_match_count=29`、`active_ebr_without_prefix_count=1`。

RED 映射：

| tenant_id | code | old_name | new_name |
| --- | --- | --- | --- |
| 1 | EBR_TN1_A_T01 | 电子批记录[A]-表1-产品信息 | 产品信息 |
| 1 | EBR_TN1_A_T02 | 电子批记录[A]-表2-粗洗工序生产记录 | 粗洗工序生产记录 |
| 1 | EBR_TN1_A_T04 | 电子批记录[A]-表4-清洗工序生产记录 | 清洗工序生产记录 |
| 1 | EBR_TN1_A_T05 | 电子批记录[A]-表5-清洁工序生产记录 | 清洁工序生产记录 |
| 1 | EBR_TN1_A_T06 | 电子批记录[A]-表6-组装Ⅰ工序生产记录 | 组装Ⅰ工序生产记录 |
| 1 | EBR_TN1_A_T07 | 电子批记录[A]-表7-光固Ⅰ工序生产记录 | 光固Ⅰ工序生产记录 |
| 1 | EBR_TN1_A_T08 | 电子批记录[A]-表8-硅化Ⅰ工序生产记录 | 硅化Ⅰ工序生产记录 |
| 1 | EBR_TN1_A_T09 | 电子批记录[A]-表9-硅化Ⅱ工序生产记录 | 硅化Ⅱ工序生产记录 |
| 1 | EBR_TN1_A_T10 | 电子批记录[A]-表10-组装Ⅱ工序生产记录 | 组装Ⅱ工序生产记录 |
| 1 | EBR_TN1_A_T11 | 电子批记录[A]-表11-检测工序生产记录 | 检测工序生产记录 |
| 1 | EBR_TN1_A_T12 | 电子批记录[A]-表12-光固Ⅱ工序生产记录 | 光固Ⅱ工序生产记录 |
| 1 | EBR_TN1_A_T13 | 电子批记录[A]-表13-单包装工序生产记录 | 单包装工序生产记录 |
| 1 | EBR_TN1_A_T14 | 电子批记录[A]-表14-中包装工序生产记录 | 中包装工序生产记录 |
| 1 | EBR_TN1_A_T15 | 电子批记录[A]-表15-大包装工序生产记录 | 大包装工序生产记录 |
| 122 | EBR_TN122_A_T01 | 电子批记录[A]-表1-产品信息 | 产品信息 |
| 122 | EBR_TN122_A_T02 | 电子批记录[A]-表2-粗洗工序生产记录 | 粗洗工序生产记录 |
| 122 | EBR_TN122_A_T03 | 电子批记录[A]-表3-精洗工序生产记录 | 精洗工序生产记录 |
| 122 | EBR_TN122_A_T04 | 电子批记录[A]-表4-清洗工序生产记录 | 清洗工序生产记录 |
| 122 | EBR_TN122_A_T05 | 电子批记录[A]-表5-清洁工序生产记录 | 清洁工序生产记录 |
| 122 | EBR_TN122_A_T06 | 电子批记录[A]-表6-组装Ⅰ工序生产记录 | 组装Ⅰ工序生产记录 |
| 122 | EBR_TN122_A_T07 | 电子批记录[A]-表7-光固Ⅰ工序生产记录 | 光固Ⅰ工序生产记录 |
| 122 | EBR_TN122_A_T08 | 电子批记录[A]-表8-硅化Ⅰ工序生产记录 | 硅化Ⅰ工序生产记录 |
| 122 | EBR_TN122_A_T09 | 电子批记录[A]-表9-硅化Ⅱ工序生产记录 | 硅化Ⅱ工序生产记录 |
| 122 | EBR_TN122_A_T10 | 电子批记录[A]-表10-组装Ⅱ工序生产记录 | 组装Ⅱ工序生产记录 |
| 122 | EBR_TN122_A_T11 | 电子批记录[A]-表11-检测工序生产记录 | 检测工序生产记录 |
| 122 | EBR_TN122_A_T12 | 电子批记录[A]-表12-光固Ⅱ工序生产记录 | 光固Ⅱ工序生产记录 |
| 122 | EBR_TN122_A_T13 | 电子批记录[A]-表13-单包装工序生产记录 | 单包装工序生产记录 |
| 122 | EBR_TN122_A_T14 | 电子批记录[A]-表14-中包装工序生产记录 | 中包装工序生产记录 |
| 122 | EBR_TN122_A_T15 | 电子批记录[A]-表15-大包装工序生产记录 | 大包装工序生产记录 |

GREEN: `python -X utf8` 事务更新匹配前缀的 `jimu_report.name` -> PASS，`matched_count=29`、`updated_count=29`，只更新 `name` 与 `update_time`。

GREEN: `python -X utf8` 回归查询未删除 EBR 报表和 `tenant_id=1` 路线 `ROUTE-YXN.069.001.1001` -> PASS，`active_ebr_reports=30`、`prefix_match_count=0`、`tenant1_route_process_count=21`、`tenant1_route_bound_report_rows=15`、`tenant1_route_bound_prefixed_report_count=0`。

INFO: 不带前缀的 `tenant_id=1 / EBR_TN1_A_T03 / 精洗工序生产记录` 保持不变。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260608-edhr-report-name-prefix-cleanup\database-schema-evidence.md` -> PASS，数据库证据合同校验通过。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260608-edhr-report-name-prefix-cleanup --mode preview` -> PASS，keep 为 `task.md` 与 `execution-log.md`，delete 为临时 `database-schema-evidence.md`，blocked/warnings 均为 none。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260608-edhr-report-name-prefix-cleanup --mode apply` -> PASS，已删除临时 `database-schema-evidence.md`。
