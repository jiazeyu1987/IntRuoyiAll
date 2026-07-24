# Task: 20260522-product-043-showroom-cover-single-native

## Goal
Create one square premium medical-device showroom cover image for `product_043` based only on the provided product facts, using exactly one native image-generation pass. The result must stay concept-level where the exact device appearance is unclear and must not introduce readable text, branding, unsupported efficacy claims, fabricated dimensions, fabricated components, or invented technical structure.

## Milestones
1. Record the request, hard constraints, output path, and verification target.
2. Run exactly one native image generation request.
3. Copy the generated PNG into the workspace and verify it exists as a square PNG.
4. Run the default closeout cleanup preview and record the result.

## Milestone Status
- Milestone 1: Completed. Task record, constraints, output path, and verification targets were created under `doc/tasks/20260522-product-043-showroom-cover-single-native/`.
- Milestone 2: Completed. Exactly one native image generation request was executed.
- Milestone 3: Completed. The generated PNG was copied into the workspace and verified as square.
- Milestone 4: Completed. The default closeout cleanup preview was executed and recorded.

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-043-showroom-cover-single-native.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-043-showroom-cover-single-native.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-043-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`
  - `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-043-showroom-cover-single-native --mode preview`

## Current Status
Completed. The single native image-generation pass was used, the final PNG was copied into the workspace, and verification plus closeout preview both passed.

## Completed Work
- Created the task record and execution log for this image-generation task.
- Reserved the final workspace PNG target path for `product_043`.
- Executed one native image-generation pass for the showroom cover request.
- Copied the generated PNG into `yudao-module-showroom/output/imagegen/product-043-showroom-cover-single-native.png`.
- Verified the final artifact exists and reports square PNG dimensions.
- Ran the closeout cleanup preview for this completed task.

## Verification Evidence
- `Copy-Item -LiteralPath 'C:\Users\BJB110\.codex\generated_images\019e502e-1cc0-7211-8f3d-d218a1a8c703\ig_082f41502cd082a5016a106f49f0c88191b02f6e1f0aee012e.png' -Destination 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-043-showroom-cover-single-native.png' -Force` -> completed
- `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-043-showroom-cover-single-native.png'` -> `True`
- `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-043-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }` -> `1254x1254`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-043-showroom-cover-single-native --mode preview` -> `status: ready`, no delete, no blocked, no warnings

## Remaining Blockers
- None.

## Cleanup Keep
- `yudao-module-showroom/output/imagegen/product-043-showroom-cover-single-native.png`
- `doc/tasks/20260522-product-043-showroom-cover-single-native/task.md`
- `doc/tasks/20260522-product-043-showroom-cover-single-native/execution-log.md`
