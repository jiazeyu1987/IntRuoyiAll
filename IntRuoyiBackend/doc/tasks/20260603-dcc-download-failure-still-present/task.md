# 任务：复查 DCC 下载失败仍存在

## 任务目标

复查用户反馈“问题还在”的 DCC 下载失败，定位与上一轮已验证通过路径的差异，确保真实前端下载路径在用户实际场景下不再提示“下载失败，请查看错误提示后重试”。

## 上一任务检查

- 上一个后端任务 `20260602-dcc-download-failure` 已提交并标记 `completed`，提交 `9783d96dae`。
- 当前仓库存在其他未提交改动，本任务只修改与本次复查直接相关的任务文档、回归脚本或必要代码。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，先复现用户仍失败的真实入口和文件场景，再决定是否需要源码修复。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: 用户实际 DCC 下载路径不再失败 -> Given 用户在 DCC 受控浏览或详情页看到有下载权限的现行文件 / When 点击“下载”并确认 / Then 浏览器必须下载后端返回的文件，页面不得出现“下载失败，请查看错误提示后重试”。

BDD: 不同文件名下载契约一致 -> Given 可下载文件的服务端文件名可能包含中文、空格或特殊字符 / When 后端返回下载响应 / Then 前端必须能读取并解析合法的服务端文件名，不能因文件名格式差异误判缺失。

## 里程碑

- [x] M1：建立复查任务文档，确认上一任务已完成。
- [x] M2：复跑上一轮 API 与 Playwright 真实路径，确认当前运行状态。
- [x] M3：复现用户仍失败的差异场景，锁定 RED 证据。
- [x] M4：按 RED -> GREEN 最小修复或确认环境/路径漂移根因。
- [ ] M5：运行回归验证、更新证据并提交本任务改动。

## 预期验证

- 后端运行状态：48081 必须是当前源码构建，不得回到旧包。
- API 回归：DCC 下载响应必须包含并暴露 `Content-Disposition`。
- Playwright：从 `http://localhost:8081` 登录测试租户，走左侧菜单进入真实 DCC 下载路径。
- 如发现文件名解析差异，补充覆盖中文或特殊文件名的回归测试。

## 当前状态

in_progress

## 已完成工作

- 已确认上一任务 `20260602-dcc-download-failure` 为 completed。
- 已复跑上一轮测试租户 API 与前端下载路径，确认 ASCII 文件名下载正常，不是全局下载链路失效。
- 已锁定差异根因：DCC 下载控制器对 `Content-Disposition` 使用了不带 `StandardCharsets.UTF_8` 的 `filename(binary.fileName())`，而项目内其他中文下载接口使用 UTF-8 写法。中文或特殊字符文件名场景下，当前响应头缺少浏览器稳定可读的 UTF-8 `filename*`。
- 已新增 RED 控制器回归测试：中文文件名 `PD可编辑.pdf.dcc` 的下载响应必须包含 `filename*=`。
- 已将下载控制器改为 `ContentDisposition.attachment().filename(binary.fileName(), StandardCharsets.UTF_8)`，与既有 Word/导出接口保持一致。
- 已用 `芋道源码/admin` 对中文文件 `INT∕GL∕4.2.4-04（E∕0）技术文件编号管理制度.pdf` 真实前端下载验证通过。

## 最终验证结果

- RED: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest#downloadControlledFile_encodesLocalizedFileNameForBrowserReadableDisposition -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，下载响应头不包含 UTF-8 `filename*=`。
- GREEN: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest#downloadControlledFile_encodesLocalizedFileNameForBrowserReadableDisposition -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS。
- GREEN: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，8 tests。
- GREEN: API 真实中文文件名下载 -> PASS，`Content-Disposition` 包含 `filename*=` 与 UTF-8 编码文件名。
- GREEN: Playwright 真实前端下载（`芋道源码/admin`） -> PASS，详情页下载建议文件名 `INT∕GL∕4.2.4-04（E∕0）技术文件编号管理制度.pdf.dcc`，下载字节 `207344`，失败 toast 数量 `0`。

## 阻塞记录

- 无。
