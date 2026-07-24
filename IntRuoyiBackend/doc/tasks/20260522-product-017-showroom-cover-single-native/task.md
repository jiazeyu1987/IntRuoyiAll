# Task: 20260522-product-017-showroom-cover-single-native

## Goal
Create one square premium medical-device showroom cover image for `product_017` based only on the provided product facts, using exactly one native image-generation pass. The result must stay concept-level where the exact device appearance is unclear and must not introduce readable text, branding, unsupported efficacy claims, fabricated dimensions, fabricated components, or invented technical structure.

## Product Facts
- Product code: `product_017`
- Chinese name: `造影剂推入器-C`
- English name: `PC Syringe`
- Owner company: `1`
- Product owner type: `瑛泰医疗`
- Lifecycle stage: `已注册`
- Indication content: `用于需要精准控制造影剂流速、剂量及压力的影像引导手术，确保病变显影清晰且操作安全。`
- Registration certificate:
  - `注册证名称：一次性使用造影剂推入器`
  - `注册证号：国械注准20153030120`
  - `生效时间：2024.5.30`

## Milestones
1. Record the request, hard constraints, output path, and verification target.
2. Run exactly one native image generation request.
3. Copy the generated PNG into the workspace and verify it exists as a square PNG.
4. Run the default closeout cleanup preview and record the result.

## Milestone Status
- Milestone 1: Completed. Task record, hard constraints, output path, and verification targets were created under `doc/tasks/20260522-product-017-showroom-cover-single-native/`.
- Milestone 2: Completed. Exactly one native image generation request produced `C:\Users\BJB110\.codex\generated_images\019e4fa0-d505-7471-9d36-172622399423\ig_06e2f044aa35ce45016a104a797c608191a38aa2c09fcc2aa0.png`.
- Milestone 3: Completed. The generated PNG was copied to `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-017-showroom-cover-single-native.png` and verified as square.
- Milestone 4: Completed. The default closeout cleanup preview reported no disposable task sidecars and no blocked paths.

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-017-showroom-cover-single-native.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-017-showroom-cover-single-native.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-017-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-017-showroom-cover-single-native --mode preview`

## Current Status
Completed. The requested single native showroom-cover PNG was generated, copied into the workspace, verified at `1254x1254`, and passed the default closeout cleanup preview.

## Completed Work
- Created the task record and execution log for this image-generation task.
- Executed exactly one native image generation request for `product_017`.
- Recovered the generated source PNG path from the fresh built-in output directory after the wrapper command timed out before printing its final message.
- Copied the generated PNG into the workspace output folder.
- Verified output existence and square dimensions.
- Ran task-closeout-cleanup in preview mode and confirmed the keep/delete set.

## Verification Evidence
- `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-017-showroom-cover-single-native.png'` -> `True`
- `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-017-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }` -> `1254x1254`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-017-showroom-cover-single-native --mode preview` -> `status: ready`, keep set includes only the task core files plus the final PNG, delete set is empty

## Remaining Blockers
- None.

## Cleanup Keep
- `yudao-module-showroom/output/imagegen/product-017-showroom-cover-single-native.png`
- `doc/tasks/20260522-product-017-showroom-cover-single-native/task.md`
- `doc/tasks/20260522-product-017-showroom-cover-single-native/execution-log.md`
