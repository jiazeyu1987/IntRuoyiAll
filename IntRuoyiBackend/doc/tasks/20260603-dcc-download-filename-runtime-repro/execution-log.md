# 执行日志：复查 DCC 下载文件名运行态仍报错

BDD: 真实按钮链路可读取下载文件名 -> Given 本机用户打开 DCC 受控文件详情页 / When 点击下载当前受控版本或当前受控副本 / Then 下载响应必须让浏览器可读取 `Content-Disposition` 文件名，前端不再提示 `DCC download response missing required filename`。

BDD: 运行态未加载最新后端时明确暴露 -> Given 源码已修复但本机后端仍运行旧代码 / When 抓取真实 `/dcc/controlled-files/{id}/download` 响应头 / Then 证据必须显示运行态未暴露 `Content-Disposition`，并通过重启后端加载最新构建解决，不得修改前端绕过。

- M1: Completed. 上一个后端任务 `20260602-dcc-download-filename-header` 已标记 `completed`，当前任务转入运行态复现。
- M2: Completed. 直接 API 下载响应头检查通过，当前运行态返回 `Content-Disposition`，且 `Access-Control-Expose-Headers` 包含 `Content-Disposition`。
- GREEN: direct API `/dcc/controlled-files/2054545668044046252/download` with tenant `122` -> PASS, status `200`, `Content-Disposition=attachment; filename="codex-e2e-stamped.pdf.dcc"`。
- M3: Completed. 无需新增代码修复；当前后端 jar 已加载上次响应头修复，且前端无 service worker/PWA 缓存注册。
- M4: Completed. Playwright 新浏览器真实点击验证通过。
- GREEN: Playwright `http://localhost:8081/dcc/controlled-file/detail/2054545668044046252` click `下载受控文件` -> PASS, response exposes `content-disposition`, suggested filename `codex-e2e-stamped.pdf.dcc`, no error toast。
- GREEN: Playwright `http://localhost:8081/dcc/controlled-file/detail/2054545668044046252?viewer=1&from=detail` click `下载当前受控副本` -> PASS, response exposes `content-disposition`, suggested filename `codex-e2e-stamped.pdf.dcc`, no error toast。
- GREEN: Playwright `http://127.0.0.1:8081/dcc/controlled-file/detail/2054545668044046252` click `下载受控文件` -> PASS, response exposes `content-disposition`, `access-control-allow-origin=http://127.0.0.1:8081`, suggested filename `codex-e2e-stamped.pdf.dcc`, no error toast。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260603-dcc-download-filename-runtime-repro\bug-regression-evidence.md` -> PASS。
- M5: Completed. 运行态复查证据已记录并通过校验，收尾清理预览无待删项、无阻塞。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dcc-download-filename-runtime-repro --mode preview` -> PASS, delete `<none>`, blocked `<none>`, warnings `<none>`。
