# Task: 20260522-product-020-showroom-cover-single-native-rerun

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
- Milestone 1: Completed. Task record, constraints, output path, and verification targets are recorded under `doc/tasks/20260522-product-020-showroom-cover-single-native-rerun/`.
- Milestone 2: Completed. Exactly one native image generation request succeeded and produced one PNG source image.
- Milestone 3: Completed. The generated PNG was copied into the workspace and verified as an existing square image at `1254x1254`.
- Milestone 4: Completed. The default closeout cleanup preview was run and confirmed the final PNG plus task records are kept.

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-020-showroom-cover-single-native-rerun.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-020-showroom-cover-single-native-rerun.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-020-showroom-cover-single-native-rerun.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-020-showroom-cover-single-native-rerun --mode preview`

## Current Status
Completed. One native image-generation pass was used, the final PNG was saved in the workspace, verification passed, and the cleanup preview was reviewed.

## Completed Work
- Identified the existing rerun task directory for this exact product and reused it for the current one-shot run.
- Corrected the stored product facts so the Chinese fields and registration details match the provided input.
- Locked the final workspace artifact path and verification commands before generation.
- Executed one native image-generation request with a concept-level premium medical showroom prompt constrained to the provided product facts only.
- Copied the generated PNG from `C:\Users\BJB110\.codex\generated_images\019e5017-dc09-7ee0-b670-8ff5ac0b34c6\ig_0bfe69bc0aab5feb016a10691cdeec8191a8f472f0293eda5f.png` to `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-020-showroom-cover-single-native-rerun.png`.
- Verified the workspace artifact exists and has square dimensions `1254x1254`.
- Ran the default task-closeout cleanup preview and confirmed only `task.md`, `execution-log.md`, and the final PNG are retained.

## Verification Evidence
- `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-020-showroom-cover-single-native-rerun.png'` -> `True`
- `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-020-showroom-cover-single-native-rerun.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }` -> `1254x1254`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-020-showroom-cover-single-native-rerun --mode preview` -> `status: ready`; keep = `task.md`, `execution-log.md`, final PNG; delete = `codex-stdout.txt`, `imagegen-marker.txt`, `imagegen-prompt.txt`

## Remaining Blockers
- None.

## Cleanup Keep
- `yudao-module-showroom/output/imagegen/product-020-showroom-cover-single-native-rerun.png`
- `doc/tasks/20260522-product-020-showroom-cover-single-native-rerun/task.md`
- `doc/tasks/20260522-product-020-showroom-cover-single-native-rerun/execution-log.md`
