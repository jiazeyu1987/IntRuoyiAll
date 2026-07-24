# 任务：同步工艺路线详情默认批记录名称

## 任务目标

修复电子批记录名称前缀清理后，工艺路线详情“默认批记录”列仍显示旧前缀的问题；将 MES 批记录报表元数据表中的名称与积木报表主表清理后的名称同步。

## 范围边界

- 作用对象为未删除电子批记录在 `mes_pro_batch_record_report.report_name` 中仍以 `电子批记录[A]-表x-` 开头的记录。
- 名称同步以同一 `report_id` 对应的 `jimu_report.name` 为准。
- 不修改报表编码、报表内容、路线绑定、工序、产品或资源配置。
- 不修改前端显示逻辑，不加入兜底名称。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。工艺路线详情接口读取 `mes_pro_batch_record_report.report_name`，本次同步该真实数据源。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 工艺路线详情显示清理后的默认批记录名称 -> Given `jimu_report.name` 已去除 `电子批记录[A]-表x-` 前缀但 MES 批记录元数据仍有旧名称 / When 打开工艺路线详情 / Then 默认批记录列显示 MES 元数据同步后的干净名称。
- BDD: 保持无前缀名称不变 -> Given 部分批记录在 MES 元数据中已经没有该前缀 / When 执行同步 / Then 这些名称保持不变。

## 里程碑

- [x] M1：确认路线详情接口取值来源和名称不一致的 RED 证据。
- [x] M2：事务同步 `mes_pro_batch_record_report.report_name`。
- [x] M3：验证 `tenant_id=1` 路线 `ROUTE-YXN.069.001.1001` 详情返回的 15 张默认批记录名称均无前缀。

## 预期验证

- RED 查询：确认 `jimu_report.name` 已清理但 `mes_pro_batch_record_report.report_name` 仍存在前缀。
- GREEN 查询：确认 `mes_pro_batch_record_report.report_name` 残留前缀数量为 0。
- 回归查询：确认 `tenant_id=1` 路线 `ROUTE-YXN.069.001.1001` 仍为 21 道工序，15 张默认批记录均可关联并显示无前缀名称。

## Current Status

completed；已同步 29 条 MES 批记录报表元数据名称，工艺路线详情接口返回名称已无 `电子批记录[A]-表x-` 前缀。

## Verification Result

- 根因：工艺路线详情列读取 `mes_pro_batch_record_report.report_name`，不是直接读取 `jimu_report.name`。
- RED：`jimu_report.name` 残留前缀为 0，但 `mes_pro_batch_record_report.report_name` 残留前缀为 29，名称不一致 29 条。
- GREEN：事务同步 29 条 `mes_pro_batch_record_report.report_name`，仅额外更新 `update_time`。
- REGRESSION：两张表 30 条 EBR 名称一致，残留前缀均为 0。
- API：本地 `tenant-id=1 / admin` 请求 `GET /admin-api/mes/pro/route-process/list-by-route?routeId=900022` 返回 21 道工序、15 张默认批记录、`prefixedReportCount=0`。
- EVIDENCE：bug regression 与 database evidence 合同校验均通过；收尾清理已删除临时 evidence 文件，保留 `task.md` 与 `execution-log.md`。

## Cleanup Candidates

- `doc/tasks/20260608-edhr-route-detail-report-name-sync/bug-regression-evidence.md`
- `doc/tasks/20260608-edhr-route-detail-report-name-sync/database-schema-evidence.md`
