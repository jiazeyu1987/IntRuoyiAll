# Task: 20260522-batch-cover-fail-showroom-cover-021049

## Goal
Create one square premium medical-device showroom cover image using only the provided product facts for:
- Product code: `BATCH-COVER-FAIL`
- Chinese name: `批量封面失败产品`
- English name: `Batch Cover Failure Product`
- Target market: `中国`
- Core selling points: `批量封面失败卖点`

## Milestones
1. Record scope, constraints, and verification plan.
2. Run exactly one native image generation request.
3. Copy the generated PNG into the workspace and verify the artifact exists and is square.
4. Run closeout cleanup preview and mark the task completed.

## Constraints
- Use only the provided facts as visual inspiration.
- Do not invent registration numbers, efficacy claims, dimensions, components, or unseen technical structures.
- If the device appearance is unclear, use a concept medical-device hero visual instead of fabricating a precise structure.
- No readable text, logos, watermarks, brands, badges, or UI overlays.
- Output must be suitable as a showroom product cover image.

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\batch-cover-fail-showroom-cover-20260522-021049.png`
- Verification commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\batch-cover-fail-showroom-cover-20260522-021049.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\batch-cover-fail-showroom-cover-20260522-021049.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`

## Completed Work
- Ran exactly one native image generation request using only the provided product facts.
- Copied the generated PNG from `C:\Users\BJB110\.codex\generated_images\019e4bba-eafb-7f93-ab4e-50d8b7403b2b\ig_0731ed0da4a9c267016a0f4ac58f74819089ac6e2b4cc6bcc6.png` to the workspace artifact path.
- Verified the saved artifact exists and is square.
- Ran closeout cleanup preview successfully at the repo root task path.

## Verification Evidence
- `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\batch-cover-fail-showroom-cover-20260522-021049.png'` returned `True`.
- The saved PNG dimensions are `1254x1254`.
- The saved artifact timestamp is `2026-05-22 02:12:35`.
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260522-batch-cover-fail-showroom-cover-021049 --mode preview` returned `status: ready` with `delete: <none>`, `blocked: <none>`, and `warnings: <none>`.

## Current Status
- Status: Completed
- Current milestone: 4
- Blockers: None
