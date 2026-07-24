# 20260524 DCC 任意文件上传与常见文件预览 - 后端

## 任务目标

- DCC 受控文件上传接口支持任意单文件类型，不因扩展名或 MIME 类型不是 PDF 而拒绝。
- DCC 预览类型识别覆盖常见文件：PDF、图片、文本/代码、Office、音频、视频。
- Office 预览依赖 OnlyOffice；配置缺失时不得伪造预览地址，必须返回明确 `previewUnavailableReason`，但不能阻断文件上传。
- 其他未知类型归类为 `DOWNLOAD_ONLY`，保留下载，不静默伪装成可预览。

## 前序任务检查

- 后端前序 DCC 任务：
  - `doc/tasks/20260523-dcc-nas-transfer-large-folder-backend/task.md`
  - 状态：`Completed on 2026-05-23`
- 结论：前序任务已完成，不阻塞本任务。

## BDD 场景

- BDD: 任意二进制文件可上传 -> Given 用户选择 `.zip` 等未知类型单文件 / When 调用 DCC `upload-preview` / Then 后端保存文件并返回 `DOWNLOAD_ONLY`，不因类型未知拒绝。
- BDD: 常见媒体文件可在线预览 -> Given 用户上传或查看 `.mp4`、`.mp3` 等浏览器原生支持媒体文件 / When 查询预览元数据 / Then 返回 `VIDEO` 或 `AUDIO` 预览类型。
- BDD: Office 配置缺失不阻断上传 -> Given 用户上传 `.docx` 且 OnlyOffice 配置缺失 / When 调用 DCC `upload-preview` / Then 文件上传成功，返回 `OFFICE` 与明确的 `previewUnavailableReason`，不返回伪造 OnlyOffice 地址。

## 里程碑

- [x] M1：建立任务文档和 BDD/TDD 证据框架。
- [x] M2：补充后端失败测试，覆盖未知二进制、音频/视频和 Office 配置缺失。
- [x] M3：扩展后端预览类型识别与 Office 配置缺失响应。
- [x] M4：运行后端定向测试。
- [x] M5：运行 evidence 校验、closeout 预览并按整体验证结果决定是否提交。

## 预期验证

- `mvn -pl yudao-module-dcc -am '-Dtest=DccControlledFileUploadApiTest,DccControlledFileQueryServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260524-dcc-any-file-common-preview\backend-api-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\worktrees\dcc-test\ruoyi-vue-pro --task-id 20260524-dcc-any-file-common-preview --mode preview --worktree-closeout off`

## 当前状态

- 状态：completed
- 当前阶段：后端定向验证、evidence 校验、closeout 预览、提交、rebase、快进合并和合并后验证均已完成。
- 收尾：按 closeout 清理规则已移除临时 evidence 文件，验证结果保留在 `execution-log.md`。
- Worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\dcc-test\ruoyi-vue-pro` 已删除。

## Current Status

completed
