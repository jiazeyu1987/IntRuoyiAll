# Task: 20260522-batch-cover-fail-showroom-cover-single-native

## Goal
Create one square premium medical-device showroom cover image for the product facts:
- Product code: `BATCH-COVER-FAIL`
- Chinese name: `批量封面失败产品`
- English name: `Batch Cover Failure Product`
- Target market: `中国`
- Core selling points: `批量封面失败卖点`

## Milestones
1. Record scope, constraints, and verification approach.
2. Run exactly one native image generation request.
3. Copy the generated PNG into the workspace and verify the final artifact exists.
4. Run closeout cleanup preview and mark the task complete.

## Constraints
- Use only the provided facts as visual inspiration.
- Do not invent registration numbers, efficacy claims, dimensions, components, or unseen technical structures.
- If the real device appearance is still unclear, create a concept medical-device hero visual instead of fabricating a precise device structure.
- No readable text, logos, watermarks, brands, badges, or UI overlays in the image.
- The image must be suitable as a showroom product cover image.

## Cleanup Keep
- `yudao-module-showroom/output/imagegen/batch-cover-fail-showroom-cover-20260522-single-native.png`

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\batch-cover-fail-showroom-cover-20260522-single-native.png`
- Verification commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\batch-cover-fail-showroom-cover-20260522-single-native.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\batch-cover-fail-showroom-cover-20260522-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`

## Completed Work
- Ran exactly one native image generation request using only the provided product facts as inspiration.
- Copied the generated PNG from `C:\Users\BJB110\.codex\generated_images\019e4bb1-5a76-7ab3-8357-8f8e7456340a\ig_00400f5a55b7a2e5016a0f48a1774c819680f622b1dc1687f0.png` to the workspace artifact path.
- Verified the saved artifact exists and is square.
- Ran closeout cleanup preview and confirmed the final PNG and task records are preserved with no deletions.

## Verification Evidence
- `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\batch-cover-fail-showroom-cover-20260522-single-native.png'` returned `True`.
- The saved PNG dimensions are `1254x1254`.
- The saved artifact timestamp is `2026-05-22 02:03:49`.
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-batch-cover-fail-showroom-cover-single-native --mode preview` returned `status: ready` with `delete: <none>`, `blocked: <none>`, and `warnings: <none>`.

## Current Status
- Status: Completed
- Current milestone: 4
- Blockers: None
