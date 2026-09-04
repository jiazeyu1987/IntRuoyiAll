# Bug Regression Evidence

## Bug summary and expected behavior

Registration certificate upload approval was blocked after the user confirmed two requirement changes:

1. Registration certificate upload/current-list frontend verification must no longer require company authorization.
2. Registration manager `chudongchuan` must be able to approve upload requests through the real frontend after having role `dcc_registration_certificate_approver` and permission `dcc:registration-certificate:upload:approve`.

Expected behavior: `wanglixuan` can submit a registration certificate upload through the frontend; `chudongchuan` can find and approve the BPM task through the frontend; after approval, the certificate appears in the current registration certificate list.

## Reproduction command or path

- Frontend-only E2E path: open `http://localhost:8097`, login through the real login page, navigate to registration certificate upload/current-list and Approval Center, submit and approve via visible UI controls.
- Failed evidence before final fix:
  - `artifacts/upload-front-only-20260904074118.json`: current list returned `1081001002` / `User has no enabled company scope`.
  - `artifacts/upload-front-only-20260904080710.json`: manager review returned `1080000275` / `未配置注册证提醒任务`.

## Root Cause

1. Company authorization failure: `yudao-server-exec.jar` embedded a stale `yudao-module-dcc-2026.04-SNAPSHOT.jar`; the jar class still called `MdmCompanyScopeApi.getEnabledCompanyIdsForUser(...)` even though source had already been changed to no longer use company authorization for the registration certificate current list.
2. Reminder job failure: `DccRegistrationCertificateBusinessEventNotificationConfigService` still read recipient role IDs and permissions from `infra_job.handler_param`. Migration `20260903_dcc_registration_certificate_threshold_recipient_config.sql` moved recipients to `dcc_registration_certificate_reminder_config.threshold_recipient_user_ids_json` and reduced the Quartz job parameter to actor metadata, so the old reader rejected valid migrated runtime data.

## Regression test added or updated

- Updated `DccRegistrationCertificateUploadServiceTest#listOwnerCompaniesReturnsTenantOwnedCandidatesWithoutCompanyScope`.
- Updated `DccRegistrationCertificateQueryServiceTest#pageListsTenantCurrentCertificatesWithoutCompanyScopeAndAuditsReturnedObjects`.
- Added `DccRegistrationCertificateBusinessEventNotificationConfigServiceTest#resolveRecipientUserIdsReadsThresholdRecipientConfigInsteadOfReminderJobParam`.
- Added `DccRegistrationCertificateBusinessEventNotificationConfigServiceTest#missingActiveConfigFailsClearlyWithoutDefaultingToJobParam`.
- Added `DccRegistrationCertificateBusinessEventNotificationTest#configuredRecipientUserIdsSendWithoutRoleCompanyScopeAndIncludeActor`.

## RED command and expected failure

- RED: frontend-only E2E `upload-front-only.cjs` -> `artifacts/upload-front-only-20260904074118.json` failed because the running server jar still required company scope for the current registration certificate page.
- RED: frontend-only E2E `upload-front-only.cjs` -> `artifacts/upload-front-only-20260904080710.json` failed because upload approval tried to resolve business-event notification recipients from the old Quartz job parameter and returned `未配置注册证提醒任务`.

## GREEN: command and passing result

- `mvn.cmd -pl yudao-module-dcc -Dtest=DccRegistrationCertificateUploadServiceTest#listOwnerCompaniesReturnsTenantOwnedCandidatesWithoutCompanyScope test` -> PASS, 1 test.
- `mvn.cmd -pl yudao-module-dcc -Dtest=DccRegistrationCertificateQueryServiceTest#pageListsTenantCurrentCertificatesWithoutCompanyScopeAndAuditsReturnedObjects test` -> PASS, 1 test.
- `mvn.cmd -pl yudao-module-dcc -Dtest=DccRegistrationCertificateBusinessEventNotificationConfigServiceTest test` -> PASS, 2 tests.
- `mvn.cmd -pl yudao-module-dcc -Dtest=DccRegistrationCertificateBusinessEventNotificationTest test` -> PASS, 8 tests.
- `mvn.cmd -pl yudao-module-dcc -DskipTests install` -> PASS.
- `mvn.cmd -pl yudao-server -DskipTests package` -> PASS.
- Frontend-only Playwright E2E `upload-front-only.cjs` -> PASS, result file `artifacts/upload-front-only-20260904082602.json`.

## Risk and regression scope

- Registration certificate current-list and upload company candidates no longer depend on user company authorization; download/old-certificate access policies remain separate and were not broadened by this fix.
- Business-event notifications now read configured reminder recipients from the tenant reminder config table. The old role/company-scope notification path remains available in `DccRegistrationCertificateBusinessEventNotificationService#send(...)`; the upload approval notifier now uses the explicit configured-user path.
- Notification platform failures, invalid template parameters, invalid events, and empty message IDs still fail clearly; the fix does not swallow notification errors.

## Verification

The final verification combined targeted regression tests, DCC module installation, server packaging, task-owned backend restart, and a frontend-only Playwright E2E run. The passing frontend result is `artifacts/upload-front-only-20260904082602.json`.

## Blockers and follow-up actions

- No current blocker for the verified upload path through E2E-7. The latest full-script frontend-only run `registration-upload-ui-only-e2e-result.json` used run key `20260904101823` and passed self-production plus entrusted-production upload, approval, current-list detail, production-mode display, and manager direct download.
- E2E-8 remains blocked because no confirmed same-tenant ordinary user C credentials are available through the frontend-only constraints.
- E2E-9 remains blocked because it depends on E2E-8 authorization and an over-24-hour state produced by natural time or a product-approved frontend/business-date path.
