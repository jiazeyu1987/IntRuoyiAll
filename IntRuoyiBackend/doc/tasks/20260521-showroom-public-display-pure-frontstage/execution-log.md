BDD: pure frontstage display can read all required public showroom content anonymously -> Given Website is a display-only frontend / When it reads company, hall, product, and narration data from showroom display routes / Then the required public display routes must be anonymously readable without adding fallback behavior.

BDD: anonymous public display still fails fast on missing live data -> Given required live company, hall, product, or narration data is missing / When the public display routes are requested / Then the backend must expose the exact missing prerequisite rather than synthesizing placeholder content.

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomDisplayCompanyAnonymousContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, hall/product/narration display routes were not yet annotated or exposed for anonymous reads.

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomDisplayCompanyAnonymousContractTest,ShowroomHttpApiIntegrationTest#publicFrontstageDisplayEndpointsShouldBeAnnotatedPermitAllExceptHome" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS

INFO: anonymous route coverage now includes company, app-config, hall detail, product detail, and narration, while `display/home` remains non-public.
