# Task: 20260524-lipid-resistant-stopcock-list-card

## Goal

Generate exactly one native landscape PNG product-thumbnail image for the medical device product `product_165 / 抗脂三通 / Lipid-resistant Stopcock`, suitable for a product list card.

## Scope

- Record the task, product facts, reference boundary, and verification plan before image generation.
- Perform exactly one native image-generation pass.
- Save one final PNG into the workspace.
- Record verification evidence and run the default closeout cleanup preview.

## Non-Scope

- No code, API, database, frontend, or configuration changes.
- No retry, rerun, fallback, batch generation, or multi-variant selection.
- No product substitution, no extra props, no tabletop scene, and no text or watermark.

## Milestones

1. Check the latest prior task status in the same service repository and create this task record.
2. Lock product facts, official reference availability, output path, and one-shot prompt.
3. Execute exactly one native image generation and save the PNG into the workspace.
4. Verify file existence, PNG format, landscape dimensions, and requested single-subject composition.
5. Run closeout cleanup preview and record the result.

## Expected Verification

- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\lipid-resistant-stopcock-list-card-20260524.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\lipid-resistant-stopcock-list-card-20260524.png'`
  - `Get-Item 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\lipid-resistant-stopcock-list-card-20260524.png' | Select-Object FullName, Length, LastWriteTime, Extension`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\lipid-resistant-stopcock-list-card-20260524.png'); try { '{0}x{1}' -f $img.Width, $img.Height } finally { $img.Dispose() }`
  - `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260524-lipid-resistant-stopcock-list-card --mode preview`

## Current Status

Completed.

- Milestone 1 completed: prior-task check and task initialization completed.
- Milestone 2 completed: product facts, official reference path, output path, and one-shot prompt locked.
- Milestone 3 completed: exactly one native image generation was executed and the final PNG was copied into the workspace.
- Milestone 4 completed: file existence, PNG signature, landscape dimensions, and manual visual review passed.
- Milestone 5 completed: closeout cleanup preview passed and cleanup apply is authorized for task-only helper files.

## Previous Task Check

- Latest standard task document in the same service repository: `ruoyi-vue-pro/doc/tasks/20260524-steerable-catheter-6f20f-list-card/task.md`
- Status after check: `Completed`
- Impact: no open prior task blocks this one-shot image task from starting.

## Product Facts And Reference Boundary

- Product code: `product_165`
- Chinese name: `抗脂三通`
- English name: `Lipid-resistant Stopcock`
- Lifecycle stage: `REGISTERED`
- Indication: `适用于与输液管路或压力监测管路连接以达成液体传输及液路控制。`
- Seed source: `ruoyi-vue-pro/sql/showroom/20260519_showroom_excel_seed.sql`
- Official related product page identified: `https://www.int-medical.com/en/col166/2453`
- Official reference image was used during prompt locking and then removed by closeout cleanup: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-lipid-resistant-stopcock-list-card\artifacts\reference-official.png`
- Reference policy:
  - If the official stopcock image can be obtained, preserve the observable single-stopcock geometry, port layout, housing proportion, and handle style.
  - If no usable official image can be obtained, keep the result constrained to one disposable three-way stopcock style device and avoid inventing unrelated structure.

## Observable Reference Geometry

- The official reference image shows multiple stopcock variants; this task must extract only one single-subject version.
- The core body is a clear transparent three-way stopcock housing with three luer-style ports.
- The top control piece is a white rotary handle / lever assembly with a dark circular center cap.
- The handle body has a softly triangular three-lobed silhouette and shallow raised directional details.
- The ports use white ribbed caps and a compact precision-molded medical-device appearance.
- The result must keep this overall clear-body plus white-handle structure and must not switch to tubing sets, syringes, valves of another class, or multi-product compositions.

## Notes

- Final user-facing delivery must be only one local absolute PNG path.
- The one-shot generation has been completed; no rerun is allowed within this task.

## Completed Work

- Checked the latest prior standard task in `ruoyi-vue-pro/doc/tasks` and confirmed it was already completed.
- Retrieved the official stopcocks reference page and used its observable structure to lock the single-stopcock geometry.
- Executed exactly one native image-generation request.
- Copied the generated source PNG into the workspace target path `ruoyi-vue-pro/output/imagegen/lipid-resistant-stopcock-list-card-20260524.png`.
- Verified file existence, PNG signature, landscape dimensions, and manual composition constraints.
- Ran closeout cleanup preview and apply; deleted only task-only helper files and kept the formal task record files.

## Verification Evidence

- Native source path: `C:\Users\BJB110\.codex\generated_images\019e56a5-ba5a-7353-a1e7-3ac9a8f34002\ig_03455df69427e615016a12180373108191997b195f3cb32ca8.png`
- Final artifact path: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\lipid-resistant-stopcock-list-card-20260524.png`
- Final artifact size: `1682578` bytes
- Final artifact extension: `.png`
- Final artifact dimensions: `1536x1024`
- PNG signature: `89504e470d0a1a0a`
- Manual review result:
  - exactly one stopcock subject is visible
  - the device keeps the clear transparent body, three-port structure, and white top rotary handle consistent with the official reference geometry
  - the product is centered slightly above middle with sufficient whitespace
  - the background is a clean rounded-card icy blue to white gradient with soft blue halo
  - no text, letters, logo, watermark, props, tabletop, or extra objects are present
- Closeout preview result:
  - `status: ready`
  - keep: `task.md`, `execution-log.md`
  - delete: `artifacts/reference-official.png`, `generation-marker.txt`, `prompt-lock.txt`
- Closeout apply result:
  - `status: applied`
  - deleted only `artifacts/reference-official.png`, `generation-marker.txt`, and `prompt-lock.txt`

## Cleanup Keep

- `output/imagegen/lipid-resistant-stopcock-list-card-20260524.png`
- `doc/tasks/20260524-lipid-resistant-stopcock-list-card/task.md`
- `doc/tasks/20260524-lipid-resistant-stopcock-list-card/execution-log.md`

## Final Verification Result

- PASS: `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\lipid-resistant-stopcock-list-card-20260524.png'` -> `True`
- PASS: `Get-Item ... | Format-List FullName,Length,LastWriteTime,Extension` -> `FullName=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\lipid-resistant-stopcock-list-card-20260524.png`, `Length=1682578`, `LastWriteTime=2026/5/24 5:12:39`, `Extension=.png`
- PASS: PNG header check -> `89504e470d0a1a0a`
- PASS: dimension check -> `1536x1024`
- PASS: visual inspection -> one centered lipid-resistant stopcock style device, clean gradient card background, sufficient whitespace, no text, no watermark, no extra objects
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260524-lipid-resistant-stopcock-list-card --mode preview` -> `status: ready`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260524-lipid-resistant-stopcock-list-card --mode apply` -> `status: applied`
