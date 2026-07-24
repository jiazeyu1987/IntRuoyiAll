# Task: generate-product-010-showroom-cover

## Goal

Generate one square PNG showroom cover image for `product_010` based only on the provided product facts, using the native image generation path exactly once.

## Product Facts

- Product code: `product_010`
- Chinese name: `股动脉鞘套装`
- English name: `Femoral Introducer set`
- Lifecycle stage: `已注册`
- Indication content: `用于股动脉介入手术，将导丝、导管等医疗器械插入血管。`
- Registration reference:
  - `注册证名称：一次性使用导管鞘套装`
  - `注册证号：国械注准20213030647`
  - `生效时间：2021.8.18`

## Milestones

1. Create task record and execution log.
2. Run a single native image generation with the constrained prompt.
3. Save the final PNG to a stable local path and record verification.
4. Run closeout cleanup preview.

## Milestone Status

- Milestone 1: Completed. Task record and execution log exist under `doc/tasks/generate-product-010-showroom-cover/`.
- Milestone 2: Completed. Exactly one native image-generation request returned a usable PNG artifact.
- Milestone 3: Completed. The generated PNG was copied to the final output path and verified as a square PNG.
- Milestone 4: Completed. Closeout cleanup preview returned `ready` with only the task records and final PNG kept.

## Expected Verification

- Final PNG exists at:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-010-showroom-cover-single-native.png`
- PNG signature is valid and dimensions are square:
  - `89504E470D0A1A0A`
  - `1254x1254`
- Visual review confirms:
  - one centered premium clinical hero device
  - no readable text, logos, or registration details rendered in-image
- Only one native image generation attempt is used.

## Current Status

- `completed`

## Completed Work

- Task record and execution log created.
- One native image-generation request completed and returned a local PNG artifact.
- The generated artifact was saved to `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-010-showroom-cover-single-native.png`.
- File verification passed for existence, PNG signature, and square dimensions.
- Closeout cleanup preview passed in `ready` state with no delete, blocked, or warning entries.

## Remaining Blockers

- None.

## Cleanup Keep

- `yudao-module-showroom/output/imagegen/product-010-showroom-cover-single-native.png`
- `doc/tasks/generate-product-010-showroom-cover/task.md`
- `doc/tasks/generate-product-010-showroom-cover/execution-log.md`
