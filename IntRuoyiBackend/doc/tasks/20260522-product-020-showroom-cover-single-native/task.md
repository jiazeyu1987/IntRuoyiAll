# Task: 20260522-product-020-showroom-cover-single-native

## Goal
Create one square premium medical-device showroom cover image for `product_020` based only on the provided product facts, using exactly one native image-generation pass. The result must stay concept-level where the exact device appearance is unclear and must not introduce readable text, branding, unsupported efficacy claims, fabricated dimensions, fabricated components, or invented technical structure.

## Product Facts
- Product code: `product_020`
- Chinese name: `气囊式止血带II`
- English name: `Transradial Pressure Bandage II`
- Owner company: `1`
- Product owner type: `瑛泰医疗`
- Lifecycle stage: `已注册`
- Indication content: `用于桡动脉导管插管术后压迫止血用。`
- Registration certificate:
  - `注册证名称：气囊式止血带`
  - `注册证号：沪械注准20202140417`
  - `生效时间：2020.8.31`

## Milestones
1. Record the request, hard constraints, output path, and verification target.
2. Run exactly one native image generation request.
3. Copy the generated PNG into the workspace and verify it exists as a square PNG.
4. Run the default closeout cleanup preview and record the result.

## Milestone Status
- Milestone 1: Completed. Task record, constraints, output path, and verification targets were created under `doc/tasks/20260522-product-020-showroom-cover-single-native/`.
- Milestone 2: Blocked. One native OpenAI Responses `image_generation` request using `model='gpt-5'` with tool `model='gpt-image-1'` failed with upstream `503 Service temporarily unavailable`, so no image payload was returned.
- Milestone 3: Blocked. No generated PNG exists to persist or verify because milestone 2 failed upstream.
- Milestone 4: Blocked. Closeout preview is not applicable while the task remains incomplete and blocked on the missing image artifact.

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-020-showroom-cover-single-native.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-020-showroom-cover-single-native.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-020-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`
  - `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-020-showroom-cover-single-native --mode preview`

## Current Status
Blocked for the original one-shot attempt. The prompt was corrected and one native image-generation request was executed exactly once, but the upstream OpenAI image service returned `503 Service temporarily unavailable`, so no PNG artifact could be created in this original task run.

## Completed Work
- Created the task record and execution log for this image-generation task.
- Corrected the stored product facts and prompt text to preserve the provided Chinese fields and constrain the image to a concept-level transradial pressure bandage hero visual.
- Executed exactly one native OpenAI Responses `image_generation` request with retries disabled to preserve the one-call constraint.

## Verification Evidence
- `python - <single OpenAI Responses image_generation request with model='gpt-5' and tool model='gpt-image-1'>` -> failed with `503 Service temporarily unavailable`; no PNG file was written to `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-020-showroom-cover-single-native.png`

## Remaining Blockers
- Upstream OpenAI image-generation service availability. Impact: the required showroom-cover PNG could not be created, saved, verified, or returned.

## Superseded Resolution

- This blocked one-shot run was later superseded by the successful rerun task:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\doc\tasks\20260522-product-020-showroom-cover-single-native-rerun\task.md`
- Final delivered artifact was recorded there and has already been committed separately:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-020-showroom-cover-single-native-rerun.png`

## Cleanup Keep
- `yudao-module-showroom/output/imagegen/product-020-showroom-cover-single-native.png`
- `doc/tasks/20260522-product-020-showroom-cover-single-native/task.md`
- `doc/tasks/20260522-product-020-showroom-cover-single-native/execution-log.md`
