# Database Schema Evidence

## Data Change Goal

修复测试服务器 `tenant_id=1` 下手动重排保护任务缺少工作站/产线绑定的问题，限制到已由第三方导入产生正式报工的目标任务。

追加修复测试服务器排产员工作台工序列表中 `Z2772/Z2510/Z2775/Z2971` 班次产能为 0 的工作站主数据缺口，限制到本地存在且测试服缺失的同租户同 `process_id` 工作站资源链路。

再次追加：将测试服剩余 8 个临时工作站调整为本机同工序正式工作站口径，保留测试服主键以保护既有报工/任务引用。

全量追加：按测试服当前 WIP 工序清单继续与本机同 `route_code + process_code + process_id` 对比，修复剩余 14 条工作站/资源/产能不一致。

## Engine And Target

- Database engine: MySQL 8 in remote Docker container `intruoyi-mysql`.
- Target database: `ruoyi-vue-pro` on test server `172.30.30.58`.
- Migration tool: 不新增迁移；这是测试服精确数据修复。

## Affected Entities

- `mes_pro_task`: 目标任务的 `workstation_id`。
- `mes_md_workstation`: 目标测试工作站的 `production_line_id`，仅在缺失且能确定正式测试产线时更新。
- `mes_md_workstation`: 追加插入本地对应正式工作站 `922725/922726/922727/922731`。
- `mes_md_workstation_machine`: 追加插入上述工作站的设备绑定 `610/611/612/613/614/615/618`。
- `mes_dv_machinery_process`: 追加插入上述设备与工序的小时产能 `954/955/956/957/958/959/962`。
- `mes_md_workstation`: 更新临时工作站 `922061/922067/922062/922073/922059/922072/922075/922065` 的编码、名称、车间、产线、班次小时和备注为本机同工序口径。
- `mes_md_workstation_machine`: 为上述临时工作站补入本机同工序设备绑定。
- `mes_dv_machinery_process`: 为上述临时工作站涉及设备补入本机同工序小时产能。
- `mes_md_workstation`: 全量追加更新临时工作站 `922074/922069/922070/922071/922068/922060/922064/922063/922066/922058`，并插入缺失工作站 `922724/922741/922747/922748`。
- `mes_md_workstation_machine`: 全量追加插入设备绑定 `609/621/622/623/624/625/626/628/629/630/631/635/636/637/638/639/640/641/642/643/644/646/647`。
- `mes_dv_machinery_process`: 全量追加插入设备工序产能 `953/965/966/967/968/969/970/972/973/974/975/979/980/981/982/983/984/985/986/987/988/990/991`。

## Data Safety Analysis

- 只允许更新截图对应且已确认有正式报工的任务。
- 任务工作站必须来自同一任务正式报工中的单一 `feedback.workstation_id`。
- 产线绑定来自测试服租户 1 现有启用生产线 `900040/AUTO-LINE-01`；该产线引用的车间 `900011` 和日历计划 `900030` 均存在且启用。
- 每个写入语句必须有精确主键范围和行数断言。
- 追加同步前确认测试服目标工作站 ID/编码、工作站设备绑定 ID、设备工序产能 ID/组合键均无冲突；确认目标设备、工序、车间、产线均存在且未删除。
- 临时工作站调整前确认这些主键存在既有正式引用，因此禁止删除、禁用或改主键。
- 全量追加仅以测试服当前 WIP 的 26 个工序为对齐目标，本机额外存在的 14 条 WIP 记录不反向插入测试服。
- 写入前核对目标字符列 collation 为 `utf8mb4_unicode_ci`；正式事务使用同 collation 临时表，并用 UTF-8 HEX 写入中文字段，避免 `Illegal mix of collations` 和中文乱码。

## Rollback Plan

- 写入前记录目标任务原始 `workstation_id` 和目标工作站原始 `production_line_id`。
- 如验证失败，按记录快照将目标任务和工作站恢复到原值。
- 追加工作站同步如需回滚，按插入主键删除 `mes_md_workstation_machine.id IN (610,611,612,613,614,615,618)`、`mes_dv_machinery_process.id IN (954,955,956,957,958,959,962)`、`mes_md_workstation.id IN (922725,922726,922727,922731)`。
- 临时工作站调整如需回滚，按修复前快照恢复 8 个临时工作站原字段，并删除本次补入的工作站设备绑定与设备工序产能主键范围。
- 全量追加如需回滚，按修复前快照恢复 10 个临时工作站原字段，并删除本次插入的 4 个工作站、23 条工作站设备绑定和 23 条设备工序产能主键范围。

