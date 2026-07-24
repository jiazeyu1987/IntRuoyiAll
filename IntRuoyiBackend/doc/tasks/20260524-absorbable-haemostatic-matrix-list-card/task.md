# Task: 20260524-absorbable-haemostatic-matrix-list-card

## Goal

Generate one native landscape PNG medical-device product-thumbnail image for `可吸收流体明胶基质` (`Absorbable Haemostatic Matrix`), suitable for a product list card. Use exactly one native image-generation pass and keep the result as a single centered medical-device subject with ample whitespace, no text, no logo, no watermark, no props, and no complex environment.

## Scope

- Create exactly one native image-generation request.
- Save one final PNG into the workspace.
- Record verification and closeout preview evidence under this task directory.

## Non-Scope

- No batch generation.
- No code, API, database, or frontend changes.
- No fallback assets, placeholders, or mock success output.

## Milestones

1. Record the request, product mapping, output path, and verification plan.
2. Execute exactly one native landscape image generation request.
3. Save the PNG into the workspace and verify file existence plus landscape dimensions.
4. Run the default closeout cleanup preview and record the result.

## Expected Verification

- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\absorbable-haemostatic-matrix-list-card-20260524.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\absorbable-haemostatic-matrix-list-card-20260524.png'`
  - `Get-Item 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\absorbable-haemostatic-matrix-list-card-20260524.png' | Select-Object FullName, Length, LastWriteTime, Extension`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\absorbable-haemostatic-matrix-list-card-20260524.png'); try { '{0}x{1}' -f $img.Width, $img.Height } finally { $img.Dispose() }`
  - `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260524-absorbable-haemostatic-matrix-list-card --mode preview`

## Current Status

Completed.

- Milestone 1 completed: request, product mapping, output path, and verification plan recorded.
- Milestone 2 completed: exactly one native landscape image-generation request produced a single source PNG.
- Milestone 3 completed: the source PNG was copied to the final workspace path and verified as an existing landscape PNG.
- Milestone 4 completed: closeout cleanup preview ran in preview mode and kept only the task records plus the final PNG.

## Final Verification Result

- Native image generation used exactly once.
- Final artifact saved to `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\absorbable-haemostatic-matrix-list-card-20260524.png`.
- Verified file existence: `True`.
- Verified landscape dimensions: `1717x916`.
- Verified closeout cleanup preview status: `ready`, with no delete, blocked, or warning entries.

## Product Mapping Note

- Repository traceability found the seed record `sql/showroom/20260519_showroom_excel_seed.sql` row `1096` with Chinese name `可吸收流体明胶基质` and English name `Absorbable Haemostatic Matrix`.
- The seed description indicates the product is used as a surgical adjunct hemostatic matrix for capillary, venous, and small arterial bleeding when pressure, ligation, or other conventional methods are ineffective or impractical.
- No real reference product photo was provided in the request. The generation prompt therefore constrains the subject to one single absorbable haemostatic matrix medical-device body with refined medical-catalog presentation and avoids unrelated product substitution.

## Cleanup Keep

- `output/imagegen/absorbable-haemostatic-matrix-list-card-20260524.png`
- `doc/tasks/20260524-absorbable-haemostatic-matrix-list-card/task.md`
- `doc/tasks/20260524-absorbable-haemostatic-matrix-list-card/execution-log.md`
