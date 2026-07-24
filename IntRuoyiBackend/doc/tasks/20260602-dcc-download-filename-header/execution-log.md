# 执行日志：修复 DCC 当前受控版本下载文件名头不可读

BDD: DCC 下载响应跨域暴露文件名 -> Given 用户有权限下载当前受控版本 / When 后端返回 DCC 下载响应 / Then 响应必须包含 `Content-Disposition` 文件名，并在 `Access-Control-Expose-Headers` 中暴露该响应头，前端才能读取文件名完成下载。

BDD: DCC 下载仍保留证据头契约 -> Given 用户发起带 `downloadRequestId` 的下载 / When 后端生成下载产物 / Then 响应继续暴露访问事件、下载请求 ID、加密策略、产物 ID 和哈希证据头，不得丢失审计契约。

- M1: Completed. 上一个后端任务 `20260602-dcc-other-category-local-apply` 已标记 `completed`；本任务只处理 DCC 下载响应头契约。
- M2: Completed. 已新增控制器回归断言，要求下载响应跨域暴露 `Content-Disposition`。
- RED: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest#downloadControlledFile_delegatesDownloadRequestIdToQueryServiceAndExposesEvidenceHeaders -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, expected reason: `Access-Control-Expose-Headers` 未包含 `Content-Disposition`，浏览器跨域前端无法读取后端文件名。
- M3: Completed. 后端 DCC 下载响应的 `Access-Control-Expose-Headers` 已补入 `Content-Disposition`，未改变下载文件名生成、产物、审计或加密证据头。
- GREEN: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest#downloadControlledFile_delegatesDownloadRequestIdToQueryServiceAndExposesEvidenceHeaders -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- M4: Completed. 同一控制器下载/预览契约回归通过。
- GREEN: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 7 tests。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260602-dcc-download-filename-header\bug-regression-evidence.md` -> PASS。
- M5: Completed. Bug 证据已记录并通过校验，收尾清理预览未发现待删临时产物或阻塞项。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-dcc-download-filename-header --mode preview` -> PASS, delete `<none>`, blocked `<none>`, warnings `<none>`。
