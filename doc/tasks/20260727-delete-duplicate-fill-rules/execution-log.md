# Execution Log

## User Intent

- 用户要求：删除当前失败批记录表单填写人规则中的其他 86 条，保留 1 条正确规则。

## BDD

- `BDD: 重复填写人规则清理 -> Given 当前租户某批记录报表和版本下存在 87 条启用的表单级 FILL 规则且业务确认其中 1 条正确 When 执行受控数据修复 Then 仅删除其余 86 条，保留规则内容不变，且规则读取接口不再因多结果异常失败。`

## Preflight

- 已读取 `docs/database-rules.md`。
- 已读取 `docs/task-closeout-rules.md`。
- 已读取 `docs/powershell-encoding.md`。
- 已读取 `docs/powershell-memory.md`。
- 已读取 `docs/experience-index.md`，命中业务数据修复和批记录版本治理门禁。
- 当前尚未执行任何写入或删除。
- 真实表：`mes_pro_edhr_process_form_permission_rule`，MySQL 8/InnoDB，本地 Docker 数据库 `int-ruoyi-mysql/ruoyi-vue-pro`。
- 目标唯一范围：`tenant_id=1`、`route_process_id=0`、`batch_record_report_id=1d05410f1d3140c5b8aa6786887ae69c`、`batch_record_version_id=130`、`rule_type=FILL`、`enabled=1`、`deleted=0`。
- 报表映射：产品“球囊扩张压力泵”，表单“粗洗工序生产记录”，版本 `V14.0`，版本 ID `130`，定义 ID `47`。
- `RED: 只读范围计数与接口日志 -> FAIL, 目标范围存在 87 条启用规则，日志出现 TooManyResultsException: expected one result (or null) to be returned by selectOne(), but found: 87`。
- 87 条记录均为 `CODX_VFC_ASSIST_*` / `E2E辅助行*`，创建时间会被 `edhr-visual-fill-config-real-flow.e2e.js` 的恢复逻辑重写；已等待冲突中的 Playwright 进程自然结束，未强停并行任务。
- 版本限制已确认：Mapper 通过 `batch_record_version_id = 130` 查询当前版本；本次 SQL 也固定相同版本条件。

## Retention Decision

- 当前 87 条中不存在正式表单级 `ALL` 规则，不能任意保留某条单元格测试规则。
- 当前 V14.0 的 `source_version_id=118`，对应 V13.0；V13.0 的粗洗规则为 `ALL / ROLE / 910405`。
- 同一 V14.0 的其余 14 个表单均为 `ALL / ROLE / 910405`，角色名称“压力泵生产1”，启用成员为“王歆、任丹”。
- 因此保留当前范围中 `scope_key=CODX_VFC_ASSIST_1` 的一条物理记录作为载体，事务内将其规范为 `ALL / ROLE / 910405`，清空单元格范围，并删除同范围其余 86 条。

## Recovery Plan

- 删除前使用 `mysqldump --no-create-info` 导出目标 87 行到任务目录，导出后核对 87 条 `INSERT` 值记录。
- 若事务校验失败，事务内 `ROLLBACK`，不留下部分删除。
- 若提交后需要恢复，先删除目标范围当前规则，再从任务快照导入 87 行；恢复前后均按目标租户、报表 ID、版本 ID 和规则类型核对行数。

## Verification

- 已完成 schema、租户、报表 ID、版本和保留规则依据核对。
- 待补充：快照文件校验、事务删除影响行数、保留行内容和接口/页面复验。

## Current Status

in_progress

## Blockers

- 当前无保留规则歧义；若目标范围不再严格为 87 条、保留载体不唯一或并行 E2E 再次写入，则事务必须失败并停止。
