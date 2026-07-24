# Task: 2026-05-22-batch-cover-fail-showroom-cover

## Goal
Create one square premium medical-device showroom cover image for the product facts:
- Product code: `BATCH-COVER-FAIL`
- Chinese name: `批量封面失败产品`
- English name: `Batch Cover Failure Product`
- Target market: `中国`
- Core selling points: `批量封面失败卖点`

## Milestones
1. Record task scope, constraints, and verification approach.
2. Generate exactly one square cover image using native image generation.
3. Save the final PNG into the workspace and verify the artifact exists.
4. Run closeout cleanup preview and mark the task complete.

## Expected Verification
- The final artifact is a PNG file in the workspace.
- The artifact path is:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\tmp\imagegen\batch-cover-fail-showroom-cover-20260522.png`
- Verification commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\tmp\imagegen\batch-cover-fail-showroom-cover-20260522.png'`
  - `Add-Type -AssemblyName System.Drawing; $img = [System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\tmp\imagegen\batch-cover-fail-showroom-cover-20260522.png'); Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()); $img.Dispose()`

## Cleanup Keep
- `yudao-module-showroom/tmp/imagegen/batch-cover-fail-showroom-cover-20260522.png`

## Completed Work
- Created the required task record and verification plan.
- Generated exactly one square premium medical-device showroom cover with the native image generation tool.
- Copied the generated PNG into the workspace at `yudao-module-showroom/tmp/imagegen/batch-cover-fail-showroom-cover-20260522.png`.
- Ran closeout cleanup preview and confirmed the artifact and task records are preserved with no deletions.

## Verification Evidence
- `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\tmp\imagegen\batch-cover-fail-showroom-cover-20260522.png'` returned `True`.
- The saved PNG dimensions are `1254x1254`.
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 2026-05-22-batch-cover-fail-showroom-cover --mode preview` returned `status: ready` with `delete: <none>`, `blocked: <none>`, and `warnings: <none>`.

## Current Status
- Status: Completed
- Current milestone: 4
- Blockers: None
