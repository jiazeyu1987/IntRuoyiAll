# Task: 20260522-product-019-showroom-cover-single-native

## Goal
Create one square premium medical-device showroom cover image for `product_019` based only on the provided product facts, using exactly one native image-generation pass. The result must stay concept-level where the exact device appearance is unclear and must not introduce readable text, branding, unsupported efficacy claims, fabricated dimensions, fabricated components, or invented technical structure.

## Milestones
1. Record the request, hard constraints, output path, and verification target.
2. Run exactly one native image generation request.
3. Copy the generated PNG into the workspace and verify it exists as a square PNG.
4. Run the default closeout cleanup preview and record the result.

## Milestone Status
- Milestone 1: Completed. Task record, constraints, output path, and verification targets were created under `doc/tasks/20260522-product-019-showroom-cover-single-native/`.
- Milestone 2: Completed. One native image generation request produced `C:\Users\BJB110\.codex\generated_images\019e4ece-4585-7db3-b2dd-aeaa51256d26\ig_060112dca8f291c2016a1014e8075481919f337167519add3f.png`.
- Milestone 3: Completed. The generated PNG was copied to `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-019-showroom-cover-single-native.png` and verified as square.
- Milestone 4: Completed. Closeout preview reported only one disposable task marker file and closeout apply deleted it with no blocked paths.

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-019-showroom-cover-single-native.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-019-showroom-cover-single-native.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-019-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-product-019-showroom-cover-single-native --mode preview`

## Current Status
Completed. The requested single native showroom-cover PNG was generated, copied into the workspace, verified at `1254x1254`, and closed out with cleanup apply completed successfully.

## Completed Work
- Created the task record and execution log for this image-generation task.
- Executed exactly one native image generation request for `product_019`.
- Copied the generated PNG into the workspace output folder.
- Verified output existence and square dimensions.
- Ran task-closeout-cleanup in preview mode and confirmed the intended keep/delete set.
- Ran task-closeout-cleanup in apply mode and removed the disposable task marker file.

## Verification Evidence
- `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-019-showroom-cover-single-native.png'` -> `True`
- `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-019-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }` -> `1254x1254`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-product-019-showroom-cover-single-native --mode preview` -> `status: ready`, keep set includes only the task core files plus the final PNG, delete set includes only `doc/tasks/20260522-product-019-showroom-cover-single-native/imagegen-marker.txt`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-product-019-showroom-cover-single-native --mode apply` -> `status: applied`, deleted only `doc/tasks/20260522-product-019-showroom-cover-single-native/imagegen-marker.txt`

## Remaining Blockers
- None.

## Cleanup Keep
- `yudao-module-showroom/output/imagegen/product-019-showroom-cover-single-native.png`
- `doc/tasks/20260522-product-019-showroom-cover-single-native/task.md`
- `doc/tasks/20260522-product-019-showroom-cover-single-native/execution-log.md`
