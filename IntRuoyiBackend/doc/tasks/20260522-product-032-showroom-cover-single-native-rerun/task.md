# Task: 20260522-product-032-showroom-cover-single-native-rerun

## Goal
Create one square premium medical-device showroom cover image for `product_032` based only on the provided product facts, using exactly one native image-generation pass. The result must stay concept-level where the exact device appearance is unclear and must not introduce readable text, branding, unsupported efficacy claims, fabricated dimensions, fabricated components, or invented technical structure.

## Product Facts
- Product code: `product_032`
- Chinese name: `PTFE导丝`
- English name: `PTFE Guide Wire`
- Owner company: `1`
- Product owner type: `瑛泰医疗`
- Lifecycle stage: `已注册`
- Indication content: `用于血管造影，目的是建立了一个从穿刺部位到病变部位或通过病变部位到达远端的通道，辅助其他器械进行定位操作。`
- Registration certificate:
  - `注册证名称：一次性使用造影导丝`
  - `注册证号：国械注准20163032107`
  - `生效时间：2021.5.17`

## Milestones
1. Record the request, hard constraints, output path, and verification target.
2. Run exactly one native image generation request.
3. Copy the generated PNG into the workspace and verify it exists as a square PNG.
4. Run the default closeout cleanup preview and record the result.

## Milestone Status
- Milestone 1: Completed. Task record, constraints, output path, and verification target are recorded.
- Milestone 2: Completed. Exactly one native image generation request ran successfully and produced a PNG in the local generated-images cache.
- Milestone 3: Completed. The generated PNG was copied into the workspace and verified as a square image.
- Milestone 4: Completed. The default closeout cleanup preview ran and reported no blockers.

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-032-showroom-cover-single-native.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-032-showroom-cover-single-native.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-032-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-product-032-showroom-cover-single-native-rerun --mode preview`

## Current Status
Completed. The single native generation pass succeeded, the final PNG was copied to the workspace, square dimensions were verified, and the default closeout cleanup preview returned `status: ready`.

## Completed Work
- Recorded the request and strict visual constraints for `product_032`.
- Executed exactly one native image-generation request for this rerun task.
- Copied the generated PNG from the local generated-images cache into the workspace target path.
- Verified that the final artifact exists and reports square dimensions.
- Ran the required closeout cleanup preview for the completed task.

## Verification Evidence
- Native image output source:
  - `C:\Users\BJB110\.codex\generated_images\019e5017-dbe1-7891-93e8-c1268bbe2bde\ig_0b357dd8186d65dd016a10697d8e6c81918fe916b50025deab.png`
- Final artifact verification:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-032-showroom-cover-single-native.png'` -> `True`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-032-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }` -> `1254x1254`
- Cleanup preview:
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-product-032-showroom-cover-single-native-rerun --mode preview` -> `status: ready`

## Cleanup Keep
- `yudao-module-showroom/output/imagegen/product-032-showroom-cover-single-native.png`
- `doc/tasks/20260522-product-032-showroom-cover-single-native-rerun/task.md`
- `doc/tasks/20260522-product-032-showroom-cover-single-native-rerun/execution-log.md`
