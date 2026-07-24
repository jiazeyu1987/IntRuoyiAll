BDD: upload-preview returns watermark metadata -> Given a valid DCC PDF preview upload request When the backend accepts and stores the temporary PDF Then the JSON response must include the unified watermark payload used by the frontend pre-submit preview

BDD: preview binary response returns watermark metadata header -> Given a user with preview permission requests `/dcc/controlled-files/{id}/preview` for an `ACTIVE` or `SUPERSEDED` revision When the backend returns the protected PDF bytes Then the response must include the base64url watermark header alongside the binary payload

BDD: preview permission and download permission remain unchanged -> Given an unauthorized or disallowed revision state When the caller requests preview or download Then the backend must keep enforcing the existing `ACTIVE/SUPERSEDED` preview and `ACTIVE` download rules without fallback

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileUploadApiTest,DccControlledFilePreviewDownloadApiTest,DccControlledFileQueryServiceTest,DccPdfStampServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL during test compilation because `DccControlledPreviewWatermarkRespVO`, `DccControlledPreviewWatermarkOverlayRespVO`, and `getWatermark()` did not exist yet.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileUploadApiTest,DccControlledFilePreviewDownloadApiTest,DccControlledFileQueryServiceTest,DccPdfStampServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 14 tests green after watermark body/header support and pending-preview original-file fallback were added.

GREEN: direct runtime header verification via PowerShell -> PASS, authenticated `Invoke-WebRequest` against `/admin-api/dcc/controlled-files/{id}/preview` returned `X-DCC-Preview-Watermark` together with the inline PDF response.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, the preview response did not expose `X-DCC-Preview-Watermark` through `Access-Control-Expose-Headers`, so browser JavaScript could not read the custom header on cross-origin preview requests.

GREEN: same Maven command -> PASS after adding `Access-Control-Expose-Headers: X-DCC-Preview-Watermark` to the preview response.

RED: live `/system/auth/login` verification -> FAIL, the runtime raised `class java.lang.String cannot be cast to class [B` inside `OAuth2ClientServiceImpl` cache access, blocking fresh login needed by the DCC browser E2E.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system -Dtest=OAuth2ClientServiceImplTest,OAuth2TokenServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS after removing the broken `@Cacheable` lookup from `OAuth2ClientServiceImpl#getOAuth2ClientFromCache`.
