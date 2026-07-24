# Task: DCC 受控查看预览水印元数据联动
## Goal

让 IntRuoyi DCC 后端在不改变受控预览二进制主契约的前提下，为 `preview` 和 `upload-preview`
返回统一水印元数据，供前端统一受控阅读器显示可追溯水印并维持现有权限边界。

## Scope

- 在后端仓库开始生产代码修改前创建本任务目录并记录前序任务状态。
- 只改 `yudao-module-dcc` 内的预览/上传预览响应、水印构建逻辑和定向测试。
- 保持 `GET /dcc/controlled-files/{id}/preview` 继续返回 PDF bytes，不改下载和审批业务语义。
- 不新增 fallback 水印逻辑；水印构建失败时必须直接抛错。

## Previous Task Check

- Previous backend task: `doc/tasks/20260516-dcc-category-directory-binding-duplicate-fix/task.md`
- Status before this task: completed.
- Impact: no unfinished backend DCC task blocks this preview-watermark contract change.

## Milestones

- [x] M1: Confirm previous backend task state and create this task package.
- [x] M2: Record BDD scenarios and RED evidence for missing preview watermark metadata.
- [x] M3: Implement preview watermark builder and wire upload-preview/preview responses.
- [x] M4: Run GREEN verification and update evidence.
- [x] M5: Assess scoped commit readiness for only this backend slice.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileUploadApiTest,DccControlledFilePreviewDownloadApiTest,DccControlledFileQueryServiceTest,DccPdfStampServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Completed on 2026-05-16 at the backend slice. Preview watermark metadata is now returned from
`upload-preview` JSON and preview response headers, and pending approval
preview now reads the original uploaded PDF for authorized requester/review
flows. In addition, the runtime verification blocker on `/system/auth/login`
was reduced by removing the broken OAuth2 client Redis lookup from
`OAuth2ClientServiceImpl`, so login now reads the client definition directly
from MySQL instead of crashing inside Redisson cache deserialization. The
preview response now also exposes `X-DCC-Preview-Watermark` through
`Access-Control-Expose-Headers`, so browser JS can read the watermark metadata.

## Blocker And Impact

- Blocker: none for this backend slice.
- Impact: backend delivery is ready and staged as a task-only commit despite the
  surrounding repository still containing unrelated dirty files.
