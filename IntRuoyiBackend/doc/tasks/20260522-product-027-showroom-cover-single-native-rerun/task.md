# Task: 20260522-product-027-showroom-cover-single-native-rerun

## Goal
Create one square premium medical-device showroom cover image for `product_027` based only on the facts provided in the current user request, using exactly one native image-generation pass. If the exact device appearance remains unclear, the output must stay at concept medical-device hero level and must not introduce readable text, branding, unsupported efficacy claims, fabricated dimensions, fabricated components, or invented technical structure.

## Milestones
1. Record the request, hard constraints, output path, and verification target.
2. Run exactly one native image generation request.
3. Save the generated PNG into the workspace and verify it exists as a square PNG.
4. Run the default closeout cleanup preview and record the result.

## Milestone Status
- Milestone 1: Completed. Task record, constraints, output path, and verification targets were created under `doc/tasks/20260522-product-027-showroom-cover-single-native-rerun/`.
- Milestone 2: Completed. One native image generation request produced `C:\Users\BJB110\.codex\generated_images\019e5017-dbe1-7891-93e8-c1268bbe2bde\ig_0b357dd8186d65dd016a10697d8e6c81918fe916b50025deab.png`.
- Milestone 3: Completed. The generated PNG was copied to `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-027-showroom-cover-single-native-rerun.png` and verified as a square PNG.
- Milestone 4: Completed. The default closeout cleanup preview passed with no blockers or warnings.

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-027-showroom-cover-single-native-rerun.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-027-showroom-cover-single-native-rerun.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-027-showroom-cover-single-native-rerun.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-product-027-showroom-cover-single-native-rerun --mode preview`

## Current Status
Completed. One square showroom cover PNG was generated, copied to the workspace output path, verified, and closed out with a clean preview.

## Completed Work
- Created the task record and execution log for this image-generation task.
- Fixed the output target path for this run.
- Executed exactly one native image-generation request using only the provided product facts as prompt input.
- Copied the generated PNG into the workspace output path and verified it as a 1254x1254 PNG.
- Ran the default closeout cleanup preview and recorded the keep set.

## Verification Evidence
- Source native image: `C:\Users\BJB110\.codex\generated_images\019e5017-dbe1-7891-93e8-c1268bbe2bde\ig_0b357dd8186d65dd016a10697d8e6c81918fe916b50025deab.png`
- Final artifact: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-027-showroom-cover-single-native-rerun.png`
- `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-027-showroom-cover-single-native-rerun.png'` -> `True`
- `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-027-showroom-cover-single-native-rerun.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }` -> `1254x1254`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-product-027-showroom-cover-single-native-rerun --mode preview` -> `ready`

## Remaining Blockers
- None.

## Cleanup Keep
- `yudao-module-showroom/output/imagegen/product-027-showroom-cover-single-native-rerun.png`
- `doc/tasks/20260522-product-027-showroom-cover-single-native-rerun/task.md`
- `doc/tasks/20260522-product-027-showroom-cover-single-native-rerun/execution-log.md`
