# Verification Report

## Scope

- 修复范围：DCC 上传预览响应、前端上传响应解析、上传页预览组件透传和 OnlyOffice 预览挂载。
- 真实路径：`http://127.0.0.1:8081/dcc/controlled-file/upload`，租户/用户标签 `芋道源码/admin`。
- 文件分类：`技术文档 / 设计和开发输入阶段 / 专利检索与分析报告（如适用）`，自动文件类别 `专利检索与分析报告`。

## Requirement Matrix

- 上传 Office 文件必须返回 OnlyOffice 服务地址和签名文档地址：后端 JUnit、前端静态合同、真实 E2E 均覆盖。
- 上传响应不得暴露原始 `fileId`：后端 JUnit 和真实 E2E 脱敏响应断言覆盖。
- 页面不得提示 `OnlyOffice 预览地址未准备好`：真实 Playwright 页面路径覆盖。
- 临时上传文件必须清理：真实 E2E 点击上传列表移除文件，`session-cleanup` 返回 `CLEANED`。

## Runtime Evidence

- 后端运行 Jar：`E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260804-dcc-upload-onlyoffice-document-url.jar`。
- Jar SHA256：`2A4270000B3B6C7F905BA7BEAC5F479241121B0801E35B65840711E703EBBD69`。
- Jar 内嵌模块检查：`BOOT-INF/lib/yudao-module-dcc-2026.04-SNAPSHOT.jar` 包含 `DccControlledFileUploadRespVO.class`、`DccControlledFileUploadServiceImpl.class`，`javap` 确认 `onlyofficeDocumentUrl` 字段存在。
- 本机运行态：后端 `48081` PID `20048` health `UP`；前端 `8081` PID `28264` HTTP `200`。
- 依赖前置：MinIO ready HTTP `200`；OnlyOffice API HTTP `200`；OnlyOffice 容器访问 `http://host.docker.internal:48081/actuator/health` 返回 `200`。

## Verification Commands

- `node tests/e2e/dcc-upload-onlyoffice-document-url-static.spec.js` -> PASS。
- `node --check tests/e2e/dcc-upload-onlyoffice-document-url-real.e2e.js` -> PASS。
- `pnpm e2e:dcc:upload-onlyoffice-document-url:real` -> PASS。
- `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileUploadApiTest#uploadResponseContract_exposesUploadTicketAndSignedOnlyOfficeDocumentUrlWithoutFileId,DccControlledFileUploadApiTest#uploadPreviewFile_sourceXlsx_withOnlyOfficeConfigReturnsSignedDocumentUrl" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileUploadApiTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 20 tests passed。
- 相邻前端回归：`node tests/e2e/dcc-preview-unavailable-reason-static.spec.js`、`node tests/e2e/dcc-common-file-preview-source.spec.js`、`node tests/e2e/dcc-upload-category-permission-static.spec.js` -> PASS。

## E2E Result

- 证据文件：`E:\IntRuoyi\output\playwright\20260803-dcc-upload-onlyoffice-document-url\dcc-upload-onlyoffice-document-url-real-evidence.json`。
- 截图：`E:\IntRuoyi\output\playwright\20260803-dcc-upload-onlyoffice-document-url\dcc-upload-onlyoffice-document-url-real.png`。
- 上传预览响应：`code=0`，`previewKind=OFFICE`，`onlyofficeBaseUrl` 存在，`onlyofficeDocumentUrl` 指向 `/admin-api/dcc/controlled-files/upload-preview/{id}/onlyoffice-file` 且包含签名 token。
- 安全边界：响应不包含原始 `fileId`；正式 DCC 受控文件提交接口未触发；仅触发 `upload-preview` 和临时文件 `session-cleanup`。
- 页面结果：OnlyOffice 表格预览可见，未出现 `OnlyOffice 预览地址未准备好`；`targetNetworkFailures=[]`、`consoleErrors=[]`、`pageErrors=[]`。
- 清理结果：`session-cleanup` 返回 `code=0`、`cleanedCount=1`、`cleanupStatus=CLEANED`。

## Cleanup Evidence

- `task_closeout.py --task-id 20260803-dcc-upload-onlyoffice-document-url --mode preview` -> ready，delete 仅包含任务临时 `.xlsx` 和 `output/runtime/int_main/jar-check-20260804-dcc-upload-onlyoffice-document-url/`。
- `task_closeout.py --task-id 20260803-dcc-upload-onlyoffice-document-url --mode apply` -> applied，已删除上述两个任务自有临时产物。
- `git -C E:\IntRuoyi worktree remove --force D:\IntRuoyiWorktree\dcc-upload-onlyoffice-runtime-20260804` -> PASS，任务隔离构建 worktree 已移除且不再登记。

## Experience Consolidation

- `docs/local-runtime.md` 新增 `2026-08-04 DCC 上传预览 OnlyOffice 文档地址门禁`。
- `docs/experience-index.md` 新增 `OnlyOffice 预览地址未准备好`、`onlyofficeBaseUrl onlyofficeDocumentUrl`、`RESOURCE_UPLOAD_PREVIEW` 等关键词路由。

## Blockers

- 当前任务代码与文档已验证通过。
- Git 收尾仍受共享分支状态约束：`int_main...origin/int_main [ahead 3, behind 2]`，且主工作区存在无关脏改动；不得宽泛暂存、回滚或强行推送。
