# Task: 20260522-product-022-showroom-cover-single-native-rerun

## Goal
Create one square premium medical-device showroom cover image for `product_022` based only on the provided product facts, using exactly one native image-generation pass. The result must stay concept-level where the exact device appearance is unclear and must not introduce readable text, branding, unsupported efficacy claims, fabricated dimensions, fabricated components, or invented technical structure.

## Product Facts
- Product code: `product_022`
- Chinese name: `压力传感器`
- English name: `Extension Transducer`
- Owner company: `1`
- Product owner type: `瑛泰医疗`
- Lifecycle stage: `已注册`
- Indication content: `该产品采用有创方式测量患者的动脉压和静脉压，供有资质的专业医护人员在手术室或住院病房中使用。`
- Registration certificate:
  - `注册证名称：一次性使用有创压力传感器`
  - `注册证号：国械注准20173073316`
  - `生效时间：2022.8.29`

## Milestones
1. Record the request, hard constraints, output path, and verification target.
2. Run exactly one native image generation request.
3. Copy the generated PNG into the workspace and verify it exists as a square PNG.
4. Run the default closeout cleanup preview and record the result.

## Milestone Status
- Milestone 1: Completed. The existing task directory was reused and the prompt constraints were captured before generation.
- Milestone 2: Completed. Exactly one native image generation pass produced a source PNG at `C:\Users\BJB110\.codex\generated_images\019e5017-dbe1-7891-93e8-c1268bbe2bde\ig_0b357dd8186d65dd016a10697d8e6c81918fe916b50025deab.png`.
- Milestone 3: Completed. The generated PNG was copied into the workspace and verified at `1254x1254`.
- Milestone 4: Completed. The default closeout cleanup preview passed with no deletions, blockers, or warnings.

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-022-showroom-cover-single-native-rerun.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-022-showroom-cover-single-native-rerun.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-022-showroom-cover-single-native-rerun.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`
  - `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-022-showroom-cover-single-native-rerun --mode preview`

## Current Status
Completed

## Completed Work
- Reused the precreated rerun task directory for this exact product and final artifact path.
- Executed exactly one native image-generation call with the provided fact-only showroom-cover prompt.
- Copied the generated PNG into the workspace artifact path and verified the file exists as a square PNG.
- Ran the default closeout cleanup preview and recorded the ready result.

## Verification Evidence
- `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-022-showroom-cover-single-native-rerun.png'` -> `True`
- `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-022-showroom-cover-single-native-rerun.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }` -> `1254x1254`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-022-showroom-cover-single-native-rerun --mode preview` -> `status: ready`

## Remaining Blockers
- None.

## Cleanup Keep
- `yudao-module-showroom/output/imagegen/product-022-showroom-cover-single-native-rerun.png`
- `doc/tasks/20260522-product-022-showroom-cover-single-native-rerun/task.md`
- `doc/tasks/20260522-product-022-showroom-cover-single-native-rerun/execution-log.md`
