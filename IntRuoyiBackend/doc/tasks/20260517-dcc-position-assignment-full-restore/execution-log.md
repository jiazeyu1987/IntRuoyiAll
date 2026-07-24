# Execution Log: DCC 审批岗位真实人员补齐

BDD: active DCC routes must resolve to real assigned users -> Given the live
approval matrix has already been restored for all 48 categories / When DCC
resolves active-route positions to current assignees / Then every position used
by an active route must have at least one active local assignment.

BDD: IntAuth source assignments must be imported by explicit local mapping ->
Given IntAuth quality-system position assignments use UUID user ids and the live
DCC runtime uses `bigint` `system_users.id` / When restoring real assignees /
Then the system must create or reuse explicit local user mappings instead of
silently dropping the source assignments.

RED: pre-fix active-route assignment coverage -> FAIL, multiple active-route
positions had no active local assignment, including `QC`、`新品开发`、`设备开发`、
`生产`、`生产计划`、`生产采购`、`仓储物流`、`包装设计`、`市场`、`检测中心`、
`研发部门负责人`、`总经理`.

GREEN: derived explicit IntAuth source user mapping for active-route positions,
including `QA -> 黄露露`, `QC -> 梁春兰`, `QMS -> 张娟`, `文控 -> 赵海辰`,
`编制部门负责人或授权代表 -> 张嘉忆`, and `研发部门负责人或总经理 -> 赵丽娜`.

GREEN: created 18 local `system_users` rows from the IntAuth source usernames and
full names, granted each local roles `1` and `2`, and replaced live
`dcc_position_assignment` rows for all positions currently used by active routes.

GREEN: post-fix active-route assignment coverage -> PASS, the query for active
route positions without active assignment returned an empty set.
