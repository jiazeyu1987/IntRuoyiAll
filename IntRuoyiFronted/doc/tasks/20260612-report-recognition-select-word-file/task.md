# 20260612 六路识别选择 Word 文件

## 任务目标

让 `报表管理 -> 报表设计器 -> 六路识别` 页签中的 A-F 路线按钮在点击后先打开本机 `.doc` Word 文件选择器，再将用户选择的 Word 文件按对应路线提交给后端解析并刷新报表列表。

## 里程碑

1. M1 审计：确认现有六路识别页面、前端 API、后端上传解析接口和上一个前端任务状态。
2. M2 RED：新增前端静态契约测试，要求路线按钮通过文件输入触发上传解析。
3. M3 GREEN：实现隐藏 Word 文件输入、路线选择状态、上传 API 调用、成功/失败提示和列表刷新。
4. M4 REGRESSION：运行目标前端测试和类型检查。
5. M5 E2E/收尾：用真实前端路径验证或记录阻塞，运行收尾清理预览。

## 预期验证

- `node scripts/report-management-six-route-page.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- Playwright 真实路径：本机 `http://localhost:8081`、测试租户 `aoteman`，进入 `报表管理 -> 报表设计器 -> 六路识别` 后点击路线按钮应弹出 Word 文件选择器。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；用户取消选择不触发解析，文件或接口失败直接展示错误，不切回固定样本。
- `是否从根因和长期维护角度解决`：是；按钮语义从固定样本解析改为正式上传解析 API，前端不模拟解析结果。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：COMPLETED。
- 已完成：确认前端上一个任务 `20260612-process-use-route-tabs` 已完成；新增上传路线解析 API 调用、隐藏 `.doc` 文件输入、按钮触发文件选择、文件校验、上传成功/失败提示和列表刷新；静态测试、类型检查和真实文件选择器验证通过。

## Current Status

completed

## Cleanup Keep

- doc/tasks/20260612-report-recognition-select-word-file/frontend-feature-evidence.md
