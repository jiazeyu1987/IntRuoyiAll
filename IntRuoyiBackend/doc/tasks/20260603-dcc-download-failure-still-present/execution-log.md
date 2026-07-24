# 执行日志：复查 DCC 下载失败仍存在

BDD: 用户实际 DCC 下载路径不再失败 -> Given 用户在 DCC 受控浏览或详情页看到有下载权限的现行文件 / When 点击“下载”并确认 / Then 浏览器必须下载后端返回的文件，页面不得出现“下载失败，请查看错误提示后重试”。

BDD: 不同文件名下载契约一致 -> Given 可下载文件的服务端文件名可能包含中文、空格或特殊字符 / When 后端返回下载响应 / Then 前端必须能读取并解析合法的服务端文件名，不能因文件名格式差异误判缺失。

## 证据

- M1: Completed. 上一任务 `20260602-dcc-download-failure` 已提交并标记 completed；本任务重新复查用户反馈“问题还在”。
- M2: Completed. 复跑上一轮测试租户回归：
- GREEN: `node doc/tasks/20260602-dcc-download-failure/verify-download-response-headers.mjs` -> PASS。
- GREEN: Playwright 测试租户 `aoteman` 从 `DCC文控中心 -> DCC受控浏览 -> 下载` -> PASS。
- GREEN: Playwright 测试租户详情页 `下载受控文件` -> PASS。
- M3: Completed. 新增中文文件名 RED 用例，目标文件名 `PD可编辑.pdf.dcc`。
- RED: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest#downloadControlledFile_encodesLocalizedFileNameForBrowserReadableDisposition -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, expected reason: `Content-Disposition` 不包含 UTF-8 `filename*=`，浏览器对中文/特殊字符文件名的稳定兼容性不足。
- ROOT CAUSE: `DccControlledFileController.downloadControlledFile` 使用 `ContentDisposition.attachment().filename(binary.fileName())`，未指定 `StandardCharsets.UTF_8`；同文件内其他下载接口已使用 UTF-8 写法。
- M4: Completed. 下载控制器已改为 `ContentDisposition.attachment().filename(binary.fileName(), StandardCharsets.UTF_8)`。
- GREEN: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest#downloadControlledFile_encodesLocalizedFileNameForBrowserReadableDisposition -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- GREEN: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 8 tests。
- GREEN: API 中文文件名下载 -> PASS, `tenantId=1`, `controlledFileId=2054545668044046933`, `Content-Disposition` 同时包含 RFC 2047 `filename="=?UTF-8?...?="` 与 RFC 5987 `filename*=UTF-8''...`。
- GREEN: Playwright 最终前端验证 -> PASS, `芋道源码/admin` 打开 `http://localhost:8081/dcc/controlled-file/detail/2054545668044046933`，点击 `下载受控文件`，建议文件名 `INT∕GL∕4.2.4-04（E∕0）技术文件编号管理制度.pdf.dcc`，下载字节 `207344`，失败 toast 数量 `0`。
