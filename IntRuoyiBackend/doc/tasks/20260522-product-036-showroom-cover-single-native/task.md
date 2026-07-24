# Task: 20260522-product-036-showroom-cover-single-native

## Goal
Create one square premium medical-device showroom cover image for `product_036` based only on the provided product facts, using exactly one native image-generation pass. The result must stay concept-level where the exact device appearance is unclear and must not introduce readable text, branding, unsupported efficacy claims, fabricated dimensions, fabricated components, or invented technical structure.

## Milestones
1. Record the request, hard constraints, output path, and verification target.
2. Run exactly one native image generation request.
3. Copy the generated PNG into the workspace and verify it exists as a square PNG.
4. Run the default closeout cleanup preview and record the result.

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-036-showroom-cover-single-native.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-036-showroom-cover-single-native.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-036-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-product-036-showroom-cover-single-native --mode preview`

## Milestone Status
1. Completed. Request, hard constraints, output path, and verification target were recorded before generation.
2. Completed. Exactly one native image generation pass was executed and saved by the built-in tool at `C:\Users\BJB110\.codex\generated_images\019e501f-27f3-7302-9b6f-c48ada313144\ig_0852016dc1cb4bb3016a106ad49c90819195cbd99571ca2f41.png`.
3. Completed. The generated PNG was copied into the workspace at `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-036-showroom-cover-single-native.png` and verified as a square PNG with dimensions `1254x1254`.
4. Completed. Closeout cleanup preview returned `status: ready` with the task records and final PNG in `keep`, and no `delete`, `blocked`, or `warnings` entries.

## Verification Evidence
- `Test-Path -LiteralPath 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-036-showroom-cover-single-native.png'` -> `True`
- `System.Drawing.Image` inspection -> `1254x1254`
- PNG signature bytes -> `89-50-4E-47-0D-0A-1A-0A`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-product-036-showroom-cover-single-native --mode preview` -> `status: ready`

## Current Status
Completed. All milestones passed and there are no remaining blockers.

## Cleanup Keep
- `yudao-module-showroom/output/imagegen/product-036-showroom-cover-single-native.png`
- `doc/tasks/20260522-product-036-showroom-cover-single-native/task.md`
- `doc/tasks/20260522-product-036-showroom-cover-single-native/execution-log.md`
