# 注册证变更审核通过后列表审计重复

## Bug Summary

注册证变更审核通过后，返回注册证当前列表时，页面提示“注册证审计事件已存在”。

## Expected Behavior

用户返回注册证列表或刷新列表时，列表读取应成功。读审计仍保留可追溯事件，但列表刷新产生的成功读审计重复键不应阻断页面。

## Reproduction

- Runtime Log: `2026-09-02 15:35:23` 后用户返回注册证列表，`/admin-api/dcc/registration-certificates/page` 多次请求在 `DccRegistrationCertificateReadAuditService.record` 抛出 `REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT`，页面提示“注册证审计事件已存在”。
- RED Command: `mvn -pl yudao-module-dcc -am "-Dtest=DccRegistrationCertificateQueryServiceTest#pageReadAuditDuplicateDoesNotBlockRepeatedListRefresh" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## BDD

- BDD: 注册证列表重复读取 -> Given 变更审核通过后用户返回注册证列表，When 页面连续或并发请求注册证列表，Then 列表读取成功，读审计保持可追溯且不以“注册证审计事件已存在”阻断页面。

## Root Cause

注册证列表查询为每一行写入成功读审计，事件键由 `requestTraceId + operation + certificateId + SUCCESS` 组成。审核通过后返回列表时，页面刷新或重复请求会产生相同 `PAGE` 读审计事件键；原服务对所有重复键统一抛 `REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT`，导致列表接口失败。

## Regression Test

- 新增 `DccRegistrationCertificateQueryServiceTest#pageReadAuditDuplicateDoesNotBlockRepeatedListRefresh`，证明同一请求上下文重复读取列表不应抛错，且只保留一条 PAGE 审计。
- 新增 `DccRegistrationCertificateAuditServiceTest#repeatableListReadKeepsSingleAuditEventForDuplicatePageSuccess`，锁定列表成功读审计的幂等边界。
- 既有详情重复审计测试继续保留，证明详情读审计仍严格冲突。

## RED

- RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccRegistrationCertificateQueryServiceTest#pageReadAuditDuplicateDoesNotBlockRepeatedListRefresh" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected because second list refresh threw `REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT`.

## Fix

新增显式方法 `DccRegistrationCertificateReadAuditService.recordRepeatableListRead(...)`，仅对 `PAGE` / `OLD_INDEX` 且 `SUCCESS` 的重复审计键按列表刷新语义幂等返回。列表查询和旧证索引使用该方法；详情、失败、提交、审批、正式化等审计继续使用严格 `record(...)`。

## GREEN

- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccRegistrationCertificateAuditServiceTest,DccRegistrationCertificateQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 32 tests, 0 failures, 0 errors.
- E2E: `node doc\tasks\20260902-registration-list-audit-conflict-after-change-approval\registration-change-approval-list-e2e.cjs` with approve-only mode -> PASS. 真实页面审核通过后返回注册证列表，捕获 2 次列表接口响应，均为 HTTP 200 / `code=0`。

## Verification

- 后端定向回归：PASS，32 tests, 0 failures, 0 errors。
- int_main 运行态：PASS，`48081` health 为 `UP`。
- 真实页面 E2E：PASS，审核通过后返回注册证列表未再提示“注册证审计事件已存在”。

## Risk And Regression Scope

- 修复范围限定在当前列表和旧证索引的成功读审计重复键。
- 不放宽详情读审计冲突，不影响变更提交、审批正式化或其它写链路审计。

## Follow-up

- 本次没有提交 Git；主工作区仍存在大量非本任务并行改动。

## Blockers

- 无代码修复阻塞。
- Git 提交/推送未执行，需用户单独授权并处理主工作区并行脏改动边界。
