# Task: BATCH-COVER-OK Showroom Cover Image

## Goal

Generate one square premium medical-device showroom cover image for product `BATCH-COVER-OK` using only the provided product facts. Persist the final PNG to a stable project-local output path suitable for downstream showroom use.

## Milestones

- [x] M1: Identify the task record and confirm the product-facts-only scope.
- [x] M2: Generate one square PNG with native image generation exactly once.
- [x] M3: Copy the selected PNG to `output/imagegen/showroom-cover-batch-cover-ok-20260521.png`.
- [x] M4: Record verification evidence and mark the task complete.

## Expected Verification

- Exactly one native image-generation call is used.
- Output is a local PNG file at `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\showroom-cover-batch-cover-ok-20260521.png`.
- Prompt uses only provided product facts and avoids prohibited text, branding, and fabricated device claims.
- Output is square and suitable as a showroom cover image.

## Cleanup Keep

- output/imagegen/showroom-cover-batch-cover-ok-20260521.png

## Current Status

- Completed on 2026-05-21
- Completed: M1-M4
- Pending: None
- Blockers: None

## Verification Evidence

- Native image generation used exactly once.
- Final artifact: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\showroom-cover-batch-cover-ok-20260521.png`
- Source built-in image path: `C:\Users\BJB110\.codex\generated_images\019e4a8f-7ba9-7180-ba10-ea651324baa8\ig_02f81d602abf72c8016a0efe4ed51481978696d3e250d365bd.png`
- Verified as PNG dimensions 1254x1254.
- Visual inspection confirmed one premium medical-device hero visual in a bright clinical showroom scene with no readable text, logos, badges, watermarks, or UI overlays.
- Closeout preview keep set includes only `task.md`, `execution-log.md`, and `output/imagegen/showroom-cover-batch-cover-ok-20260521.png` for this task.
