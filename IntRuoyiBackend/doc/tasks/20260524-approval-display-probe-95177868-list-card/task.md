# Task: 20260524-approval-display-probe-95177868-list-card

## Goal

Generate one native landscape PNG medical-device product-thumbnail image for `审批展示探针95177868`, suitable for a product list card. Use exactly one native image-generation pass and keep the result as a single centered medical-device subject with ample whitespace, no text, no logo, no watermark, no props, and no complex environment.

## Scope

- Create exactly one native image-generation request.
- Save one final PNG into the workspace.
- Record verification and closeout preview evidence under this task directory.

## Non-Scope

- No batch generation.
- No code, API, database, or frontend changes.
- No fallback assets, placeholders, or mock success output.

## Milestones

1. Record the request, constraints, output path, and verification plan.
2. Execute exactly one native landscape image generation request.
3. Save the PNG into the workspace and verify file existence plus landscape dimensions.
4. Run the default closeout cleanup preview and record the result.

## Expected Verification

- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\approval-display-probe-95177868-list-card-20260524.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\approval-display-probe-95177868-list-card-20260524.png'`
  - `Get-Item 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\approval-display-probe-95177868-list-card-20260524.png' | Select-Object FullName, Length, LastWriteTime, Extension`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\approval-display-probe-95177868-list-card-20260524.png'); try { '{0}x{1}' -f $img.Width, $img.Height } finally { $img.Dispose() }`
  - `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260524-approval-display-probe-95177868-list-card --mode preview`

## Current Status

Completed.

- Milestone 1 completed: request, product mapping, output path, and verification plan recorded.
- Milestone 2 completed: exactly one native image-generation pass produced source PNG `C:\Users\BJB110\.codex\generated_images\019e5601-23d4-7b51-95b2-4caba605c06f\ig_027fd937e81ca63e016a11ec8bf18081919e6ae16596513886.png`.
- Milestone 3 completed: the final PNG was copied to `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\approval-display-probe-95177868-list-card-20260524.png` and verified as landscape `1536x1024` with `.png` extension.
- Milestone 4 completed: closeout cleanup preview returned `status: ready`, and cleanup apply removed the task-only prompt artifact `imagegen-prompt.txt`.

## Product Mapping Note

- The user-facing request name is `审批展示探针95177868`.
- Repository traceability for the same test-linked product points to `product_001`, seeded as `三通旋塞-OFF / Manifold for Single use-OFF`.
- No real reference product photo was found in the workspace. The generation prompt therefore constrains the subject to a single disposable three-way stopcock style device body and avoids unrelated product substitution.

## Cleanup Keep

- `output/imagegen/approval-display-probe-95177868-list-card-20260524.png`
- `doc/tasks/20260524-approval-display-probe-95177868-list-card/task.md`
- `doc/tasks/20260524-approval-display-probe-95177868-list-card/execution-log.md`

## Final Verification Result

- PASS: `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\approval-display-probe-95177868-list-card-20260524.png'` -> `True`
- PASS: `Get-Item ...` reported `FullName=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\approval-display-probe-95177868-list-card-20260524.png`, `Length=1640739`, `LastWriteTime=2026-05-24 02:07:14`, `Extension=.png`
- PASS: `Add-Type -AssemblyName System.Drawing; ...` -> `1536x1024`
- PASS: visual inspection confirmed one centered three-way stopcock style medical-device subject, ample whitespace, no text, no logo, no watermark, no props, and no tabletop scene
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260524-approval-display-probe-95177868-list-card --mode preview` -> `status: ready`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260524-approval-display-probe-95177868-list-card --mode apply` -> `status: applied`, deleted only `doc/tasks/20260524-approval-display-probe-95177868-list-card/imagegen-prompt.txt`
