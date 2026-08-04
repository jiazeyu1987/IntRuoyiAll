# Execution Log

## User Intent

- 用户截图显示 DCC 文件上传页上传 `报工.xlsx` 后，在“提交前预览”区域出现 `OnlyOffice 预览地址未准备好`。
- 期望：文件类别等前置逻辑已自动处理后，Office 文件上传预览应自动准备 OnlyOffice 预览地址，不要求用户选择或填写。

## BDD

- BDD: 上传 Office 文件自动准备 OnlyOffice 预览地址 -> Given 本机 DCC 上传页和 OnlyOffice 配置可用，When 用户选择文件分类叶子节点并上传 `.xlsx` 文件，Then 上传预览响应包含 OnlyOffice 服务地址和签名文档地址，页面不显示 `OnlyOffice 预览地址未准备好`。
- BDD: 上传响应不暴露原始文件 ID -> Given 上传预览使用临时文件，When 后端返回 Office 预览元数据，Then 响应不包含可直接绑定的 `fileId` 字段，只包含带 token 的临时文件下载地址。

## Milestone Evidence

- in_progress: 2026-08-03 读取 bug-regression-fix-loop、quality-assurance-test-suite、前后端、E2E、登录、本机运行态、PowerShell 编码和 worktree 规则。
- in_progress: 2026-08-03 截图确认新错误文本为 `OnlyOffice 预览地址未准备好`，出现在上传预览卡片内。
- completed: 2026-08-04 后端 `DccControlledFileUploadRespVO` 增加 `onlyofficeDocumentUrl`，上传预览服务签发 `RESOURCE_UPLOAD_PREVIEW` token 并返回 `publicFileBaseUrl + /admin-api/dcc/controlled-files/upload-preview/{fileId}/onlyoffice-file?token=...`。
- completed: 2026-08-04 前端上传响应类型、parser、上传页和 `ProtectedPdfViewer` 均透传 `onlyofficeDocumentUrl`，继续禁止暴露原始 `fileId`。
- completed: 2026-08-04 隔离 worktree 构建后端 runtime jar，并替换本机 `48081` 旧 int_main 后端；新 PID `20048` health `UP`，Jar SHA256 `2A4270000B3B6C7F905BA7BEAC5F479241121B0801E35B65840711E703EBBD69`。
- completed: 2026-08-04 真实 Playwright 通过文件上传页选择 `技术文档 / 设计和开发输入阶段 / 专利检索与分析报告（如适用）`，自动文件类别为 `专利检索与分析报告`，上传 `.xlsx` 后显示 OnlyOffice 表格预览。

- completed: 2026-08-04 `task_closeout.py --task-id 20260803-dcc-upload-onlyoffice-document-url --mode preview/apply` 删除任务临时 `.xlsx` 和 jar-check 目录，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- completed: 2026-08-04 移除任务自有隔离构建 worktree `D:\IntRuoyiWorktree\dcc-upload-onlyoffice-runtime-20260804`；移除前仅包含本任务两处后端补丁且无活动进程。
## RED / GREEN / REGRESSION

- RED: `node tests/e2e/dcc-upload-onlyoffice-document-url-static.spec.js` -> FAIL, 上传解析器仍把 `onlyofficeDocumentUrl` 作为 forbidden raw file capability 字段拒绝。
- RED: `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileUploadApiTest#uploadResponseContract_exposesUploadTicketAndSignedOnlyOfficeDocumentUrlWithoutFileId,DccControlledFileUploadApiTest#uploadPreviewFile_sourceXlsx_withOnlyOfficeConfigReturnsSignedDocumentUrl" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `DccControlledFileUploadRespVO` 缺少 `onlyofficeDocumentUrl` bean property，xlsx 上传响应无法返回签名文档地址。
- GREEN: `node tests/e2e/dcc-upload-onlyoffice-document-url-static.spec.js` -> PASS。
- GREEN: `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileUploadApiTest#uploadResponseContract_exposesUploadTicketAndSignedOnlyOfficeDocumentUrlWithoutFileId,DccControlledFileUploadApiTest#uploadPreviewFile_sourceXlsx_withOnlyOfficeConfigReturnsSignedDocumentUrl" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS。
- GREEN: `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileUploadApiTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS, 20 tests passed。
- GREEN: `mvn.cmd -f D:\IntRuoyiWorktree\dcc-upload-onlyoffice-runtime-20260804\IntRuoyiBackend\pom.xml -pl yudao-server -am "-DskipTests" package` -> BUILD SUCCESS, `yudao-server-exec.jar` generated.
- GREEN: `node --check tests/e2e/dcc-upload-onlyoffice-document-url-real.e2e.js` -> PASS。
- GREEN: `pnpm e2e:dcc:upload-onlyoffice-document-url:real` -> PASS, evidence `E:\IntRuoyi\output\playwright\20260803-dcc-upload-onlyoffice-document-url\dcc-upload-onlyoffice-document-url-real-evidence.json`。
- REGRESSION: `node tests/e2e/dcc-preview-unavailable-reason-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/dcc-common-file-preview-source.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/dcc-upload-category-permission-static.spec.js` -> PASS。

## E2E Evidence

- Runtime preflight: frontend `http://127.0.0.1:8081/` HTTP `200`; backend `http://127.0.0.1:48081/actuator/health` `UP`; MinIO ready HTTP `200`; OnlyOffice API HTTP `200`; OnlyOffice container to backend health HTTP `200` via `host.docker.internal:48081`.
- Upload preview response: `code=0`, `previewKind=OFFICE`, `onlyofficeBaseUrl` present, signed `onlyofficeDocumentUrl` present, raw `fileId` absent.
- Page assertion: `提交前预览` visible with OnlyOffice spreadsheet preview; `OnlyOffice 预览地址未准备好` not visible; `targetNetworkFailures=[]`, `consoleErrors=[]`, `pageErrors=[]`.
- Cleanup assertion: clicked upload list remove control; `/admin-api/dcc/controlled-files/upload-temporary/session-cleanup` returned `code=0`, `cleanedCount=1`, `cleanupStatus=CLEANED`.

## Blockers

- Implementation and verification blockers: none.
- Closeout blocker: shared branch/worktree is dirty and `int_main...origin/int_main [ahead 3, behind 2]`; do not broad-stage or push until unrelated changes are separated.
