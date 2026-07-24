# Execution Log：同步工艺路线详情默认批记录名称

BDD: 工艺路线详情显示清理后的默认批记录名称 -> Given `jimu_report.name` 已去除 `电子批记录[A]-表x-` 前缀但 MES 批记录元数据仍有旧名称 / When 打开工艺路线详情 / Then 默认批记录列显示 MES 元数据同步后的干净名称。

BDD: 保持无前缀名称不变 -> Given 部分批记录在 MES 元数据中已经没有该前缀 / When 执行同步 / Then 这些名称保持不变。

- SETUP: 创建任务文档 -> PASS，任务目录 `doc/tasks/20260608-edhr-route-detail-report-name-sync`。

- ROOT CAUSE: 工艺路线详情后端 `MesProRouteProcessController` 通过 `MesProBatchRecordReportMapper.selectListByReportIds()` 读取 `mes_pro_batch_record_report.report_name` 填充 `batchRecordReportName`，不是直接读取 `jimu_report.name`。

RED: `python -X utf8` 对比 `jimu_report` 与 `mes_pro_batch_record_report` 同一 `report_id` 的 EBR 名称 -> FAIL，`joined_ebr_reports=30`、`jimu_prefix_count=0`、`mes_prefix_count=29`、`name_mismatch_count=29`。

GREEN: `python -X utf8` 事务同步 `mes_pro_batch_record_report.report_name = jimu_report.name` -> PASS，`matched_count=29`、`updated_count=29`，只更新 `report_name` 与 `update_time`。

GREEN: `python -X utf8` 数据库联查验证 -> PASS，`joined_ebr_reports=30`、`jimu_prefix_count=0`、`mes_prefix_count=0`、`name_mismatch_count=0`、`tenant1_route_process_count=21`、`tenant1_route_bound_report_rows=15`、`tenant1_route_bound_prefixed_report_count=0`。

GREEN: `python -X utf8` 登录本地 `tenant-id=1 / admin` 并请求 `GET http://127.0.0.1:48081/admin-api/mes/pro/route-process/list-by-route?routeId=900022` -> PASS，接口返回 `routeProcessCount=21`、`boundReportCount=15`、`prefixedReportCount=0`。

API 返回的默认批记录名称：

| sort | processCode | batchRecordReportCode | batchRecordReportName |
| --- | --- | --- | --- |
| 1 | B010 | EBR_TN1_A_T01 | 产品信息 |
| 2 | B020 | EBR_TN1_A_T02 | 粗洗工序生产记录 |
| 3 | B030 | EBR_TN1_A_T03 | 精洗工序生产记录 |
| 4 | B040 | EBR_TN1_A_T04 | 清洗工序生产记录 |
| 5 | B050 | EBR_TN1_A_T05 | 清洁工序生产记录 |
| 6 | B140 | EBR_TN1_A_T06 | 组装Ⅰ工序生产记录 |
| 7 | B060 | EBR_TN1_A_T07 | 光固Ⅰ工序生产记录 |
| 8 | B200 | EBR_TN1_A_T08 | 硅化Ⅰ工序生产记录 |
| 9 | B290 | EBR_TN1_A_T09 | 硅化Ⅱ工序生产记录 |
| 10 | B210 | EBR_TN1_A_T10 | 组装Ⅱ工序生产记录 |
| 16 | B230 | EBR_TN1_A_T11 | 检测工序生产记录 |
| 17 | B240 | EBR_TN1_A_T12 | 光固Ⅱ工序生产记录 |
| 18 | B250 | EBR_TN1_A_T13 | 单包装工序生产记录 |
| 19 | B280 | EBR_TN1_A_T14 | 中包装工序生产记录 |
| 21 | B320 | EBR_TN1_A_T15 | 大包装工序生产记录 |

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260608-edhr-route-detail-report-name-sync\bug-regression-evidence.md` -> PASS，bug regression evidence 合同校验通过。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260608-edhr-route-detail-report-name-sync\database-schema-evidence.md` -> PASS，database evidence 合同校验通过。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260608-edhr-route-detail-report-name-sync --mode preview` -> PASS，keep 为 `task.md` 与 `execution-log.md`，delete 为临时 `bug-regression-evidence.md` 与 `database-schema-evidence.md`，blocked/warnings 均为 none。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260608-edhr-route-detail-report-name-sync --mode apply` -> PASS，已删除临时 evidence 文件。
