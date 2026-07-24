# Task: DCC 审批岗位真实人员补齐

## Goal

基于 `D:\ProjectPackage\Int\IntAuth\data\auth.db` 中现有的质量体系岗位分配数据，
把当前 DCC live 审批矩阵使用到的岗位补齐到可解析真实审批人的状态，并确保
`dcc_position_assignment` 不再只靠测试账号兜底。

## Scope

- 先检查上一条后端任务状态；若未完成，先显式记录阻塞后再开始本任务。
- 先创建当前任务文档和执行日志，再进行 live 数据恢复。
- 允许修改以下 live 数据：
  - `system_users`
  - `system_user_role`
  - `dcc_position_assignment`
- 不切换数据库，不改文件类别、审批矩阵、目录、权限、培训、分发或受控文件数据。
- 仅处理当前 active route 实际引用到的岗位，以及其直接对应的 PDF/IntAuth 源岗位人。

## Previous Task Check

- Previous backend task:
  `doc/tasks/20260517-dcc-review-signoff-pdf-full-restore/task.md`
- Status before this task: completed.
- Impact: the full matrix restore is already closed; this task only补齐真实审批人。

## Milestones

- [x] M1: Create the task package and capture the current assignment gap.
- [x] M2: Derive the source user-to-position mapping from IntAuth quality-system data.
- [x] M3: Create any missing local users needed for active-route positions.
- [x] M4: Replace live DCC position assignments with the mapped real users and verify coverage.

## Expected Verification

- Pre-fix query proves active routes still reference positions without active assignment.
- Post-fix query proves every position used by active routes has at least one active assignment.
- Representative mapping evidence records:
  - `QA -> 黄露露`
  - `QC -> 梁春兰`
  - `QMS -> 张娟`
  - `文控 -> 赵海辰`
  - `编制部门负责人/授权代表 -> 张嘉忆`
  - `研发部门负责人/总经理 -> 赵丽娜`

## Current Status

Completed. The IntAuth source assignments have been mapped into local
`system_users` and `dcc_position_assignment`, and all positions used by active
routes now resolve to active local assignees.

## Blocker And Impact

- Blocker: none currently.
- Impact: none currently.

## Final Verification Result

- Local-user mapping design:
  - because IntAuth source assignments use UUID user ids and local DCC requires
    `bigint` `system_users.id`, this task created explicit local users from the
    IntAuth source usernames/full names instead of dropping assignments.
  - created users were seeded into `system_users` with active status, `dept_id=100`,
    and the same local password hash template as current `admin123`.
  - each created or reused source user was granted roles `1` and `2`, matching the
    existing local DCC real-approval accounts.
- Created local users:
  - `149 zhaojumei / 赵居梅`
  - `150 huanglulu / 黄露露`
  - `151 liangchunlan / 梁春兰`
  - `152 zhangjuan / 张娟`
  - `153 fuzhangfeng / 付长凤`
  - `154 jiping / 季萍`
  - `155 ranfeiyan / 冉飞艳`
  - `156 mapeili / 马培莉`
  - `157 yexiaojing / 叶晓静`
  - `158 liulinlin / 刘林林`
  - `159 licaixia / 李彩霞`
  - `160 wangduan / 王端`
  - `161 caoqin / 曹勤`
  - `162 baiyanping / 白艳萍`
  - `163 fengmiaomiao / 冯苗苗`
  - `164 zhangjiayi / 张嘉忆`
  - `165 zhaolina / 赵丽娜`
  - `166 zhaohaichen / 赵海辰`
- Representative restored assignments:
  - `QA -> 黄露露`
  - `QC -> 梁春兰`
  - `QMS -> 张娟`
  - `文控 -> 赵海辰`
  - `部门负责人 / 部门授权代表 / 编制部门负责人 / 授权代表 -> 张嘉忆`
  - `研发部门负责人 / 总经理 -> 赵丽娜`
- Coverage:
  - positions used by active routes without active assignment: `0`
  - created local user count for this task: `18`
