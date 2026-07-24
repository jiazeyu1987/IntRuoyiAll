# Task: 批记录对图修正二轮

## Goal

继续缩小本地电子批记录预览与目标图片的差距，优先解决顶部“产品信息表”和后续工序表在视觉上被拆开的差异，让预览更接近图片中的完整单页观感。

## Scope

- 仅修改前端本地预览层。
- 在导入页预览和列表抽屉预览中，将同一次导入的顶部产品信息表与后续工序表做组合显示。
- 不改后端 parser、不改 DCC、不改其他无关页面。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260517-batch-record-image-compare-fix/task.md`
- Status before this task: completed.
- Impact: merge-span fidelity and vertical section labels are already repaired, so this follow-up focuses on single-page visual continuity.

## BDD Scenarios

- BDD: process preview should include the leading product-information section -> Given the same import session contains a source-table-1 product-information template and a later process template, When the operator previews the process template, Then the preview includes the leading product-information section above the process form.
- BDD: list drawer preview should preserve the same combined page feel -> Given an imported process template is opened from the template list, When the read-only drawer preview opens, Then it includes the same leading product-information section when that sibling template is available in the current list data.

## Milestones

1. [x] M1: Create this follow-up task package and record the remaining visual gap.
2. [x] M2: Add RED evidence showing process previews currently exclude the product-information header section.
3. [x] M3: Implement preview-layer combination of source-table-1 with later process templates.
4. [ ] M4: Re-run real DOC verification and confirm the combined preview is closer to the target image.
5. [ ] M5: Update evidence and commit only task-scoped frontend files.

## Expected Verification

- Rough-wash preview includes the top product-information section in the same preview page.
- Real DOC import still succeeds and preview remains readable.

## Current Status

Blocked pending login interaction repair. The preview layer now supports prepending the source-table-1 product-information section above later process templates in both import preview and drawer preview, but fresh live-browser verification is currently blocked because the login page interaction sometimes does not fire the actual login request from the login form.

## Blocker And Impact

- Blocker: the shared `XButton` login submit interaction is unstable, so browser verification can stop before any `/system/auth/login` request is sent.
- Impact: the phase2 preview-composition code is implemented, but the final real-browser screenshot comparison cannot be truthfully closed until the login interaction is repaired.
