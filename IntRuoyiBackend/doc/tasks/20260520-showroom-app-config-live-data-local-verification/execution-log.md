BDD: anonymous app-config should expose real local showroom data -> Given anonymous auth is already opened for `GET /showroom/display/app-config` / When the local runtime has complete live company, hall, and product data / Then the endpoint should return 200 with the confirmed app-config JSON contract.

BDD: local verification should still fail fast on missing live data -> Given the local runtime is missing required preview or narration data / When `GET /showroom/display/app-config` is requested / Then the endpoint must report the exact missing live prerequisite instead of falling back.

RED: `GET http://127.0.0.1:48081/showroom/display/app-config` -> FAIL, `{"success":false,"message":"SHOWROOM_TARGET_NOT_FOUND: live company preview asset is required","code":500,"result":null}`

INFO: local live inventory before backfill
- company current revision -> `showroom_company.id = 1`, `current_revision_id = 4`
- company published ZH/EN narration -> exists
- hall published preview rows -> `8`
- company published preview rows -> `0`
- product published preview rows -> `0`
- current hall mappings -> point to many old product ids, not the only current live product

GREEN: execute `local-app-config-backfill.sql` -> PASS

GREEN: live data after backfill
- company preview -> `target_type='COMPANY', target_id=1, source_revision_id=4, image_file_id=2272, status='PUBLISHED'`
- product preview -> `target_type='PRODUCT', target_id=172, source_revision_id=1173, image_file_id=2272, status='PUBLISHED'`
- product narration -> `target_id=172`, both `ZH` and `EN` now `PUBLISHED`
- hall mapping -> `hall_id 1..8` all map to `product_id=172`, `display_order=1`

GREEN: `GET http://127.0.0.1:48081/showroom/display/app-config` -> PASS, HTTP 200 and `code=0`

## Outstanding Blockers

- None for this local verification scope.
