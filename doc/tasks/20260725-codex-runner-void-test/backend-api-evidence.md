# Backend API Evidence

## Scope

- Endpoint group: `/admin-api/system/codex-test-runner/*`
- Services: `CodexTestRunnerServiceImpl`, `CodexTestArtifactServiceImpl`

## Contract

- Runner requests must include a valid `X-Codex-Runner-Token`.
- Runner requests must include management `tenant-id`.
- Controller binds management tenant during Runner service execution despite `@TenantIgnore`.
- Runner-created records stamp `creator/updater=codex-runner` when no login user exists.

## BDD

- BDD: Runner management tenant binding -> Given a Runner request contains `tenant-id` / When controller calls service / Then service executes with that tenant context and tenant ignore disabled.
- BDD: Missing management tenant -> Given a Runner request omits `tenant-id` / When controller handles request / Then it fails fast with `CODEX_TEST_RESULT_SCHEMA_INVALID`.
- BDD: Runner audit user -> Given Runner is unauthenticated but token-valid / When it registers or uploads artifact / Then persisted records have non-null `creator/updater`.

## Validation

- RED: `mvn.cmd -pl yudao-module-system "-Dtest=CodexTestRunnerServiceImplTest,CodexTestArtifactServiceImplTest" test` -> FAIL, Runner-created records had null audit fields.
- GREEN: `mvn.cmd -pl yudao-module-system "-Dtest=CodexTestRunnerControllerTest,CodexTestRunnerServiceImplTest,CodexTestArtifactServiceImplTest" test` -> PASS.

## Verification

- `mvn.cmd -pl yudao-module-system "-Dtest=CodexTestRunnerControllerTest,CodexTestRunnerServiceImplTest,CodexTestArtifactServiceImplTest" test` -> PASS
- `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS
- Local register probe -> `code=0`, `runnerSessionId=1`

## Blockers

- Requested target test item “作废测试” is absent from current system data, so no execution batch can be created for that item.
