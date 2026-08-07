# Execution Log

## User Intent

- 在上一轮设备台账比对结论基础上，用户要求新增 `C01017 / 撤压机`。
- 用户要求将当前光固机编码 `A05059` 改为 `A05075`。
- 用户要求将当前箱型干燥机编码 `B09041` 改为 `B04091`。
- 变更范围限定为本地 `int_main` 运行库的租户 `122`，不修改其它租户或其它设备。

## BDD Scenarios

BDD: 新增撤压机 -> Given 租户 122 不存在未删除编码 C01017 且正式设备类型和车间已核对, When 新增设备 C01017 / 撤压机, Then 台账存在唯一正式记录；设备条码配置缺失时保持无条码，不伪造记录。

BDD: 光固机编码修正 -> Given 租户 122 的设备 A05059 唯一存在且 A05075 不存在, When 将该设备编码改为 A05075, Then 原设备 ID 和非编码业务字段不变，设备工序关系和全局 MES 工序目录不再残留 A05059。

BDD: 箱型干燥机编码修正 -> Given 租户 122 的设备 B09041 唯一存在且 B04091 不存在, When 将该设备编码改为 B04091, Then 原设备 ID 和非编码业务字段不变，设备工序关系和全局 MES 工序目录不再残留 B09041。

BDD: 任一前置条件失败时整体回滚 -> Given 三项变更在同一事务中执行, When 任一唯一性、关联、租户或影响行数断言失败, Then 三项变更均不提交且现有数据保持原状。

## Command Intent And Evidence

- READ: 已读取 `docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/database-rules.md` 和 `docs/experience-index.md` 适用门禁。
- GIT BASELINE: 任务开始时 `int_main` 比 `origin/int_main` 领先 1 个提交，且存在并发任务日志改动；并发提交 `9c7507e1d` 随后保存了该日志和另一并发任务的初始文档。该提交不作为本任务实现提交，本任务未修改其内容。
- SCHEMA: 上一轮只读核对已确认本地运行库为 Docker `int-ruoyi-mysql` 的 `ruoyi-vue-pro`，正式设备表为 `mes_dv_machinery`；本任务写入前将重新核对 schema、目标行和引用关系。
- SCHEMA: `mes_dv_machinery` 当前无设备编码唯一键，正式服务按租户校验唯一性；事务将按租户、删除标记和精确旧编码加锁并断言目标编码不存在。
- SOURCE: 光固机为 `id=202 / A05059 / 光固机`，箱型干燥机为 `id=198 / B09041 / 箱型干燥机`；两者均属于租户 `122`，设备类型 `5 / DEFAULT-MACHINERY-TYPE`、车间 `900066 / AUTO-WSHOP`、状态 `2 / 生产中`。
- RELATION: 两台设备各有 1 条活动 `mes_dv_machinery_process` 和 1 条按设备 ID 关联的 `mes_md_workstation_machine`；点检、保养、维修、QA 规程、PQC 明细、组长范围和资源调整均无目标设备活动记录。
- CATALOG: 全局只读 MES 工序目录中 `9003131004/9003132004` 使用 `B09041`，`9003131008/9003132008` 使用 `A05059`；它们属于当前页面正式读模型，必须随编码修正同步更新。
- BARCODE: 租户 `122` 没有 `biz_type=400` 的设备条码配置，两台现有设备也没有条码记录；正式 `autoGenerateBarcode` 在配置缺失时直接不生成，本任务保持该正式行为。
- SNAPSHOT: 变更前租户 `122` 未删除设备共 `49` 条；`id=198` 非编码字段 MD5 为 `d03fcab9d173520de79485ab0f4d678e`，`id=202` 为 `9b04a596ce68241a70a32d2a0904d405`。
- CONCURRENCY: 当前存在另一任务的 Playwright `lossreason0807` 页面写入进程，但其目标为损耗原因，不涉及设备台账或本任务目标 ID；本任务不停止、不修改该进程，并使用事务锁和影响行数断言防止目标数据并发漂移。
- RED: desired-state precheck -> FAIL as expected，租户 `122` 的目标编码 `C01017/A05075/B04091` 为 `0` 条，旧编码 `A05059/B09041` 为 `2` 条。
- RED: first transaction attempt -> FAIL as designed，`apply.sql` 第 201 行影响行数断言触发；原因是先读取 `LAST_INSERT_ID()` 的 `SET` 语句将后续 `ROW_COUNT()` 变为 `0`，不是业务前置条件失败。
- ROLLBACK: first transaction attempt -> PASS，连接在 `COMMIT` 前因断言错误退出；复核设备台账仍为 `A05059/B09041`、目标编码仍为 `0` 条、设备工序关系和全局 MES 工序目录仍为旧编码、租户未删除设备仍为 `49` 条。仅 MySQL 自增序列按数据库语义前进到 `980007`，没有生成业务记录。
- FIX: 将插入后的 `ROW_COUNT()` 保存移到 `LAST_INSERT_ID()` 读取之前，保持同一 fail-fast 事务设计。

## Milestone Updates

- M1 completed：任务记录、BDD、预期验证、设计约束和并发 Git 基线已记录。
- M2 completed：目标设备、关联表、全局 MES 工序目录、条码配置和新增设备必填元数据已核对。
- M3 completed：变更前精确行、关联计数、非编码字段 MD5、租户总数和回滚条件已记录。
- M4 in_progress：准备执行单事务数据修正。

## Blockers

- 当前无已确认 blocker；业务数据尚未修改。
