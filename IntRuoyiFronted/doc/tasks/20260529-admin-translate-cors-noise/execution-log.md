# 执行日志：禁用后台本地页自动翻译注入

BDD: 后台本地页声明禁止自动翻译 -> Given 用户打开 `http://localhost:8081` 后台前端 / When 浏览器翻译功能或翻译扩展检查文档 / Then 入口文档必须声明 `translate="no"`、`notranslate` 与 Google `notranslate` meta，避免第三方翻译脚本从 localhost 发起 `translate-pa.googleapis.com/v1/translateHtml` 请求。

- 2026-05-29 M1：已确认 `http://127.0.0.1:8081/` 返回的是 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\index.html` 对应的后台前端入口，而不是 `D:\ProjectPackage\Website` 展厅前端。
- 2026-05-29 RED: `node tests/index-translation-opt-out.test.mjs` -> FAIL，`index.html` 当前只有 `<html lang="en">`，缺少 `translate="no"`、`class="notranslate"` 与 `<meta name="google" content="notranslate" />`。
- 2026-05-29 GREEN: `node tests/index-translation-opt-out.test.mjs` -> PASS，入口 HTML 已声明禁止自动翻译。
- 2026-05-29 GREEN: `node --check tests/index-translation-opt-out.test.mjs` -> PASS。
- 2026-05-29 GREEN: `Invoke-WebRequest http://127.0.0.1:8081/` -> PASS，当前 8081 readback HTML 包含 `<html lang="en" translate="no" class="notranslate">` 与 Google `notranslate` meta。
- 2026-05-29 GREEN: Playwright browser load `http://127.0.0.1:8081/` -> PASS，DOM 暴露翻译禁用声明，普通页面加载未发起 `translate-pa.googleapis.com` 请求。
- 2026-05-29 BUG-EVIDENCE: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260529-admin-translate-cors-noise/bug-regression-evidence.md` -> PASS。
- 2026-05-29 CLOSEOUT-PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260529-admin-translate-cors-noise --mode preview` -> PASS，keep task.md、execution-log.md、bug-regression-evidence.md；delete=<none>；blocked=<none>。
- 2026-05-29 CLOSEOUT-APPLY: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260529-admin-translate-cors-noise --mode apply` -> PASS，deleted_paths=<none>。
