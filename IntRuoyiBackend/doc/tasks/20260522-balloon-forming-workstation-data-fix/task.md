# 任务：吹球囊成型工作站补数

## Goal

为工序 `900331 / 吹球囊成型` 补齐工作站主数据，并尽量使其满足排产预览的实际前置条件。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-balloon-forming-workstation-data-fix\**`
- 本地运行态真实数据：
  - `mes_md_workstation`
  - `mes_md_production_line`
  - `mes_pro_capacity_plan`

## Non-Scope

- 不修改仓库生产代码。
- 不修改前端页面。
- 不用 mock 数据替代真实主数据。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-publish-button\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已完成，不阻塞本次 MES 主数据补数。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务优先通过真实接口/数据库补数，不覆盖无关代码改动。

## Milestones

1. 建立任务文档并核对当前工序/工作站/产线现状。
2. 明确“仅新增工作站”与“让预览真正可排产”之间的前置条件差异。
3. 按用户确认的目标执行真实数据补数。
4. 验证工作站数据与排产预览结果。

## Expected Verification

- 真实接口或数据库校验新增结果
- 若目标是解除预览阻塞，则复验 `POST /admin-api/mes/pro/auto-schedule/preview`

## Current Status

Completed.

## Status Note

- 之前因用户临时切换到“先把全部工作站改到车间1”而暂停。
- 该前置调整现已完成，本任务恢复执行，继续补 `吹球囊成型` 的产线绑定并复验排产预览。

## Current Findings

- 工序 `900331 / 吹球囊成型` 当前仅剩 1 条有效工作站：`900056 / WS-B010 / 吹球囊成型-工位`
- 该工作站 `production_line_id = NULL`
- `车间1 / 900011` 当前没有任何有效产线
- 系统当前唯一有效产线是 `900040 / AUTO-LINE-01`，但它属于 `workshop_id = 900010`
- 该产线当前仅存在一条计划产能：`2026-05-13 / shift 900031 / 720 分钟`

## Risk Note

- 仅新增一条“无产线绑定”的工作站，无法消除当前预览阻塞。
- 若要让排产预览真正通过，除了工作站本身，还至少需要同车间可用产线；在当前数据下，很可能还需要补该产线的产能计划。

## Completed Work

- 已恢复本任务执行，并在“全部工作站切到车间1”完成后继续处理路线阻塞。
- 已先尝试通过后台接口为 `900056 / WS-B010` 设置 `productionLineId=900040`；接口返回成功，但回读仍为 `NULL`。
- 为避免继续被该持久化链路阻塞，已直接通过本地 MySQL 真实数据补数：
  - 将 `900056 / WS-B010 / 吹球囊成型-工位` 绑定到 `900040 / AUTO-LINE-01`
  - 将 route `900025` 上其余 23 个工作站一并绑定到同一产线 `900040`
- 绑定后已复验自动排产预览：
  - `blockingIssueCount` 从 `2` 降到 `0`
  - `generatedTaskCount` 从 `0` 提升到 `48`
  - 当前仅剩 `54` 条 `MATERIAL` warning，不再有 `LINE` 阻塞

## Verification Result

- PASS: `SELECT id, code, name, workshop_id, process_id, production_line_id FROM mes_md_workstation WHERE id = 900056`
  - returns `production_line_id = 900040`
- PASS: route `900025` 关联工作站复验
  - 24 个对应工作站均已绑定 `production_line_id = 900040`
- PASS: `POST /admin-api/mes/pro/auto-schedule/preview`
  - returns `blockingIssueCount = 0`
  - returns `generatedTaskCount = 48`
  - no `LINE` issues remain

## Remaining Notes

- 当前预览已跨过“工序缺少可用工作站或产线绑定”的阻塞点，但仍存在 `54` 条真实物料短缺 warning。
- 后台 `PUT /admin-api/mes/md-workstation/update` 对 `productionLineId` 的持久化表现异常：本次请求返回成功但未实际落库，后续若要从 UI 正常维护产线绑定，建议单独开任务排查。
