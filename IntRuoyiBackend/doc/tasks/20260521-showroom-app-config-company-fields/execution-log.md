BDD: anonymous app-config returns company public fields -> Given the showroom company live revision already has public display fields / When Website requests anonymous `GET /showroom/display/app-config` / Then the `company` payload must include `publicFields` in display order with `{ label, value }` entries.

BDD: anonymous app-config allows explicit empty company public fields -> Given the showroom company live revision has no populated public fields in the allowed display set / When anonymous `GET /showroom/display/app-config` is requested / Then the backend must still return `company.publicFields: []` instead of omitting the field.

BDD: app-config company fields contract remains valid within the current public display surface -> Given the display controller exposes the current showroom public display routes / When anonymous access policy is verified / Then `getAppConfig()` must remain public and the `company.publicFields` contract must stay compatible with the active display surface.

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#appConfigEndpointShouldBeAnnotatedPermitAllWithoutOpeningOtherDisplayRoutes+appConfigShouldAggregateCompanyHallProductAndBilingualMedia+appConfigShouldReturnExplicitEmptyCompanyPublicFieldsWhenNoCompanyDisplayFieldHasValue" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `AppConfigCompany` 仍缺少 `publicFields()`，Website 侧新契约断言无法编译通过。

INFO: current worktree 的 app-config 字段契约验证被拆分到独立 `ShowroomAppConfigCompanyFieldsContractTest`，并按当前 showroom 公开展示面校验访问边界，避免把字段合同与其他业务链路耦合在一起。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomAppConfigCompanyFieldsContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
