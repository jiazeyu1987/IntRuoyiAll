BDD: anonymous app-config should succeed with current mapped live products -> Given hall mappings point at published live products with matching published ZH/EN narration and preview assets / When anonymous `GET /showroom/display/app-config` is requested / Then the endpoint must return 200 with real aggregated data.

BDD: runtime still fails fast on broken mapped product live data -> Given a mapped live product is missing or mismatched in preview or narration / When anonymous `GET /showroom/display/app-config` is requested / Then the backend must expose the exact missing prerequisite instead of synthesizing data.

RED: current anonymous `GET /showroom/display/app-config` -> FAIL, runtime returns a live product narration mismatch error.

INFO: historical hall mapping restoration had reintroduced 166 mapped products with no complete current live preview + ZH/EN narration bundle, so the public frontstage contract could not succeed as-is.

GREEN: local live data realignment -> PASS, all 8 halls now map to product `1`, product `1` preview asset is published for revision `1326`, and published ZH/EN product narration rows align to revision `1326`.

GREEN: anonymous `GET /showroom/display/app-config` -> PASS

GREEN: anonymous `GET /showroom/display/product/1` -> PASS

GREEN: real Website browser probe against `http://127.0.0.1:4174/` -> PASS, root kiosk rendered real company copy, hall title, product detail fields, and backend audio sources from IntRuoyi.
