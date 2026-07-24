BDD: showroom cover asset generation -> Given the provided product facts for product_020, When one square premium medical showroom cover image is generated, Then the output should present a single clear hero medical-device concept visual with no readable text, branding, or fabricated technical detail.

RED: automated test command -> FAIL, no code-level automated test applies to this raster image generation task; verification must be file and visual constraint validation.

RED: codex exec --enable image_generation -> FAIL, child Codex run reported "Native image generation tool is unavailable in this session."

BLOCKER: required native image generation capability is not exposed in the current session, so no compliant image file could be created and the target PNG verification could not proceed.
