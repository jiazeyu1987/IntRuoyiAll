# 20260627 发布脚本前端构建内存保护缺失

BDD: 发布脚本沿正式 test 构建入口执行 -> Given 维护控制台 build-release 需要在前端仓构建 test 静态资源 / When 发布脚本触发前端构建 / Then 必须沿用前端仓正式 build:test 入口与 8GB Node heap 保护，不得再直接以默认 heap 调用 vite CLI。

- 根因摘要：前端源码修复后，`pnpm build:test` 可在 `NODE_OPTIONS=--max-old-space-size=8192` 下成功；但 `publish-int-ruoyi.ps1` 的 `Invoke-FrontendViteBuild` 直接执行 `node node_modules\\vite\\bin\\vite.js build --mode test`，导致发布链路绕开正式构建入口并在默认 heap 下 OOM。
- 预期行为：发布脚本必须继续显式设置 `NODE_OPTIONS=--max-old-space-size=8192`，并通过正式前端构建入口执行 `test` 模式打包，避免维护控制台与手工回归路径分叉。

- RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\vite\bin\vite.js build --mode test` -> FAIL, default Node heap OOM during test build after template-root fix
- RED: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, 新增构建入口契约未发现 `Invoke-CheckedShell -Command 'pnpm build:test'`
- GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，`90 passed`
- GREEN: `pnpm build:test` -> PASS，`Build successful. Please see dist-test directory`
- 修复说明：`Invoke-FrontendViteBuild` 已改为在前端仓执行 `pnpm build:test`，从而复用正式 `cross-env NODE_OPTIONS=--max-old-space-size=8192 vite build --mode test` 入口；契约测试同时禁止回退到直接 `node vite.js build --mode test`。
