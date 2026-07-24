# Task: 20260522-product-006-showroom-cover-single-native

## Goal
Create one square premium medical-device showroom cover image for `product_006` based only on the provided product facts, using exactly one native image-generation pass. The result must remain concept-level where the exact device appearance is unclear and must not introduce readable text, branding, unsupported efficacy claims, fabricated dimensions, fabricated components, or invented technical structure.

## Milestones
1. Record the request, hard constraints, output path, and verification target.
2. Run exactly one native image generation request.
3. Copy the generated PNG into the workspace and verify it exists as a square PNG.
4. Run the default closeout cleanup preview and record the result.

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-006-showroom-cover-single-native.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-006-showroom-cover-single-native.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-006-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-product-006-showroom-cover-single-native --mode preview`

## Current Status
Completed.

- Milestone 1 completed: request, constraints, output path, and verification targets were recorded.
- Milestone 2 completed: exactly one native image-generation pass was executed for this task.
- Milestone 3 completed: the generated PNG was copied to `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-006-showroom-cover-single-native.png` and verified as `1254x1254`.
- Milestone 4 completed: closeout cleanup preview ran successfully and marked only `codex-stdout.txt` and `imagegen-prompt.txt` as removable task artifacts.

## Cleanup Keep
- `yudao-module-showroom/output/imagegen/product-006-showroom-cover-single-native.png`
- `doc/tasks/20260522-product-006-showroom-cover-single-native/task.md`
- `doc/tasks/20260522-product-006-showroom-cover-single-native/execution-log.md`
