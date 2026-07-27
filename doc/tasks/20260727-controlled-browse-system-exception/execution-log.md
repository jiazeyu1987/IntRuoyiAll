# Execution Log

## 2026-07-27

- User intent: 分析测试服务器上 `wangsiyu` 账号进入“文控中心 > 受控浏览”时文件详情页显示“系统异常”的原因；用户提供了账号和截图，密码不写入记录。
- Rules loaded: `docs/server-access.md`, `docs/login-access.md`, `docs/e2e-rules.md`, `docs/task-closeout-rules.md`, `bug-regression-fix-loop`, `playwright`.
- Existing worktree state: workspace already had unrelated modified and untracked files before this task; this task will avoid changing those files.
- BDD: 测试服受控浏览文件详情异常定位 -> Given 测试服登录用户 `wangsiyu` 可进入受控浏览, When 打开截图中的受控文件详情, Then 页面不应只显示泛化“系统异常”，诊断应定位到真实失败接口或后端错误。
- GREEN: experience-preflight -> PASS, applicable gates recorded in task.md.
- RED: Playwright login using `测试租户/wangsiyu` -> FAIL, `/system/auth/login` returned `code=1002000000` and `msg=登录失败，账号密码不正确`; password redacted.
- GREEN: read-only user tenant check -> PASS, remote DB shows `wangsiyu` belongs to tenant `1 / 芋道源码`, user id `910250`, status enabled, not deleted.
- GREEN: Playwright reproduce via viewer path -> PASS, opened `http://172.30.30.58:8081/dcc/controlled-file/detail/2054545668044071537?viewer=1&from=browser` as `芋道源码/wangsiyu` and reproduced visible `系统异常`.
- GREEN: frontend failure capture -> PASS, `GET /admin-api/dcc/controlled-files/2054545668044071537/preview-metadata` returned application `code=500`, `msg=系统异常`.
- GREEN: backend log root-cause capture -> PASS, `docker logs --since 5m intruoyi-backend` shows `IllegalArgumentException: fileNumber is required` from `DccControlledPreviewAccessService.requireNotBlank`.
- GREEN: database data check -> PASS, target file `2054545668044071537 / 血液瓶瓶体清洗验证.pdf` has blank `file_number`; tenant 1 has `15995` active/superseded controlled files with blank `file_number`.
- Root cause: preview metadata generation passes `file.getFileNumber()` into `DccPreviewAccessRequest`, but `DccControlledPreviewAccessService.requireRequest` requires nonblank `fileNumber`. This conflicts with existing metadata validation intent that file number is optional for some controlled files.
- No code fix performed in this task; user requested cause analysis. Current workspace already has unrelated ahead commits and parallel dirty changes, so no commit/push was performed for this analysis record.

## 2026-07-27 Fix Extension

- User intent: 让 `preview-metadata` 不再强制要求 `fileNumber` 非空。
- Rules loaded: `bug-regression-fix-loop`, `backend-api-delivery`, `docs/backend-development.md`, `docs/task-closeout-rules.md`, `docs/powershell-memory.md`, `docs/powershell-encoding.md`.
- Existing worktree state: branch `int_main` is already ahead of `origin/int_main` and has unrelated modified/untracked files; this task will only touch the DCC preview/audit code, target regression tests, and this task directory.
- BDD: 空文件编号受控文件可生成预览元数据 -> Given 受控文件 `fileNumber` 为空且用户有预览权限, When 后端生成 `preview-metadata` 的 viewer token、水印追踪和访问日志, Then 不抛出 `fileNumber is required`，并保留访问审计与水印追踪。
- RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledPreviewAccessServiceTest#prepareAccess_allowsMissingFileNumberForPreviewMetadata" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected `IllegalArgumentException: fileNumber is required` from `DccControlledPreviewAccessService.requireRequest`.
