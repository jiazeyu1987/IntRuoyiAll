# Task: Steerable Catheter 6F-20F List Card Image

## Goal

Generate one landscape PNG product-thumbnail image for the medical device product `product_141 / 可控弯导管 内径6F-20F / Steerable Catheter Tube ID 6F-20F`, suitable for a product list card.

## Milestones

1. Record the current task, lock the prompt, and archive the official reference product image.
2. Perform exactly one native image generation and save the selected PNG into the task artifacts directory.
3. Verify file existence, PNG extension, landscape dimensions, and requested single-subject composition, then mark complete.
4. Run closeout cleanup preview and record the result.

## Expected Verification

- Output file exists under `ruoyi-vue-pro/doc/tasks/20260524-steerable-catheter-6f20f-list-card/artifacts/`.
- Output file is a PNG.
- Output image is landscape-oriented and suitable for card thumbnail use.
- The image contains exactly one centered steerable catheter product subject with ample whitespace.
- The visible shape follows the official product image's observable geometry instead of switching to another catheter product type.
- Only one native image generation is performed.

## Current Status

- Completed

## Previous Task Check

- Latest standard task document in the same service repository: `ruoyi-vue-pro/doc/tasks/20260524-zebra-guide-wire-list-card/task.md`
- Status after check: `Blocked`
- Impact: the previous task has been explicitly blocked because it lacks active instruction to continue its unfinished one-shot image generation, so it does not silently carry over into the current request.

## Product Facts And Reference

- Product code: `product_141`
- Chinese name: `可控弯导管 内径6F-20F`
- English name: `Steerable Catheter Tube ID 6F-20F`
- Lifecycle stage: `已注册`
- Indication: `与扩张器配合使用，用于将导丝、导管等医疗器械插入血管。`
- Registration certificate: `可控弯导管鞘 / 沪械注准20242030241 / 2024.7.19`
- Official product page: `https://int-medical.com/col14/3156`
- Official reference image: `https://int-medical.com/upload/image/2024-07/col14/1721874100659.png`
- Observable reference geometry to preserve:
  - one single medical device product composed of a long dark-blue flexible catheter shaft and an integrated white control handle
  - the shaft forms a clean large curve or loop with a slim uniform profile
  - the handle is white with soft blue accent parts and a compact ergonomic control-body appearance
  - a small side port / branch detail may exist as part of the same product, but must not read as a second object

## Notes

- No retry, fallback, alternate product substitution, or second native generation is allowed in this task.
- Final user-facing delivery must be only one local absolute PNG path.

## Completed Work

- Archived the official product reference image from `https://int-medical.com/upload/image/2024-07/col14/1721874100659.png`.
- Performed exactly one native image generation using the locked single-subject card-thumbnail prompt.
- Saved the generated PNG into the task artifacts directory and verified format, dimensions, and requested composition constraints.

## Verification Evidence

- Source path: `C:\Users\BJB110\.codex\generated_images\019e5686-54f5-7662-a4ee-db7e2550855a\ig_017eb2614b195141016a121023ef088191be9bdcc910fbc5e9.png`
- Output path: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-steerable-catheter-6f20f-list-card\artifacts\steerable-catheter-6f20f-list-card.png`
- Reference path: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-steerable-catheter-6f20f-list-card\artifacts\reference-official.png`
- File existence: `True`
- Extension: `.png`
- PNG signature: `89504E470D0A1A0A`
- Dimensions: `1536x1024` (landscape)
- Native generation count: `1`
- Manual review: the result shows one complete steerable catheter product with a long dark-blue curved shaft, one integrated white-and-blue control handle, clean ice-blue card background, ample whitespace, and no text, watermark, props, or extra objects.
- Closeout preview: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260524-steerable-catheter-6f20f-list-card --mode preview` returned `status: ready` with only keep entries and no delete, blocked, or warning paths.

## Cleanup Keep

- `doc/tasks/20260524-steerable-catheter-6f20f-list-card/artifacts/steerable-catheter-6f20f-list-card.png`
- `doc/tasks/20260524-steerable-catheter-6f20f-list-card/artifacts/reference-official.png`
- `doc/tasks/20260524-steerable-catheter-6f20f-list-card/task.md`
- `doc/tasks/20260524-steerable-catheter-6f20f-list-card/execution-log.md`
- `doc/tasks/20260524-steerable-catheter-6f20f-list-card/prompt-lock.txt`
