# Task: Showroom cover image for product_001

## Goal

Generate one square PNG showroom cover image for `product_001` using only the provided product facts. The output must be a premium medical-device hero visual suitable for a showroom cover and must not include readable text, logos, watermarks, or invented technical claims.

## Milestones

- [x] M1: Create the task record and define the output requirement.
- [x] M2: Generate the image exactly once with the native image generator.
- [x] M3: Save the generated PNG to a stable local path and record verification.
- [x] M4: Mark the task complete and record final status.

## Expected Verification

- A PNG file exists at the final absolute local filesystem path returned to the user.
- The image is square and visually matches the provided product facts and constraints.
- The image contains no readable text, logo, watermark, badge, or UI overlay.

## Current Status

Completed on 2026-05-21. One fresh square PNG showroom cover image was generated with the native image generator and saved as the project artifact `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\showroom-cover-product_001-20260521.png`.

## Final Verification Result

- PASS: Final project PNG exists at `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\showroom-cover-product_001-20260521.png`.
- PASS: Image dimension check returned `1254x1254`.
- PASS: Visual inspection confirms a single premium medical-device hero visual with no readable text, logo, watermark, badge, or UI overlay.
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-cover-product-001-image --mode preview` returned `status: ready`.
