# 任务：禁用后台本地页自动翻译注入

## 任务目标

- 处理 `http://localhost:8081` 控制台出现的 `translate-pa.googleapis.com/v1/translateHtml` CORS / 502 噪声。
- 不吞掉异常、不添加 fallback、不修改业务运行时。
- 在后台前端入口 HTML 明确声明禁止自动翻译，避免 Google 翻译脚本对本地页面发起跨域翻译请求。

## BDD 场景

- BDD: 后台本地页声明禁止自动翻译 -> Given 用户打开 `http://localhost:8081` 后台前端 / When 浏览器翻译功能或翻译扩展检查文档 / Then 入口文档必须声明 `translate="no"`、`notranslate` 与 Google `notranslate` meta，避免第三方翻译脚本从 localhost 发起 `translate-pa.googleapis.com/v1/translateHtml` 请求。

## 里程碑

- [x] M1：确认 8081 归属并创建任务记录。
- [x] M2：补 RED 静态回归测试。
- [x] M3：最小修改后台入口 HTML。
- [x] M4：执行 GREEN 与本地 8081 readback 验证。
- [x] M5：记录 bug evidence、收尾预览并提交本任务改动。

## 预期验证

- RED：`node tests/index-translation-opt-out.test.mjs` 失败，当前入口 HTML 缺少翻译禁用声明。
- GREEN：`node tests/index-translation-opt-out.test.mjs` 通过。
- READBACK：`Invoke-WebRequest http://127.0.0.1:8081/` 返回的 HTML 包含翻译禁用声明。

## Current Status

completed

## 当前进展

- M1：已确认 8081 的真实源码归属为 `yudao-ui-admin-vue3`。
- M2：已新增入口 HTML 翻译禁用声明的静态回归测试，并记录 RED 失败。
- M3：已在 `index.html` 添加文档级翻译禁用声明。
- M4：静态测试、本地 8081 readback 与 Playwright 页面验证均通过。
- M5：bug evidence 校验通过，收尾预览与 apply 通过，无临时产物需要删除。

## 验证结果

- RED: `node tests/index-translation-opt-out.test.mjs` -> FAIL，入口 HTML 缺少翻译禁用声明。
- GREEN: `node tests/index-translation-opt-out.test.mjs` -> PASS。
- GREEN: `node --check tests/index-translation-opt-out.test.mjs` -> PASS。
- GREEN: `Invoke-WebRequest http://127.0.0.1:8081/` -> PASS，readback 包含翻译禁用声明。
- GREEN: Playwright browser load `http://127.0.0.1:8081/` -> PASS，DOM 声明正确且普通加载未发起 `translate-pa.googleapis.com` 请求。
- BUG-EVIDENCE: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260529-admin-translate-cors-noise/bug-regression-evidence.md` -> PASS。
- CLOSEOUT: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260529-admin-translate-cors-noise --mode preview` -> PASS，delete=<none>，blocked=<none>。
- CLOSEOUT: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260529-admin-translate-cors-noise --mode apply` -> PASS，deleted_paths=<none>。

## Cleanup Keep

- `doc/tasks/20260529-admin-translate-cors-noise/bug-regression-evidence.md`
