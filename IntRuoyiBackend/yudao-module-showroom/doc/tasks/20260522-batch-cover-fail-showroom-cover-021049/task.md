> 路径整理说明：原项目外文件引用已移除，未收录的历史材料不作为当前验收证据；执行仍需真实输入，缺失时明确阻断。路径基准见仓库内 docs/document-paths.md。

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
  - `./IntRuoyiBackend/yudao-module-showroom/output/imagegen/batch-cover-fail-showroom-cover-20260522-021049.png`
- Verification commands:
  - `Test-Path './IntRuoyiBackend/yudao-module-showroom/output/imagegen/batch-cover-fail-showroom-cover-20260522-021049.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('./IntRuoyiBackend/yudao-module-showroom/output/imagegen/batch-cover-fail-showroom-cover-20260522-021049.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`

## Completed Work
- Ran exactly one native image generation request using only the provided product facts.
- Copied the generated PNG from `历史外部材料（引用已移除）` to the workspace artifact path.
- Verified the saved artifact exists and is square.
- Ran closeout cleanup preview successfully at the repo root task path.

## Verification Evidence
- `Test-Path './IntRuoyiBackend/yudao-module-showroom/output/imagegen/batch-cover-fail-showroom-cover-20260522-021049.png'` returned `True`.
- The saved PNG dimensions are `1254x1254`.
- The saved artifact timestamp is `2026-05-22 02:12:35`.
- `python 会话技能：task-closeout-cleanup --task-id 20260522-batch-cover-fail-showroom-cover-021049 --mode preview` returned `status: ready` with `delete: <none>`, `blocked: <none>`, and `warnings: <none>`.

## Current Status
- Status: Completed
- Current milestone: 4
- Blockers: None
