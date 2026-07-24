# Task: 20260522-product-037-showroom-cover-single

## Goal
Create one square premium medical-device showroom cover image for `product_037` based only on the provided product facts, using exactly one native image-generation pass. The result must stay concept-level where the exact device appearance is unclear and must not introduce readable text, branding, unsupported efficacy claims, fabricated dimensions, fabricated components, or invented technical structure.

## Milestones
1. Record the request, hard constraints, output path, and verification target.
2. Run exactly one native image generation request.
3. Copy the generated PNG into the workspace and verify it exists as a square PNG.
4. Run the default closeout cleanup preview and record the result.

## Milestone Status
- Milestone 1: Completed. Task record, constraints, output path, and verification targets were created under `doc/tasks/20260522-product-037-showroom-cover-single/`.
- Milestone 2: Completed. This execution used exactly one native image-generation pass and produced a valid PNG artifact at `2026-05-22 22:42:27`.
- Milestone 3: Completed. The generated PNG was copied into the workspace target and verified as a square image (`1254x1254`).
- Milestone 4: Completed. The default closeout cleanup preview ran successfully and reported `status: ready` with no deletions, blockers, or warnings.

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-037-showroom-cover-single-native.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-037-showroom-cover-single-native.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-037-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-product-037-showroom-cover-single --mode preview`

## Current Status
Completed. The square showroom-cover PNG exists at the target path and passed file-existence, dimension, and closeout-preview verification on `2026-05-22`.

## Completed Work
- Created the task record and execution log for this image-generation task.
- Executed exactly one native image-generation pass for this completion attempt using the constrained showroom-cover prompt for `product_037`.
- Copied the generated PNG into `yudao-module-showroom/output/imagegen/product-037-showroom-cover-single-native.png`.
- Verified that the workspace artifact exists and is square (`1254x1254`).
- Ran `task-closeout-cleanup` in preview mode and confirmed the task is safe to close out later with no blockers.

## Verification Evidence
- Native generation output copied from `C:\Users\BJB110\.codex\generated_images\019e501f-27f3-7302-9b6f-c48ada313144\ig_0852016dc1cb4bb3016a106ad49c90819195cbd99571ca2f41.png`
- `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-037-showroom-cover-single-native.png'` -> `True`
- `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-037-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }` -> `1254x1254`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-037-showroom-cover-single --mode preview` -> `status: ready`, no deletions, blockers, or warnings

## Remaining Blockers
- None.

## Cleanup Keep
- `yudao-module-showroom/output/imagegen/product-037-showroom-cover-single-native.png`
- `doc/tasks/20260522-product-037-showroom-cover-single/task.md`
- `doc/tasks/20260522-product-037-showroom-cover-single/execution-log.md`
