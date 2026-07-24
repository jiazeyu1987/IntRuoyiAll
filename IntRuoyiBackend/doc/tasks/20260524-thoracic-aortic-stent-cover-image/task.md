# Task: Thoracic Aortic Stent Cover Image

## Goal

Generate exactly one native landscape PNG product-thumbnail image for the `Thoracic Aortic Stent` bare-stent product, suitable for a product list card.

## Scope

- Create exactly one native image generation prompt and one PNG output only.
- Keep the composition minimal, bright, clinical, centered, and catalog-oriented.
- Save the final image into the workspace and record verification evidence.

## Non-Scope

- No multi-image batch generation.
- No UI, backend, database, or API changes.
- No text, logo, watermark, people, props, tables, or complex scene elements.
- No product substitution with any device other than a thoracic aortic bare stent.

## Milestones

1. Create the task record and define verification.
2. Generate one native landscape PNG that matches the product-thumbnail constraints.
3. Verify the PNG exists locally with landscape dimensions, then mark the task complete.

## Expected Verification

- `Test-Path "D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-thoracic-aortic-stent-cover-image\artifacts\thoracic-aortic-stent-cover.png"`
- `Get-Item "D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-thoracic-aortic-stent-cover-image\artifacts\thoracic-aortic-stent-cover.png" | Select-Object FullName, Length, LastWriteTime`
- `Get-Item "D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-thoracic-aortic-stent-cover-image\artifacts\thoracic-aortic-stent-cover.png" | Select-Object -ExpandProperty Extension`
- `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-thoracic-aortic-stent-cover-image\artifacts\thoracic-aortic-stent-cover.png'); try { '{0}x{1}' -f $img.Width, $img.Height } finally { $img.Dispose() }`
- Output image is a PNG and landscape-oriented.
- Only one native image generation is performed.

## Milestone Status

### Milestone 1

- Status: Completed
- Completed work:
  - Created the task directory and baseline task records.
  - Captured the single-generation, no-text, single-subject constraints.
  - Checked the repository for a reference product image and found only product seed data, not a usable source image.
- Verification evidence:
  - `doc/tasks/20260524-thoracic-aortic-stent-cover-image/task.md`
  - `doc/tasks/20260524-thoracic-aortic-stent-cover-image/execution-log.md`
  - `sql/showroom/20260519_showroom_excel_seed.sql`
- Remaining blockers:
  - The single native image has not been generated yet.

### Milestone 2

- Status: Completed
- Completed work:
  - Ran exactly one native image generation request.
  - Produced one source PNG at `C:\Users\BJB110\.codex\generated_images\019e5605-3ac4-73b1-b9bc-4f375f2b4d0f\ig_0e39872c7059b782016a11ee2c20e881918b850c5e7f393ae5.png`.
  - Copied the source PNG into the final workspace path `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-thoracic-aortic-stent-cover-image\artifacts\thoracic-aortic-stent-cover.png`.
- Verification evidence:
  - `C:\Users\BJB110\.codex\generated_images\019e5605-3ac4-73b1-b9bc-4f375f2b4d0f\ig_0e39872c7059b782016a11ee2c20e881918b850c5e7f393ae5.png`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-thoracic-aortic-stent-cover-image\artifacts\thoracic-aortic-stent-cover.png`
- Remaining blockers:
  - None.

### Milestone 3

- Status: Completed
- Completed work:
  - Verified that the final artifact exists locally.
  - Verified the file extension is `.png`.
  - Verified PNG landscape dimensions `1536x1024`.
  - Ran closeout cleanup preview for the task id.
- Verification evidence:
  - `Test-Path` returned `True` for the final PNG path.
  - `Get-Item ... | Select-Object -ExpandProperty Extension` returned `.png`.
  - `Add-Type -AssemblyName System.Drawing ...` returned `1536x1024`.
  - `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260524-thoracic-aortic-stent-cover-image --mode preview`
- Remaining blockers:
  - None.

## Current Status

- Status: Completed
- Completed work:
  - Task documentation created.
  - Repository searched for product reference material.
  - Exactly one native image generation request completed.
  - Final artifact saved to `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-thoracic-aortic-stent-cover-image\artifacts\thoracic-aortic-stent-cover.png`.
  - Final artifact verified as PNG landscape `1536x1024`.
  - Closeout cleanup preview executed successfully.
- Remaining blockers:
  - None.

## Final Verification Result

- PASS: `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-thoracic-aortic-stent-cover-image\artifacts\thoracic-aortic-stent-cover.png'` -> `True`
- PASS: `Get-Item 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-thoracic-aortic-stent-cover-image\artifacts\thoracic-aortic-stent-cover.png' | Select-Object -ExpandProperty Extension` -> `.png`
- PASS: `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-thoracic-aortic-stent-cover-image\artifacts\thoracic-aortic-stent-cover.png'); try { '{0}x{1}' -f $img.Width, $img.Height } finally { $img.Dispose() }` -> `1536x1024`
- PASS: `Get-Item 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-thoracic-aortic-stent-cover-image\artifacts\thoracic-aortic-stent-cover.png' | Select-Object FullName, Length, LastWriteTime` -> file metadata returned for the final artifact.
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260524-thoracic-aortic-stent-cover-image --mode preview` -> preview ready; keep list contains only `task.md`, `execution-log.md`, and the final PNG artifact.

## Cleanup Keep

- doc/tasks/20260524-thoracic-aortic-stent-cover-image/artifacts/thoracic-aortic-stent-cover.png
