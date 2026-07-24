# Task: 同步球囊扩张压力泵工艺批记录路线数据

## 任务目标

- 从现有“工艺批记录路线”数据源重新同步“球囊扩张压力泵”相关批记录路线绑定数据。
- 只处理本机数据，目标限定为当前项目本地库与测试租户数据；不操作服务器或正式环境。
- 同步前做只读预检，确认源数据、目标缺口、目标租户和受影响表；同步后做断言式回查。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`；中文 SQL/命令必须显式 UTF-8，PowerShell 不使用 `&&`。
- 项目经验索引：已读取 `docs/experience-index.md`；本任务命中数据库写入、批记录路线、PowerShell 门禁。
- 数据库写入：执行写库前必须记录 `GREEN: experience-preflight -> PASS`，并使用只读预检结果限定写入范围。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；优先复用已有 SQL/契约测试并以后置断言验证数据恢复。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 压力泵批记录路线数据恢复 -> Given “球囊扩张压力泵”工艺路线存在批记录路线源数据 / When 执行同步 / Then 工序设置可重新显示对应批记录表单与填写人链接。
- BDD: 同步范围受控 -> Given 数据库还有其他工艺路线 / When 执行同步 / Then 只影响“球囊扩张压力泵”相关路线、工序、批记录绑定。

## 里程碑

- [x] M1：建立任务记录并读取经验门禁。
- [x] M2：定位并验证现有同步 SQL/测试。
- [x] M3：只读预检源数据与目标缺口。
- [x] M4：执行同步并后置回查。
- [x] M5：记录验证、closeout preview 与提交边界。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_mes_rt000006_batch_record_mapping_sql.py -q`
- 本机 MySQL 只读预检与同步后回查：确认 RT000006 / 球囊扩张压力泵批记录绑定恢复。

## 当前状态

COMPLETED_WITH_COMMIT_BLOCKER：已重新执行 `sql/mysql/20260709_mes_rt000006_batch_record_mapping.sql`，并进一步按用户截图排查测试租户页面数据。根因是 tenant=1 的 `route_id=922067 / RT000006 / 球囊扩张压力泵` 已有 14 个表单绑定与 42 条填写人规则，但测试租户 tenant=122 页面命中的是 `route_id=922060 / RT000006 / E2E-WORD-1783433099306`，该路线已有 14 个表单绑定但缺少 3 个压力泵填写员角色和 42 条填写人规则。已用稳定 `role.code` 与 `system_role_category.code=batch-record` 补齐 tenant=122 的 `压力泵生产填写员 / 压力泵质量填写员 / 压力泵设备填写员` 三个角色，并补齐 RT000006 14 道工序的 `FILL / QUALITY_FILL / EQUIPMENT_FILL` 规则；回查确认 `route_process_rows=14`、`batch_bindings=14`、`matched_reports=14`、`enabled_fill_rules=42`。当前仓库存在大量既有脏改，暂不提交以避免夹带无关改动。
