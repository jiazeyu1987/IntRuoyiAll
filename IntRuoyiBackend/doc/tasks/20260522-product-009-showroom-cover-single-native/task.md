# Task: 20260522-product-009-showroom-cover-single-native

## Goal
Create one square premium medical-device showroom cover image for `product_009` based only on the provided product facts, using exactly one native image-generation pass. The result must stay concept-level where the exact device appearance is unclear and must not introduce readable text, branding, unsupported efficacy claims, fabricated dimensions, fabricated components, or invented technical structure.

## Product Facts
- Product code: `product_009`
- Chinese name: `一次性使用亲水涂层导管鞘套装`
- English name: `Introducer Set`
- Owner company: `1`
- Product owner type: `瑛泰医疗`
- Lifecycle stage: `已注册`
- Indication content: `该产品用于介入手术中扩大桡动脉经皮切口，建立导管导入血管的通道。`
- Registration certificate:
  - `注册证名称：一次性使用亲水涂层导管鞘`
  - `注册证号：国械注准20203031014`
  - `生效时间：2020.12.31`

## Milestones
1. Record the request, hard constraints, output path, and verification target.
2. Run exactly one native image generation request.
3. Persist the generated PNG into the workspace and verify it exists as a square PNG.
4. Run the default closeout cleanup preview and record the result.

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-009-showroom-cover-single-native.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-009-showroom-cover-single-native.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-009-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-009-showroom-cover-single-native --mode preview`

## Milestone Status
- Milestone 1: Completed. Task record, constraints, output path, and verification targets were created under `doc/tasks/20260522-product-009-showroom-cover-single-native/`.
- Milestone 2: Completed. Exactly one native generation request was executed through `codex.cmd exec`; it wrote source PNG `C:\Users\BJB110\.codex\generated_images\019e4fa0-d505-7471-9d36-172622399423\ig_06e2f044aa35ce45016a104a797c608191a38aa2c09fcc2aa0.png`.
- Milestone 3: Completed. The generated PNG was copied to `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-009-showroom-cover-single-native.png` and verified as square.
- Milestone 4: Completed. Closeout preview reported one disposable task sidecar log and no blocked paths or warnings.

## Current Status
Completed. The requested single native showroom-cover PNG was generated, visually inspected for a clean single-hero medical-device concept with no readable text, copied into the workspace, and verified at `1254x1254`.

## Completed Work
- Created the task record and execution log for this image-generation task.
- Executed exactly one native image generation request for `product_009`.
- Recovered the generated source PNG from the Codex native generated-images directory after the CLI session ended without writing a last-message path file.
- Copied the generated PNG into the workspace output folder.
- Verified output existence and square dimensions.
- Ran task-closeout-cleanup in preview mode and confirmed only one disposable stdout log would be removed.

## Verification Evidence
- Source generated PNG: `C:\Users\BJB110\.codex\generated_images\019e4fa0-d505-7471-9d36-172622399423\ig_06e2f044aa35ce45016a104a797c608191a38aa2c09fcc2aa0.png`
- `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-009-showroom-cover-single-native.png'` -> `True`
- `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-009-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }` -> `1254x1254`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-product-009-showroom-cover-single-native --mode preview` -> `status: ready`, keep set includes only the task core files plus the final PNG, delete set includes only `doc/tasks/20260522-product-009-showroom-cover-single-native/codex-stdout.txt`

## Remaining Blockers
- None.

## Cleanup Keep
- `yudao-module-showroom/output/imagegen/product-009-showroom-cover-single-native.png`
- `doc/tasks/20260522-product-009-showroom-cover-single-native/task.md`
- `doc/tasks/20260522-product-009-showroom-cover-single-native/execution-log.md`
