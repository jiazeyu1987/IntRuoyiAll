# Task: Showroom Public Display Distinct Hall Products

## Goal

Upgrade the local public display verification data from the temporary single-product mapping shape to a more realistic 8-hall / 8-distinct-product shape, while keeping anonymous public display endpoints green.

## Scope

- Local `ruoyi-vue-pro` MySQL showroom live data
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-public-display-distinct-hall-products\**`

## Non-Scope

- No Website frontend code changes
- No Java contract changes
- No schema changes
- No fallback logic

## Milestones

1. Identify candidate live products whose current revisions already match published ZH/EN narration rows.
2. Rebuild hall-product mapping so each hall points at one distinct candidate product.
3. Publish minimal preview asset rows for those candidate products.
4. Re-verify anonymous app-config and real Website rendering.

## Expected Verification

- `Invoke-WebRequest http://127.0.0.1:48081/showroom/display/app-config`
- SQL inspection of `showroom_hall_product` and `showroom_preview_asset_version`
- Real browser probe through the Website root route

## Current Status

- Status: Completed
- Completed work:
  - Identified 8 candidate products whose current revisions already match published ZH/EN narration rows: `240, 241, 242, 243, 245, 246, 248, 251`.
  - Published preview asset rows for those 8 products using the shared local preview file `2272`.
  - Rebuilt `showroom_hall_product` so each hall now maps to a different candidate product.
  - Verified anonymous `GET /showroom/display/app-config` still returns `200`.
  - Verified the Website root route can render distinct hall titles and product titles from the real runtime.
- Remaining blockers:
  - None.

## Final Verification Result

- PASS: anonymous `GET /showroom/display/app-config`
- PASS: hall mapping now uses 8 distinct products
- PASS: real browser probe shows distinct hall titles and distinct product titles