## BDD Scenarios

- BDD: protected feedback task can be replanned -> Given 正式报工已存在并带工作站, When 保护任务参与手动重排, Then 任务工作站和产线应可解析。
- BDD: process WIP shift capacity uses copied workstation -> Given 测试服四个当前路线工序班次产能为 0, When 同步本地同 `process_id` 工作站资源链路, Then 工作台应能计算非 0 班次产能。
- BDD: temporary workstations align with local capacity -> Given 测试服剩余 8 个工序使用临时工作站且班次产能低于本机, When 临时工作站资源链路调整为本机口径, Then 工作台 12 个截图工序班次产能应与本机一致。
- BDD: all current test WIP processes align with local workstation resources -> Given 测试服当前 WIP 工序仍存在剩余差异, When 按本机同工序正式工作站口径同步缺口, Then 测试服当前 WIP 工序资源与班次产能差异数应为 0。

## RED / GREEN

- RED: 目标任务 `925854/925855/925964/926006` 均有正式报工且报工工作站单一，但任务 `workstation_id` 为 `NULL`；目标工作站 `922058/922066/922068/922073` 产线为 `NULL`。
- GREEN: SQL 事务提交成功，`task_updated=4`、`workstation_updated=4`、`post_validation_ok`。
- GREEN: 修复后目标任务工作站分别为 `922066/922058/922073/922068`，目标工作站产线均为 `900040/AUTO-LINE-01`。
- GREEN: 真实页面重排复验通过，两个排产工单 `replanApply` 均返回 `applied=true` 且 `blockingIssueCount=0`。
- RED: 测试服 `process_id=922920/922921/922919` 无启用未删除工作站，`process_id=922925` 仅有已删除工作站；本地存在对应工作站 `922726/922727/922725/922731`。
- GREEN: 追加同步事务提交成功，插入工作站 4 行、工作站设备绑定 7 行、设备工序小时产能 7 行。
- GREEN: 工作台产能 SQL 复验 `Z2772=420.000000`、`Z2510=2340.000002`、`Z2775=270.000003`、`Z2971=254.999997`。
- RED: 测试服剩余 8 个目标工序仍使用临时工作站，班次小时 7.00，导致班次产能为本机 2/3 左右。
- GREEN: 临时工作站调整事务提交成功，保留既有引用 9 条，更新工作站 8 行，插入工作站设备绑定 14 行、设备工序小时产能 14 行。
- GREEN: 工作台 12 个截图工序班次产能与本机一致，均按 `shift_hours=10.50` 和本机同工序资源链路计算。
- RED: 全量 WIP 对比发现测试服 26 条当前 WIP 中仍有 14 条不一致，集中在 `Z3710/Z5200/Z2972/Z2973/Z2974/Z2550/Z2580/Z2490/Z2774/Z2773/Z3850/Z2560/Z5600/Z2620`。
- GREEN: 全量追加事务完成，更新工作站 10 行，插入工作站 4 行、工作站设备绑定 23 行、设备工序小时产能 23 行。
- GREEN: 全量 WIP 后验 `TEST_WIP_ROWS=26`、`LOCAL_WIP_ROWS=40`、`MISMATCH_COUNT=0`。

## Migration Verification

- 不新增迁移；测试服数据修复通过事务断言、后验 SQL 和真实页面 E2E 验证。
- 后验阻断扫描：目标工单范围内 `受保护任务未绑定工作站` / `受保护任务未绑定产线` 计数为 0。
- 追加工作站同步不新增迁移；通过事务断言、目标行计数和工作台产能 SQL 复验。
- 临时工作站调整不新增迁移；通过事务断言、引用保留计数、目标行计数和 12 个工作台工序产能 SQL 复验。
- 全量追加不新增迁移；通过 schema/collation 核对、失败事务零落库后验、正式事务目标行计数和全量 WIP 差异 SQL 复验。

## Blockers

- 无业务阻塞。提交/推送未执行，原因是当前工作区已有大量非本任务脏改动。
