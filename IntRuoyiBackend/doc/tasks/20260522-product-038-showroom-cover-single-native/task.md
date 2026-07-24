# Task: 20260522-product-038-showroom-cover-single-native

## Goal
Create one square premium medical-device showroom cover image for `product_038` based only on the provided product facts, using exactly one native image-generation pass. The result must stay concept-level where the exact device appearance is unclear and must not introduce readable text, branding, unsupported efficacy claims, fabricated dimensions, fabricated components, or invented technical structure.

## Milestones
1. Record the request, hard constraints, output path, and verification target.
2. Run exactly one native image generation request.
3. Copy the generated PNG into the workspace and verify it exists as a square PNG.
4. Run the default closeout cleanup preview and record the result.

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-038-showroom-cover-single-native.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-038-showroom-cover-single-native.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-038-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-product-038-showroom-cover-single-native --mode preview`

## Current Status
Completed. On `2026-05-22`, the resumed task executed exactly one native image-generation request successfully, then copied and verified the final square PNG artifact at the expected workspace output path.

## Outcome
- Exactly one native `image_gen` request succeeded on the resumed run and produced source PNG `C:\Users\BJB110\.codex\generated_images\019e5025-61b9-7f93-a3cc-8ff3fdba6e6d\ig_0dc5853c70c42c4f016a106ced859481918028bf2b7eb4b17f.png`.
- The generated PNG was copied to `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-038-showroom-cover-single-native.png`.
- Verification confirmed the final artifact exists and is a square PNG with dimensions `1254x1254`.
- Visual review confirmed one clear balloon-catheter-like hero device in a bright clinical showroom scene, with no readable text, no logos, and no branding overlays.
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-038-showroom-cover-single-native --mode preview` returned `status: ready` with `delete: <none>`, `blocked: <none>`, and `warnings: <none>`.
- Historical note: earlier same-day native attempts were blocked by upstream `503 Service temporarily unavailable`, but the final delivery completed without fallback once the upstream request succeeded.

## Cleanup Keep
- `yudao-module-showroom/output/imagegen/product-038-showroom-cover-single-native.png`
- `doc/tasks/20260522-product-038-showroom-cover-single-native/task.md`
- `doc/tasks/20260522-product-038-showroom-cover-single-native/execution-log.md`
