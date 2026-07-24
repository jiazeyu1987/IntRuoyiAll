# Task: 20260524-approval-display-probe-95177868-showroom-cover-single-native

## Goal
Create one horizontal premium medical-device showroom cover image for `审批展示探针95177868` (`Approval display probe 95177868`) for use as a product-list card thumbnail, using exactly one native image-generation pass. The result must contain a single centered medical-device subject only, no text, no logo, no watermark, no extra props, and sufficient white space.

## Milestones
1. Record the request, constraints, output path, and verification target.
2. Run exactly one native image generation request.
3. Copy the generated PNG into the workspace and verify it exists as a horizontal PNG.
4. Run the default closeout cleanup preview and record the result.

## Expected Verification
- Final artifact path:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\approval-display-probe-95177868-showroom-cover-single-native.png`
- Commands:
  - `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\approval-display-probe-95177868-showroom-cover-single-native.png'`
  - `Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile('D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\approval-display-probe-95177868-showroom-cover-single-native.png'); try { Write-Output ($img.Width.ToString() + 'x' + $img.Height.ToString()) } finally { $img.Dispose() }`
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260524-approval-display-probe-95177868-showroom-cover-single-native --mode preview`

## Current Status
Completed.

- Milestone 1 completed: request, constraints, output path, and verification targets were recorded.
- Milestone 2 completed: exactly one native image-generation pass was executed for this task.
- Milestone 3 completed: the generated PNG was copied to `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\approval-display-probe-95177868-showroom-cover-single-native.png` and verified as `1672x941`.
- Milestone 4 completed: closeout cleanup preview ran successfully and reported no removable task artifacts.

## Cleanup Keep
- `yudao-module-showroom/output/imagegen/approval-display-probe-95177868-showroom-cover-single-native.png`
- `doc/tasks/20260524-approval-display-probe-95177868-showroom-cover-single-native/task.md`
- `doc/tasks/20260524-approval-display-probe-95177868-showroom-cover-single-native/execution-log.md`
