# Task: 20260522-product-018-showroom-cover-single-native

## Goal
Create one square premium medical-device showroom cover image for `product_018` based only on the provided product facts, using exactly one native image-generation pass. The result must stay concept-level where the exact device appearance is unclear and must not introduce readable text, branding, unsupported efficacy claims, fabricated dimensions, fabricated components, or invented technical structure.

## Product Facts
- Product code: `product_018`
- Chinese name: `??????`
- English name: `Femoral Pressure Bandage`
- Owner company: `1`
- Product owner type: `????`
- Lifecycle stage: `???`
- Indication content: `??????????????????????????`
- Registration certificate:
  - `??????????????`
  - `?????????20212140484`
  - `?????2021.8.24`

## Milestones
1. Record the request, hard constraints, output path, and verification target.
2. Run exactly one native image generation request.
3. Copy the generated PNG into the workspace and verify it exists as a square PNG.
4. Run the default closeout cleanup preview and record the result.

## Milestone Status
- Milestone 1: Completed. Task record, constraints, output path, and verification targets were recorded under `doc/tasks/20260522-product-018-showroom-cover-single-native/`.
- Milestone 2: Completed. One native `image_gen` request was executed exactly once for the constrained `product_018` showroom-cover prompt.
- Milestone 3: Completed. The generated PNG was copied from `C:\Users\BJB110\.codex\generated_images\019e4fc5-741e-7093-9f5b-0cb2e93f8c65\ig_0ebd9cbfd33ea275016a1053d4a5b48191b766500e5c6f5e66.png` to the workspace artifact path and verified as square.
- Milestone 4: Completed. The default closeout cleanup preview returned `status: ready` with no deletes, blockers, or warnings.

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-018-showroom-cover-single-native.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-018-showroom-cover-single-native.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-018-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-018-showroom-cover-single-native --mode preview`

## Current Status
Completed. The single native image-generation pass succeeded, the final PNG was persisted at `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-018-showroom-cover-single-native.png`, and verification plus closeout preview both passed.

## Completed Work
- Reused the existing `product_018` task directory and updated it to match the current user-provided facts and constraints.
- Executed exactly one native `image_gen` generation request using only the provided facts and the concept-level showroom-cover constraints.
- Copied the generated PNG from `$CODEX_HOME/generated_images/...` into the workspace artifact path.
- Verified that the final workspace artifact exists and reports `1254x1254`.
- Ran the default task closeout cleanup preview and recorded the ready status.

## Verification Evidence
- Source generated PNG:
  - `C:\Users\BJB110\.codex\generated_images\019e4fc5-741e-7093-9f5b-0cb2e93f8c65\ig_0ebd9cbfd33ea275016a1053d4a5b48191b766500e5c6f5e66.png`
- Final workspace PNG:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-018-showroom-cover-single-native.png`
- Verification results:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-018-showroom-cover-single-native.png'` -> `True`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-018-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }` -> `1254x1254`
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-018-showroom-cover-single-native --mode preview` -> `status: ready`

## Cleanup Keep
- `yudao-module-showroom/output/imagegen/product-018-showroom-cover-single-native.png`
- `doc/tasks/20260522-product-018-showroom-cover-single-native/task.md`
- `doc/tasks/20260522-product-018-showroom-cover-single-native/execution-log.md`
