BDD: app-config aggregates frontstage showroom data -> Given showroom company, hall, product, preview asset, and narration data exist in IntRuoyi / When Website requests the showroom app-config endpoint / Then the backend must return a single payload with company, hall, product, bilingual text, and bilingual audio URLs.

BDD: app-config fails fast on missing live prerequisites -> Given required live company, product, narration, or preview data is missing / When the app-config endpoint is requested / Then the backend must return an explicit failure instead of fake defaults.

BDD: final cross-repo integration -> Given the provider endpoint and the Website consumer are both complete / When Website runs against the real IntRuoyi runtime / Then end-to-end browsing must use only backend-provided showroom data.
BDD: app-config contract freeze -> Given Website consumer tests already target `company + showrooms[].products[]` / When IntRuoyi freezes the backend payload / Then both repos must share the same stable JSON contract without runtime fallback.

RED: mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, `ShowroomDisplayController.AppConfigPayload` and `getAppConfig()` were missing.

GREEN: mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS

## Implementation Notes

- Added `GET /showroom/display/app-config`.
- Frozen contract sample: `app-config-contract.json`.
- Aggregation is strict fail-fast:
  - company must have live revision, published preview asset, and live ZH/EN narration bound to the same source revision
  - each hall must have published preview asset and non-empty product mappings
  - each product must have live revision, published preview asset, and live ZH/EN narration bound to the same source revision
- Product `cover_image` is not used as a fallback inside `app-config`; missing published preview asset now fails explicitly.

## Outstanding Blockers

- Cross-repo browser integration with `D:\ProjectPackage\Website` was not executed in this repository because the Website consumer task is owned by the separate Website repo.
