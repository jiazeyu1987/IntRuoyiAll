BDD: local preview should preserve merged structure from the recognized table -> Given the parser returns merged `rowSpan` and `colSpan`, When the local preview renders the rough-wash form, Then major header and section merges remain visible instead of being flattened.
BDD: local preview should display left-side section labels closer to the target image -> Given the recognized layout includes tall narrow section cells, When the preview renders those cells, Then the left-side section labels are displayed in a vertical-friendly way that better matches the source image.

RED: image-vs-preview compare -> FAIL before implementation, the recognized local preview differed from the target image in three visible ways:
- merged title/header cells were visually flattened
- left-side section labels were horizontal instead of vertical
- the rough-wash title row kept title text and checkbox text on one line

GREEN: `pnpm exec eslint src/views/mes/pro/batchrecordtemplate/TemplateLayoutPreview.vue` -> PASS.

GREEN: compare after implementation -> PASS at the local preview fidelity layer:
- merged structure from parser spans is now rendered instead of flattened
- tall first-column section labels now render with vertical-friendly writing mode
- full-width title rows with checkbox markers now split into two display lines

NOTE: one structural difference remains by design: the top standalone product-info table is still recognized as a separate template, so it is not visually fused into the rough-wash preview card.
