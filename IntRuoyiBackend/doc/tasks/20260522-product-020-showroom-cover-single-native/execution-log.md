# Execution Log: 20260522-product-020-showroom-cover-single-native

BDD: Premium medical showroom single cover generation -> Given only the provided facts for product_020 and strict no-text/no-branding/no-fabrication constraints / When exactly one native image generation request is executed / Then one square PNG showroom cover is saved in the workspace with one clear hero product and no fabricated technical claims or exact device internals
RED: `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\output\imagegen\product-020-showroom-cover-single-native.png'` -> FAIL, expected output file does not exist before generation
RED: `python - <single OpenAI Responses image_generation request with model='gpt-5' and tool model='gpt-image-1', retries disabled>` -> FAIL, upstream OpenAI service returned `503 Service temporarily unavailable`; no image payload was returned and no PNG file was created
BLOCKER: the single allowed native image-generation request failed upstream with `503 Service temporarily unavailable`, so the required artifact path does not exist and the task cannot proceed without violating the one-call constraint
