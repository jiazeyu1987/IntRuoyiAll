# Task: 20260522-product-021-showroom-cover-single-native

## Goal
Create one square premium medical-device showroom cover image for `product_021` based only on the provided product facts, using exactly one native image-generation pass. The result must remain concept-level where the exact device appearance is unclear and must not introduce readable text, branding, unsupported efficacy claims, fabricated dimensions, fabricated components, or invented technical structure.

## Product Facts
- Product code: `product_021`
- Chinese name: `桡动脉止血带I`
- English name: `Transradial Pressure Bandage I`
- Owner company: `1`
- Product owner type: `瑛泰医疗`
- Lifecycle stage: `已注册`
- Indication content: `在动静脉穿刺手术中拔除穿刺针或留置针后辅助压迫止血用。`
- Registration certificate:
  - `注册证名称：一次性使用动脉压迫止血带`
  - `注册证号：沪械注准20202140509`
  - `生效时间：2020.10.27`

## Milestones
1. Record the request, hard constraints, output path, and verification target.
2. Run exactly one native image generation request.
3. Persist the generated PNG into the workspace and verify it exists as a square PNG.
4. Run the default closeout cleanup preview and record the result.

## Milestone Status
- Milestone 1: Completed. Task record, constraints, output path, and verification targets were created under `doc/tasks/20260522-product-021-showroom-cover-single-native/`.
- Milestone 2: Blocked. One native OpenAI Responses `image_generation` request using `model='gpt-5.2'` with tool model `gpt-image-1` failed upstream with `503 Service temporarily unavailable`, so no image payload was returned.
- Milestone 3: Blocked. No workspace PNG exists at `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-021-showroom-cover-single-native.png`.
- Milestone 4: Completed. The default closeout cleanup preview ran successfully and marked only disposable task sidecars for deletion.

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-021-showroom-cover-single-native.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-021-showroom-cover-single-native.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-021-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-021-showroom-cover-single-native --mode preview`

## Current Status
Blocked. The single allowed native generation request failed upstream with `503 Service temporarily unavailable`, so no PNG could be produced without violating the one-call constraint.

## Completed Work
- Created the task record and execution log for this image-generation task.
- Corrected the product facts in the task record to match the user-provided source exactly.
- Executed exactly one native OpenAI Responses `image_generation` request with retries disabled.
- Confirmed the target PNG path still does not exist after the failed upstream response.
- Ran the default closeout cleanup preview and confirmed only disposable task sidecars would be removed.

## Verification Evidence
- `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-021-showroom-cover-single-native.png'` -> `False`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-021-showroom-cover-single-native --mode preview` -> `status: ready`, keep set includes only the task core files and intended final PNG path, delete set includes only `codex-last-message.txt`, `codex-stdout.txt`, and `imagegen-prompt.txt`

## Remaining Blockers
- Upstream OpenAI Responses `image_generation` service availability. Impact: the required PNG artifact could not be created, saved, or verified, and the one-call constraint prevents another attempt in this turn.

## Cleanup Keep
- `yudao-module-showroom/output/imagegen/product-021-showroom-cover-single-native.png`
- `doc/tasks/20260522-product-021-showroom-cover-single-native/task.md`
- `doc/tasks/20260522-product-021-showroom-cover-single-native/execution-log.md`
