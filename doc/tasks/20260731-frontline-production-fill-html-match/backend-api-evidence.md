# Backend API Evidence

## Scope

- Endpoint/service scope: MES frontline fixed-template catalog/payload validation, frontline report+recordbook submit splitter, and process-pool submit-event adapter.
- Owned backend files:
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/template/FrontlineTemplateServiceImpl.java`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/template/FrontlineTemplateFieldCodes.java`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesProFrontlineRecordbookPayloadReqVO.java`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackPayloadSplitter.java`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolSubmitEventCreateReqBO.java`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolSubmitEventServiceImpl.java`

## API Contract

- Production simplified template fields are now exactly `DEVICE`, `DEVICE_PARAMETERS`, `OUTPUT_QUANTITY`, and `SCRAP_QUANTITY`.
- `PREVIOUS_PROCESS_INPUT_QUANTITY` is rejected as an unknown production template field.
- Frontline recordbook payload no longer has required `previousProcessInputQuantity`.
- Process-pool submit quantity fragments no longer include `previousProcessInputQuantity` in fragment raw payload.

## Data Contract

- No database schema, migration, seed, mock, or persisted data repair was introduced.
- Existing raw record payload is still carried through unchanged, but the owned frontend no longer sends previous-process input quantity.
- Equipment parameters, output quantity, loss quantity, feedback metadata, recordbook metadata, and signature metadata remain unchanged.

## Auth Permissions Validation Errors

- Existing authorization flow remains unchanged through `MesFrontlineSubmitAuthorizationService`.
- Existing signature mismatch rejection remains unchanged and still prevents writes before authorization and persistence.
- Old `PREVIOUS_PROCESS_INPUT_QUANTITY` in formal template payload fails fast with `PRO_FRONTLINE_TEMPLATE_FIELD_INVALID`; no fallback or auto-fill is introduced.

## Required Config Fixtures Migrations

- Required fixtures: existing unit-test builders in `MesProFrontlineFeedbackSubmitTestData`.
- Required services: mocked feedback, recordbook, process-pool event, and authorization services for submit service tests.
- Migrations: none.

## BDD Scenarios

- BDD: 生产模板字段移除 -> Given production simplified template catalog When the service exposes fields Then previous-process input quantity is absent and only device, device parameters, output, and scrap remain.
- BDD: 旧字段拒绝 -> Given a client submits `PREVIOUS_PROCESS_INPUT_QUANTITY` When backend validates production payload Then it fails with `PRO_FRONTLINE_TEMPLATE_FIELD_INVALID`.
- BDD: 记录本拆分不携带旧字段 -> Given frontline submit combines report and recordbook data When payload splitter builds recordbook content Then `previousProcessInputQuantity` is absent.
- BDD: 资源池事件不携带旧字段 -> Given frontline submit creates process-pool quantity fragments When adapter maps fragments Then fragment raw payload does not include `previousProcessInputQuantity`.

## RED

- RED: `mvn -pl yudao-module-mes -am "-Dtest=ProductionTemplateContractTest,FrontlineTemplatePayloadContractTest,MesProFrontlineFeedbackPayloadSplitterTest,MesProFrontlineFeedbackSubmitServiceTest,MesProcessPoolSubmitEventServiceAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: old production template, recordbook VO/splitter, and process-pool event adapter still retained previous-process input quantity.

## GREEN

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=ProductionTemplateContractTest,FrontlineTemplatePayloadContractTest,MesProFrontlineFeedbackPayloadSplitterTest,MesProFrontlineFeedbackSubmitServiceTest,MesProcessPoolSubmitEventServiceAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 11 tests, 0 failures, 0 errors.

## Contract Verification

- `ProductionTemplateContractTest` verifies the field list excludes `PREVIOUS_PROCESS_INPUT_QUANTITY`.
- `FrontlineTemplatePayloadContractTest` verifies valid production payloads no longer require the field and old field submission is rejected.
- `MesProFrontlineFeedbackPayloadSplitterTest` verifies recordbook content and recordbook request VO no longer expose `previousProcessInputQuantity`.
- `MesProFrontlineFeedbackSubmitServiceTest` verifies the submit orchestration writes recordbook content without the old field.
- `MesProcessPoolSubmitEventServiceAdapterTest` verifies quantity fragments and submit-event BO no longer expose the old field.

## Observability

- No new logging, metrics, or tracing was required.
- Existing fail-fast ServiceException behavior remains the observable API error path for unknown formal template fields.

## Blockers

- No backend blocker remains for removing employee-entered previous-process input quantity.
