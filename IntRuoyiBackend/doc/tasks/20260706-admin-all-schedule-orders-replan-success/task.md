# 芋道源码/admin 全量排产工单重排成功

## 任务目标
- 使用本机真实环境 `http://localhost:8081` 与 `芋道源码/admin`。
- 对当前所有未完成排产工单执行真实手动重排路径。
- 按真实阻断逐项补齐缺失基础数据，目标是最终应用重排成功。

## 里程碑
1. 建立 admin 租户当前排产工单全量清单与状态分布。- 已完成。
2. 对所有当前未完成排产工单执行重排预览，定位真实阻断。- 已完成。
3. 对可按默认值补齐的缺口执行最小范围补齐，并记录 SQL/API 回查证据。- 已完成。
4. 用 `芋道源码/admin` 真实链路执行应用重排，验证成功。- 已完成。
5. 清理临时产物并提交本任务文档。- 待提交。

## 预期验证
- 官方登录预检进入 `/mes/pro/schedule-order` 通过。
- 当前未完成排产工单清单可追溯到 `tenant_id=1`。
- 重排预览无阻断并返回 `calendarContextToken`。
- 应用重排接口成功，返回生成/删除/保留任务统计。
- 只读数据库核对生成任务最早开始时间与任务数量。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。修复 `startTime` 字符串反序列化根因，并让手动重排使用排产工单快照承接当前路线已删除但仍有剩余量的工序。
- 是否存在临时补丁或绕过：否。

## 经验门禁
- PowerShell/中文/SQL：命令显式 UTF-8，避免 PowerShell 文本污染。
- 登录/E2E：先用官方真实登录路径进入目标页面；最终验证使用 Playwright 真实用户路径。
- admin 租户写入：用户已明确要求在 `芋道源码/admin` 中用真实数据完成排产；写入前核对 `tenant_id=1`、记录主键和阻断原因。
- 无 fallback：缺少不可自动补齐的业务前置时必须明确阻断，不得伪造成功。

## 根因与修复
- `startTime` 以 `2026-07-06 00:00:00` 字符串提交时，公共 `TimestampLocalDateTimeDeserializer` 旧逻辑按 long 读取，字符串被解析为 `0`，后端得到 `1970-01-01 08:00:00`；已修复为显式支持日期时间字符串并保留非法字符串失败。
- admin 当前真实数据中，路线 `900025` 的第 24 道 `全检导丝` 当前路线工序已删除，但 9 个排产工单快照仍有该工序剩余量；旧逻辑只按当前路线生成任务，导致 `ACTIVE_TASK` 阻断；已修复为重排计算合并仍有剩余量的排产工单工序快照。
- 保留既有冻结工单、阻断问题、手工锁定/已报工/已完成任务保护、`calendarContextToken` 校验与 eDHR 前置校验，不增加降级路径。

## 最终验证
- 后端单测：`JsonUtilsTest#testTimestampLocalDateTimeDeserializer_shouldParseFormattedDateTimeString` PASS。
- 排产契约测试：`MesProAutoScheduleAlgorithmContractTest#preview_shouldScheduleRemainingSnapshotProcessWhenCurrentRouteRemovedIt+preview_shouldMatchScheduleOrderProcessByRouteSortWhenRouteProcessIdDrifted` PASS。
- 后端打包：`mvn -pl yudao-module-mes -am -Dmaven.test.skip=true package` PASS。
- 运行态：48081 后端使用更新后的 `yudao-common` 与 `yudao-module-mes` 嵌套 jar，`compress_type=0`，健康检查 HTTP 200。
- 真实 E2E：`芋道源码/admin` 选择当前 9 个排产工单，preflight PASS，preview `generatedTaskCount=432`、`blockingIssueCount=0`、`capacityMissingMessages=[]`、`calendarContextToken` 存在，apply `applied=true`。
- 数据库只读核验：9 个排产工单均有生成任务，`全检导丝(process_id=900378)` 均有任务承接；最早任务开始时间 `2026-07-06 08:00:00`；早于 `2026-07-06 00:00:00` 的任务数为 `0`。

## 当前状态
- 状态：completed，已完成并通过真实 E2E 与数据库只读核验。
