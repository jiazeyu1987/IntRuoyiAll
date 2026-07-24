# Task: Showroom cover image for product_008

## Goal

Generate one square PNG showroom cover image for `product_008` using only the provided product facts. The output must be a premium medical-device hero visual suitable for a showroom cover and must not include readable text, logos, watermarks, invented claims, or fabricated technical structures beyond a concept medical-device hero visual.

## Milestones

- [x] M1: Create the task record and define the output requirement.
- [ ] M2: Generate the image exactly once with the native image generator. Blocked: the single allowed native request returned upstream `503 Service temporarily unavailable`.
- [ ] M3: Save the generated PNG to a stable local path and record verification.
- [ ] M4: Mark the task complete and record final status.

## Expected Verification

- A PNG file exists at the final absolute local filesystem path returned to the user.
- The image is square and visually matches the provided product facts and constraints.
- The image contains no readable text, logo, watermark, badge, or UI overlay.

## Cleanup Keep

- output/imagegen/showroom-cover-product_008-20260522.png

## Blockers

- Upstream OpenAI Responses `image_generation` service availability.
  Impact: the required PNG artifact was not returned, so `output/imagegen/showroom-cover-product_008-20260522.png` was not created and the one-call constraint prevents another generation attempt in this turn.

## Current Status

Blocked on 2026-05-22. One native OpenAI Responses `image_generation` request was executed with retries disabled at `2026-05-22 20:33:53 +08:00` and failed with upstream `503 Service temporarily unavailable`, so no PNG file exists at `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\output\imagegen\showroom-cover-product_008-20260522.png`.
