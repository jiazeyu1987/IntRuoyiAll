# 任务：修复 DCC 当前受控版本下载文件名头不可读

## 任务目标

修复点击 `下载当前受控版本` 时前端提示 `DCC download response missing required filename` 的问题，确保 DCC 下载接口在跨域前端场景下暴露 `Content-Disposition`，让前端可以按后端返回文件名保存文件。

## 上一任务检查

- 上一个后端任务 `20260602-dcc-other-category-local-apply` 已标记 `completed`。
- 当前任务只修改本机后端代码与本任务记录，不接管当前后端仓库已有的其他未提交改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。下载响应缺少必要文件名或证据头时仍应由前端失败暴露。
- `是否从根因和长期维护角度解决`：是。根因是跨域响应未暴露 `Content-Disposition`，后端契约应显式暴露该头。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: DCC 下载响应跨域暴露文件名 -> Given 用户有权限下载当前受控版本 / When 后端返回 DCC 下载响应 / Then 响应必须包含 `Content-Disposition` 文件名，并在 `Access-Control-Expose-Headers` 中暴露该响应头，前端才能读取文件名完成下载。

BDD: DCC 下载仍保留证据头契约 -> Given 用户发起带 `downloadRequestId` 的下载 / When 后端生成下载产物 / Then 响应继续暴露访问事件、下载请求 ID、加密策略、产物 ID 和哈希证据头，不得丢失审计契约。

## 里程碑

- [x] M1：建立任务文档并确认上一后端任务已完成。
- [x] M2：补充 RED 回归测试，复现下载响应未暴露文件名头。
- [x] M3：最小修改后端下载响应暴露头契约。
- [x] M4：运行目标测试和必要回归验证。
- [x] M5：记录验证证据，执行收尾清理预览并提交本任务改动。

## 预期验证

- `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest#downloadControlledFile_delegatesDownloadRequestIdToQueryServiceAndExposesEvidenceHeaders -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-dcc-download-filename-header --mode preview`

## Cleanup Keep

- `doc/tasks/20260602-dcc-download-filename-header/bug-regression-evidence.md`

## 当前状态

completed

后端 DCC 下载接口已在 `Access-Control-Expose-Headers` 中暴露 `Content-Disposition`。前端跨域调用 `下载当前受控版本` 时可以读取后端文件名，不再因文件名头不可读触发 `DCC download response missing required filename`。

## 最终验证结果

- RED: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest#downloadControlledFile_delegatesDownloadRequestIdToQueryServiceAndExposesEvidenceHeaders -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，原因是 `Access-Control-Expose-Headers` 未包含 `Content-Disposition`。
- GREEN: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest#downloadControlledFile_delegatesDownloadRequestIdToQueryServiceAndExposesEvidenceHeaders -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- GREEN: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，7 tests。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260602-dcc-download-filename-header\bug-regression-evidence.md` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-dcc-download-filename-header --mode preview` -> PASS，delete `<none>`、blocked `<none>`、warnings `<none>`。
