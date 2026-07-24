# 任务：复查 DCC 下载文件名运行态仍报错

## 任务目标

复查点击 `下载当前受控版本/下载当前受控副本` 后仍提示 `DCC download response missing required filename` 的运行态问题，抓取真实按钮链路的下载响应头，确认是否为本机后端未重启到最新提交、响应头被 CORS 覆盖，或按钮走了另一个下载接口。

## 上一任务检查

- 上一个后端任务 `20260602-dcc-download-filename-header` 已标记 `completed`，并提交 `e327ae6248`。
- 当前任务只处理本机运行态复现与必要修复；未经用户明确授权，不操作测试服或正式服。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。前端读不到后端文件名仍应失败暴露。
- `是否从根因和长期维护角度解决`：是。先用真实按钮链路和响应头定位，再决定重启生效或补正式契约。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: 真实按钮链路可读取下载文件名 -> Given 本机用户打开 DCC 受控文件详情页 / When 点击下载当前受控版本或当前受控副本 / Then 下载响应必须让浏览器可读取 `Content-Disposition` 文件名，前端不再提示 `DCC download response missing required filename`。

BDD: 运行态未加载最新后端时明确暴露 -> Given 源码已修复但本机后端仍运行旧代码 / When 抓取真实 `/dcc/controlled-files/{id}/download` 响应头 / Then 证据必须显示运行态未暴露 `Content-Disposition`，并通过重启后端加载最新构建解决，不得修改前端绕过。

## 里程碑

- [x] M1：建立任务文档并确认上一后端任务已完成。
- [x] M2：抓取当前运行态下载链路响应头，复现用户仍见错误的原因。
- [x] M3：按根因执行最小修复或重启生效。
- [x] M4：用真实按钮链路或等价浏览器响应头验证。
- [x] M5：记录证据、收尾清理预览并提交本任务改动。

## 预期验证

- 浏览器或 Playwright 真实链路点击 DCC 下载按钮，捕获 `/dcc/controlled-files/*/download` 响应头。
- 若只需重启：重启本机后端后重复抓取，确认 `Access-Control-Expose-Headers` 包含 `Content-Disposition`。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dcc-download-filename-runtime-repro --mode preview`

## 当前状态

completed

当前本机运行态与新浏览器真实点击均已通过：详情页 `下载受控文件`、预览模式 `下载当前受控副本`、`localhost:8081` 与 `127.0.0.1:8081` 两个入口都能读取 `content-disposition` 并触发下载，未再出现错误 toast。本轮未修改生产代码。

## 最终验证结果

- 直接 API `/dcc/controlled-files/2054545668044046252/download` -> PASS，状态 `200`，响应包含 `Content-Disposition` 与已暴露的 `Access-Control-Expose-Headers`。
- Playwright `http://localhost:8081/dcc/controlled-file/detail/2054545668044046252` 点击 `下载受控文件` -> PASS，下载文件名 `codex-e2e-stamped.pdf.dcc`，无错误 toast。
- Playwright `http://localhost:8081/dcc/controlled-file/detail/2054545668044046252?viewer=1&from=detail` 点击 `下载当前受控副本` -> PASS，下载文件名 `codex-e2e-stamped.pdf.dcc`，无错误 toast。
- Playwright `http://127.0.0.1:8081/dcc/controlled-file/detail/2054545668044046252` 点击 `下载受控文件` -> PASS，`access-control-allow-origin=http://127.0.0.1:8081`，无错误 toast。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260603-dcc-download-filename-runtime-repro\bug-regression-evidence.md` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dcc-download-filename-runtime-repro --mode preview` -> PASS，delete `<none>`、blocked `<none>`、warnings `<none>`。

## Cleanup Keep

- `doc/tasks/20260603-dcc-download-filename-runtime-repro/bug-regression-evidence.md`
