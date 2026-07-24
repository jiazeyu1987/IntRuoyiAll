BDD: showroom cover image from provided facts only -> Given the supplied product facts and image constraints / When one native image generation call is executed / Then a single square PNG showroom cover image is produced and its absolute local path is returned

GREEN: native image generation -> PASS
GREEN: `Get-ChildItem -Path 'C:\Users\BJB110\.codex\generated_images\019e4ece-4585-7db3-b2dd-aeaa51256d26' -File | Select-Object -ExpandProperty FullName` -> PASS
BDD: product_024 premium showroom cover rerun -> Given only the provided product facts and strict no-text constraints / When exactly one native image generation call is executed for a new versioned output / Then one square PNG exists under the current task directory and its absolute path can be returned
RED: `Test-Path 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-product-024-showroom-cover\product_024-showroom-cover-v2.png'` -> FAIL, target output file did not exist before the single native generation run was materialized into the workspace
GREEN: native image generation rerun -> PASS
GREEN: `Get-Item 'D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-product-024-showroom-cover\product_024-showroom-cover-v2.png' | Select-Object -ExpandProperty FullName` -> PASS
