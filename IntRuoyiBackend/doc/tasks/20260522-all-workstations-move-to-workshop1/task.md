# 任务：工作站全部调整到车间1

## Goal

将当前 `mes_md_workstation` 中所有未删除工作站的 `所在车间` 统一调整为 `车间1 (900011)`，并补齐必要的一致性数据，避免出现“工作站属于车间1，但所绑定产线仍属于其他车间”的数据冲突。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-all-workstations-move-to-workshop1\**`
- 本地运行态真实数据：
  - `mes_md_workstation`
  - `mes_md_production_line`

## Non-Scope

- 不修改仓库生产代码。
- 不修改前端页面。
- 不额外重建全部产线或产能计划，除非为保持本次工作站-产线归属一致性所必需。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-balloon-forming-workstation-data-fix\task.md`
- Status before this task: `Blocked`
- Impact: 上一任务已因用户目标切换显式阻塞，不阻塞本次全量工作站车间调整。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务优先通过真实数据库补数，不覆盖无关代码改动；提交只包含本任务文档。

## Milestones

1. 建立任务文档并确认当前工作站/车间/产线分布。
2. 先记录 RED 现状证据，证明当前并非全部属于 `车间1`。
3. 执行真实数据更新，将所有工作站调整到 `车间1`。
4. 如存在已绑定产线的工作站，同步把该产线的 `workshop_id` 调整为 `车间1`，保持引用一致。
5. 运行 GREEN 校验并回写证据。

## Expected Verification

- 真实 SQL / 数据库校验更新前后数量变化
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-all-workstations-move-to-workshop1 --mode preview`

## Current Status

Completed.

## Current Findings

- 当前共有 `93` 条未删除工作站，其中 `69` 条已属于 `车间1 (900011)`，仍有 `24` 条不在 `车间1`。
- 当前仅有 `1` 条工作站带有效产线绑定：`900050 / AUTO-WS-01 -> production_line_id=900040`。
- 当前仅有 `1` 条有效产线：`900040 / AUTO-LINE-01`，且其 `workshop_id = 900010`。

## Assumption

- 为避免留下“工作站归属车间1，但已绑定产线仍归属其他车间”的脏数据，本次会同步把 `900040` 的 `workshop_id` 一并改为 `900011`。

## Completed Work

- 已将全部 `93` 条未删除工作站的 `workshop_id` 统一调整为 `900011 / 车间1`。
- 已同步将当前唯一被工作站引用的产线 `900040 / AUTO-LINE-01` 的 `workshop_id` 调整为 `900011`，保证工作站与产线归属一致。
- 已通过真实 SQL 和后台接口复验典型记录：
  - `900050 / AUTO-WS-01` 已改到 `workshopId=900011`，仍绑定 `productionLineId=900040`
  - `900056 / WS-B010` 已保持 `workshopId=900011`

## Verification Result

- PASS: SQL 变更前分布为 `900010 -> 24`、`900011 -> 69`
- PASS: SQL 变更后分布为 `900011 -> 93`
- PASS: 绑定产线的工作站 `900050` 更新后仍与产线 `900040` 归属一致，二者都属于 `900011`
- PASS: 后台接口 `GET /admin-api/mes/md-workstation/get?id=900050` 与 `id=900056` 返回值均确认 `workshopId=900011`

## Remaining Notes

- 本次只调整了工作站与被引用产线的车间归属，没有额外为 `吹球囊成型` 补产线绑定，所以它当前是否能通过排产预览仍取决于后续是否继续补 `production_line_id`。
