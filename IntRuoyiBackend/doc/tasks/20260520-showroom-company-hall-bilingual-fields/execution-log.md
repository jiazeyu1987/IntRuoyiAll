BDD: company and hall app-config fields are bilingual -> Given Website needs full bilingual switching / When IntRuoyi emits app-config / Then company and hall payloads must include Chinese and English fields.

BDD: runtime remains anonymous for app-config -> Given app-config is a frontstage endpoint / When an unauthenticated client requests it / Then the endpoint stays anonymously readable and returns the expanded bilingual structure.

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomFoundationContractTest,ShowroomHttpApiIntegrationTest#appConfigShouldAggregateCompanyHallProductAndBilingualMedia" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, company/hall English fields were missing from schema, DTOs, and runtime aggregation.

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomFoundationContractTest,ShowroomCompanyContentTest,ShowroomHallContentTest,ShowroomPersistentContentServiceTest,ShowroomHttpApiIntegrationTest#appConfigShouldAggregateCompanyHallProductAndBilingualMedia" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS

GREEN: local MySQL migration -> PASS, `showroom_company.display_name_en`, `showroom_hall.name_en`, and `showroom_hall.description_en` now exist.

GREEN: runtime probe -> PASS, `GET /showroom/display/app-config` now returns `company.nameEn` and `showrooms[].nameEn/descriptionEn`.
