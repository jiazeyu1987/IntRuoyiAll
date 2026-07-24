# Task: Showroom Public Display Live Data Repair

## Goal

Repair the local IntRuoyi showroom live data so anonymous `GET /showroom/display/app-config` succeeds again for the pure Website display flow, without adding fallback logic or weakening live-data validation.

## Scope

- Local `ruoyi-vue-pro` MySQL live showroom data
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-public-display-live-data-repair\**`

## Non-Scope

- No Website frontend code changes
- No Java contract changes unless a real contract bug is discovered
- No schema changes
- No fallback or partial-success behavior

## Milestones

1. Inspect current live app-config failure and identify the exact mapped product/live narration mismatch.
2. Audit the mapped hall products for preview and ZH/EN narration completeness.
3. Apply the minimal local live-data repair needed to make app-config succeed.
4. Re-verify anonymous public display endpoints.

## Expected Verification

- `Invoke-WebRequest http://127.0.0.1:48081/showroom/display/app-config`
- SQL inspection of showroom hall/product/narration live rows

## Current Status

- Status: Completed
- Completed work:
  - Confirmed anonymous `app-config` was failing on live product narration source revision mismatch.
  - Audited the restored historical hall mappings and confirmed they pointed at products without complete preview and ZH/EN narration live resources.
  - Repaired the local public display data into a minimal valid frontstage shape:
    - product `1` preview asset is aligned to current revision `1326`
    - published ZH/EN product narrations for product `1` are aligned to current revision `1326`
  - Verified anonymous `GET /showroom/display/app-config` and `GET /showroom/display/product/1` now return `code=0`.
  - Verified the Website root route can read real company, hall, product, detail, and audio-linked display data from the live IntRuoyi runtime.
- Remaining blockers:
  - None.

## Final Verification Result

- PASS: anonymous `GET /showroom/display/app-config`
- PASS: anonymous `GET /showroom/display/product/1`
- PASS: local live hall mapping now `8` rows across `1` distinct product
- PASS: product `1` preview live row exists with `source_revision_id = 1326`
- PASS: product `1` published ZH/EN narration rows now align to `source_revision_id = 1326`
