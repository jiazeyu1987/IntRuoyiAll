# Backend API Evidence

## Scope

- Endpoint: `GET /admin-api/dcc/controlled-files/{id}/preview-metadata`
- Services: `DccControlledFileQueryServiceImpl#getPreviewMetadata`, `DccControlledPreviewAccessService#prepareAccess`, `DccControlledFileAccessAuditService#recordWatermarkTrace`
- Behavior change: `preview-metadata` no longer treats missing `fileNumber` as a required-field failure.

## API And Data Contract

受控文件元数据中 `fileNumber` 是可选字段。预览元数据响应仍返回 viewer token、token id、nonce、access event code、watermark trace code、文件名、内容类型和水印信息。水印追踪表 `file_number` 在当前测试 schema 中是 `NOT NULL`，因此空编号按空串写入审计字段和 watermark payload。

## Auth, Permissions, Validation, Errors

- Auth and permission behavior unchanged: `getPreviewMetadata` still calls read/preview permission checks before preparing access.
- Required validations preserved: tenantId、userId、fileId、versionId、accessType、purpose、ttlSeconds、privacyMode、watermark payload、访问事件和日志关键字段仍 fail fast。
- Removed validation: `fileNumber` is no longer checked with `requireNotBlank` in preview access or watermark trace creation.
- Error behavior unchanged for real missing required fields; no exception swallowing or default-success response added.

## Required Config, Services, Fixtures, Migrations

No config, service dependency, fixture, or migration change is required for this code slice. The regression uses the existing DCC H2 test schema and real service wiring for preview access/audit/token generation.

## BDD

BDD: 空文件编号受控文件可生成预览元数据 -> Given 受控文件 `fileNumber` 为空且用户有预览权限, When 后端生成 `preview-metadata` 的 viewer token、水印追踪和访问日志, Then 不抛出 `fileNumber is required`，并保留访问审计与水印追踪。

## RED:

`mvn -pl yudao-module-dcc -am "-Dtest=DccControlledPreviewAccessServiceTest#prepareAccess_allowsMissingFileNumberForPreviewMetadata" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期失败为 `IllegalArgumentException: fileNumber is required`。

## GREEN:

- `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledPreviewAccessServiceTest#prepareAccess_allowsMissingFileNumberForPreviewMetadata" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledPreviewAccessServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests。
- `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest#getPreviewMetadata*" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，4 tests。
- `mvn -pl yudao-module-dcc -am "-Dtest=DccOnlineFilePreviewServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests。

## Contract And Integration Verification

`DccControlledFileQueryServiceTest#getPreviewMetadata*` verifies the controller-facing service still builds preview metadata and audit context correctly. `DccControlledPreviewAccessServiceTest` verifies the real access event, watermark trace, access log, and viewer token chain accepts missing `fileNumber`.

## Observability

Access event, watermark trace, access log, request id, source IP and user agent recording remain unchanged. For missing file numbers, watermark trace and payload explicitly contain an empty `fileNumber` value instead of failing before audit persistence.

## Blockers And Downstream Skills

No downstream backend, database, frontend, or E2E implementation blocker remains for the code fix. Current Git closeout is not completed because the workspace has unrelated concurrent dirty changes and ahead commits.
