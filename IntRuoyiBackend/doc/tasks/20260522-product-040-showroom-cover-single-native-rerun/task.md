# Task: 20260522-product-040-showroom-cover-single-native-rerun

## Goal
Create one square premium medical-device showroom cover image for `product_040` based only on the provided product facts, using exactly one native image-generation pass. If the precise device appearance remains unclear, the result must stay at concept level and must not introduce readable text, branding, unsupported efficacy claims, fabricated dimensions, fabricated components, or invented technical structure.

## Product Facts
- Product code: `product_040`
- Chinese name: `棘突球囊`
- English name: `Coronary Dilatation Catheter`
- Owner company: `1`
- Product owner type: `瑛泰医疗`
- Lifecycle stage: `已注册`
- Indication content: `该类产品在临床上通常定位于血管病变的预处理，具体适用的情况包括：1.支架内再狭窄病变；2.开口病变；3.分叉病变；4.轻中度钙化病变或重度钙化病变经过旋磨等预处理后；5.纤维性病变等常规球囊处理效果不佳的，经腔内影像确认后，使用常规球囊扩张试过仍然效果不佳的。`
- Registration certificate:
  - `注册证名称：冠状动脉棘突球囊扩张导管`
  - `注册证号：国械注准20243032641`
  - `生效时间：2024.12.26`

## Milestones
1. Record the request, constraints, target path, and verification commands for this rerun.
2. Run exactly one native image-generation request.
3. Save the resulting PNG into the workspace and verify that it exists as a square image.
4. Run the default closeout cleanup preview and record the result.

## Milestone Status
- Milestone 1: Completed. Task record, product facts, constraints, output path, and verification commands are recorded under `doc/tasks/20260522-product-040-showroom-cover-single-native-rerun/`.
- Milestone 2: Completed. Exactly one native image-generation request succeeded and produced one PNG source image.
- Milestone 3: Completed. The generated PNG was copied into the workspace and verified as an existing square image at `1254x1254`.
- Milestone 4: Completed. The default closeout cleanup preview was run and confirmed the final PNG plus task records are kept.

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-040-showroom-cover-single-native-rerun.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-040-showroom-cover-single-native-rerun.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-040-showroom-cover-single-native-rerun.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`
  - `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-040-showroom-cover-single-native-rerun --mode preview`

## Current Status
Completed. One native image-generation pass was used, the final PNG was saved in the workspace, verification passed, and the cleanup preview was reviewed.

## Completed Work
- Identified the prior blocked `product_040` attempt and isolated this rerun into a separate task directory.
- Recorded the provided product facts, hard constraints, output path, and verification commands before generation.
- Executed one native image-generation request with a concept-level premium medical showroom prompt constrained to the provided product facts only.
- Copied the generated PNG from `C:\Users\BJB110\.codex\generated_images\019e5025-bb45-7661-9f41-2b5fa4a3c68d\ig_03d2bb48dd94aa6a016a106de2ed3c81919afdc8246d0a67a6.png` to `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-040-showroom-cover-single-native-rerun.png`.
- Verified the workspace artifact exists and has square dimensions `1254x1254`.
- Ran the default task-closeout cleanup preview and confirmed only `task.md`, `execution-log.md`, and the final PNG are retained.

## Verification Evidence
- `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-040-showroom-cover-single-native-rerun.png'` -> `True`
- `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-040-showroom-cover-single-native-rerun.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }` -> `1254x1254`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-040-showroom-cover-single-native-rerun --mode preview` -> `status: ready`; keep = `task.md`, `execution-log.md`, final PNG; delete = none; blocked = none; warnings = none

## Remaining Blockers
- None.

## Cleanup Keep
- `yudao-module-showroom/output/imagegen/product-040-showroom-cover-single-native-rerun.png`
- `doc/tasks/20260522-product-040-showroom-cover-single-native-rerun/task.md`
- `doc/tasks/20260522-product-040-showroom-cover-single-native-rerun/execution-log.md`
