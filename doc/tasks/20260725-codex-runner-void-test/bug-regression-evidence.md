# Bug Regression Evidence

## Bug Summary

- Runner 协议端点为 `@PermitAll`，没有登录用户上下文。
- Runner 注册和 artifact 上传会写入带 `creator/updater NOT NULL` 约束的表。
- MyBatis 通用填充只能在存在登录用户时填充操作者，导致本机 Runner 注册真实探针失败。

## Expected Behavior

- Runner 协议在 token 校验和管理租户头通过后，应能注册/上传证据，并以固定系统操作者 `codex-runner` 写入审计字段。
- 缺 token、缺管理租户或目标 schema 错误仍必须 fail fast。

## Reproduction

- Real probe: POST `/admin-api/system/codex-test-runner/register` with valid Runner token and `tenant-id=1` -> 500, DB error `Column 'creator' cannot be null`.
- RED: `mvn.cmd -pl yudao-module-system "-Dtest=CodexTestRunnerServiceImplTest,CodexTestArtifactServiceImplTest" test` -> FAIL because `creator/updater` were null.

## Root Cause

- Runner 协议是非登录机器协议，不能依赖 `SecurityFrameworkUtils.getLoginUserId()` 填充审计字段。
- 修复前服务层未对 Runner 自身创建的记录设置系统操作者。

## Regression Tests

- `CodexTestRunnerServiceImplTest#registerRunner_stampsAuditFieldsWithoutLoginUser`
- `CodexTestArtifactServiceImplTest#saveArtifact_stampsAuditFieldsWithoutLoginUser`

## Verification

- GREEN: `mvn.cmd -pl yudao-module-system "-Dtest=CodexTestRunnerServiceImplTest,CodexTestArtifactServiceImplTest" test` -> PASS
- GREEN: `mvn.cmd -pl yudao-module-system "-Dtest=CodexTestRunnerControllerTest,CodexTestRunnerServiceImplTest,CodexTestArtifactServiceImplTest" test` -> PASS

## Blockers

- No remaining blocker for Runner registration.
- The requested downstream “作废测试” execution remains blocked because that test item is absent from current system data.

## Risk

- Scope is limited to Codex Runner service writes.
- No global DB fill behavior or authentication model was changed.
