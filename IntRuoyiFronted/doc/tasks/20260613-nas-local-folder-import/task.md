# 20260613 NAS 本地文件夹导入 DCC 前端实施

## 任务目标

在 `NAS 管理` 页签的 `选择` 与 `导出` 按钮之间新增 `导入文件夹`，允许用户选择本地文件夹后将文件和相对目录结构导入 DCC。空文件夹不导入；本地导入不保存本机绝对路径、不要求 NAS 路径权限、不展示 NAS 权限恢复面板；不在前端设置 300MB 导入大小限制。

## 前置任务检查

- 最近前端任务目录 `20260612-edhr-attachment-prepare-upload-api` 无 `task.md`，仅包含证据文件，视为无可检查任务文档，未修改该无关目录。
- 最近可检查前端任务文档：`20260612-report-recognition-select-word-file/task.md`，状态 `COMPLETED`。
- 本任务限定在 NAS 管理页、DCC workflow API 类型和本任务测试/证据文件。

## 里程碑

1. M1 文档与审计：创建任务文档，确认现有 NAS 转移弹窗、DCC API 封装和验证命令。
2. M2 RED：新增前端静态契约测试，要求按钮位置、目录选择器、导入 API、`sourceType` 和本地来源隐藏权限恢复面板。
3. M3 GREEN：实现本地目录选择、相对路径校验、multipart 提交、导入弹窗复用和状态轮询。
4. M4 REGRESSION：运行目标静态测试和 TypeScript 检查。
5. M5 E2E/收尾：用真实前端路径验证或记录阻塞，运行收尾清理预览。

## 预期验证

- `node scripts/system-nas-management.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- Playwright 真实路径：本机 `http://localhost:8081/system/nas`，测试租户 `aoteman`，选择本任务小型本地目录并确认导入。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；浏览器取消选择不创建任务，目录选择或上传失败直接提示，不切换到 NAS 转移或 mock 成功。
- `是否从根因和长期维护角度解决`：是；新增正式本地目录导入 API 契约，前端只上传文件和相对路径，由后端统一创建 DCC 目录/文件。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：BLOCKED_E2E。
- 已完成：M1-M4。已新增 `导入文件夹` 按钮、隐藏目录选择器、相对路径校验、multipart API、复用导入弹窗/轮询/结果展示，以及 `sourceType=LOCAL_FOLDER` 时隐藏 NAS 权限恢复面板；已按用户要求去除前端 300MB 导入大小限制。
- 剩余阻塞：M5 真实 Playwright E2E 尚未完成。后端 48081 已恢复可访问，Playwright 已登录测试租户并看到 `导入文件夹` 按钮；但测试租户缺少启用的 DCC 模板类别 `其他`，小型目录探针在业务前置处 fail fast。用户指定目录 `E:\Downloads\1. QMS documents` 包含 962 个文件、约 843.72MB，Playwright filechooser 未能把该目录内容注入浏览器 input（`input.files.length=0`），未进入后端导入接口。

## Cleanup Keep

- doc/tasks/20260613-nas-local-folder-import/frontend-feature-evidence.md
