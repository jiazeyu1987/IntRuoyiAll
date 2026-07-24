# 任务：展厅封面失败项补齐 live preview asset（后端）

## Goal

为当前一键封面后台任务 `id=2` 中缺少 `live product preview asset` 的产品补齐最小可用的 live preview asset 记录，并把因此永久失败的任务项恢复为可续跑状态。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-cover-failed-items-preview-asset-repair\**`

## Non-Scope

- 不修改业务代码。
- 不删除现有封面任务。
- 不处理 OpenAI `503` 造成的封面生成失败，除非它与 preview asset 修复直接耦合。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-batch-cover-failed-items-diagnosis\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已确认失败根因是缺少 `live product preview asset`，不阻塞本次直接补齐数据。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务仅新增修复记录，不覆盖无关改动。

## Milestones

1. 读取 preview asset 成功样本与共享预览文件基线。
2. 为任务 `id=2` 中所有缺少 preview asset 的产品补齐 live preview asset 记录。
3. 将因 preview asset 缺失而 `FAILED` 的任务项恢复为 `WAITING`。
4. 观察下一轮后台续跑是否开始消费修复结果。
5. 更新证据并执行 closeout preview。

## Expected Verification

- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-cover-failed-items-preview-asset-repair --mode preview`

## Current Status

Completed.

## Completed Work

- 读取了现有 product live preview asset 成功样本，确认系统已存在可复用的共享预览文件 `infra_file.id = 2272`。
- 为任务 `id=2` 中全部缺少 preview asset 的产品一次性补齐了 `124` 条 `showroom_preview_asset_version` 的 `PUBLISHED` 记录。
- 将因为 preview asset 缺失而永久 `FAILED` 的 `10` 条任务项恢复为 `WAITING`，并清理了同类 stale 错误背景。
- 复验结果显示：
  - 任务 `id=2` 中“缺 preview asset”的项已降为 `0`
  - 当前任务项分布已从 `FAILED=10` 变为 `FAILED=0`
  - 后台续跑重新启动后出现了至少 `1` 条 `COMPLETED` 项，说明修复结果已被任务消费

## Verification Result

- PASS: inserted preview assets
  - `124` new `PRODUCT` live preview asset rows created for task `id=2` missing items
- PASS: no remaining preview-asset blocker
  - query result: missing published preview asset rows for task `id=2` -> `0`
  - query result: task items still carrying `SHOWROOM_TARGET_NOT_FOUND: live product preview asset is required` -> `0`
- PASS: task item state improved after scheduler resumed
  - before repair: `FAILED=10`, `RUNNING=8`, `WAITING=106`
  - after repair and next run: `FAILED=0`, `RUNNING=8`, `WAITING=115`, `COMPLETED=1`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-cover-failed-items-preview-asset-repair --mode preview`

## Final Assessment

- 用户选择的第 `1` 项已完成：缺少 `live product preview asset` 的失败根因已经修复。
- 当前一键封面任务里已不再有 preview asset 缺失导致的失败项。
- 任务中仍残留另外一类外部失败背景：部分项的 `last_error` 为 OpenAI native `image_generation` `503 Service temporarily unavailable`；这与 preview asset 缺失无关，是下一类独立问题。
