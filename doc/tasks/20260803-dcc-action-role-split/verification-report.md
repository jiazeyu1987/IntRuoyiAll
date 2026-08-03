# Verification Report

## Result

- PASS: 已在本机租户 `1` 将 DCC 查看、下载、培训/阅读确认、下发拆成四个独立权限角色，并完成目标账号绑定与只读核验。

## Role Split

- 查看角色：`dcc_action_view_independent` / roleId `910432`，菜单仅为 `6807:dcc:controlled-file:query:controlled-file/browser` 与 `6810:dcc:controlled-file:preview`，类别 `906104` 动作为 `VIEW`。
- 下载角色：`dcc_action_download_independent` / roleId `910433`，菜单仅为 `6811:dcc:controlled-file:download`，类别 `906104` 动作为 `DOWNLOAD`。
- 培训角色：`dcc_action_training_independent` / roleId `910434`，菜单仅为 `980121:dcc:controlled-file:training:mine:controlled-file/training-mine`，不绑定下载或下发类别动作。
- 下发角色：`dcc_action_distribute_independent` / roleId `910435`，不绑定菜单，类别 `906104` 动作为 `DISTRIBUTE`。

## Account Bindings

- DCC 下发/查看账号：`wangsiyu` / userId `910250`，已绑定查看角色和下发角色。
- 培训对象账号：`chenchen(885)`、`sunrongrong(937)`、`liuru(957)`、`zhaojie(1074)`、`xuejianxia(1194)`、`tengweihua(1354)`、`shihaisong(1453)`、`malingling(1499)`、`zhaomingyu(424)`，均已绑定培训角色。
- 负向核验：9 个培训对象中新下载角色绑定数量为 `0`。

## Verification Evidence

- SQL execution: `role-split.sql` 执行结果 `precheck_status=OK`，返回 role IDs `910432/910433/910434/910435`。
- Read-only DB verification: `roleCount=4`、`trainingUsersWithTrainingRole=9`、`trainingUsersWithNewDownloadRole=0`、`distributeMenuCount=0`。
- Redis cache refresh: 精确删除本次角色、目标用户、目标菜单和目标 permission 缓存键，`redis-cli DEL` 返回 `7`；后端重建后的 `user_role_ids:910250` 已包含 `910432/910435`。
- Static contract: `node -e "...role-split.sql static contract..."` -> `PASS: role split SQL static contract`，确认未直接更新 DCC 文件状态、发布指针、培训完成状态或确认时间。
- Evidence validator: `validate_database_schema.py --evidence ...database-schema-evidence.md` -> `Database schema evidence is valid.`
- File hygiene: task files UTF-8 readable and no trailing whitespace.

## Boundaries

- 未删除或禁用旧混合角色：`wenkong`、`wenkong_download`、`dcc_training_mine_e2e`、`dcc_distribute_e2e`。
- 当前“文件分发”页面入口仍复用 `dcc:controlled-file:category:manage`，本轮为了保持下发角色独立，没有把该共享管理权限授给下发角色；如果要让下发角色完全自带页面入口，需要另开任务拆分独立分发菜单权限。
- 未使用 admin 绕过，未修改 DCC 文件发布状态、培训完成状态、确认时间或 master active 指针。
- Git closeout 未执行：当前工作区存在大量非本任务改动且分支 `ahead 2`，本任务未提交或推送这些并行改动。
