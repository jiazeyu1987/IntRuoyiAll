# 20260524 prod frontend backend target fix execution log

## BDD

- BDD: 正式前端不得请求测试后端 -> Given 正式前端发布产物 / When reviewer 检查入口 JS / Then 产物不得包含 `172.30.30.58:48081`。
- BDD: 正式目标构建可生成正确产物 -> Given 使用正式 backend 目标构建前端 / When 构建完成 / Then JS 产物必须包含 `172.30.30.57:48081` 且不包含 `172.30.30.58:48081`。
- BDD: 正式发布需要显式确认 -> Given 发布脚本要求输入 `PROD` / When 用户未明确确认 / Then 不执行正式服发布，发布门禁保持 `BLOCKED`。
- BDD: 测试提升正式时重建目标前端 -> Given promotion 脚本从测试服读取已验证后端镜像和数据 / When 准备上传正式服镜像包 / Then 必须使用正式 backend 地址重建前端镜像，并上传重建后的目标镜像包。

## TDD Evidence

- RED: `Invoke-WebRequest http://172.30.30.57:8081/` + fetch `assets/index-b7bUP0rr.js` -> FAIL, 当前正式前端产物 backendTargets=`172.30.30.58:48081`，正式前端仍指向测试服 backend。
- RED: `pnpm exec vite build --mode test` with `VITE_BASE_URL=http://172.30.30.57:48081` -> FAIL, 当前 `node_modules/.bin` 缺少 `vite.cmd`，`pnpm exec` 无法找到 `vite`；改用同一已安装包入口继续验证。
- GREEN: `node node_modules/vite/bin/vite.js build --mode test` with `VITE_BASE_URL=http://172.30.30.57:48081`, `VITE_BASE_PATH=/`, `VITE_OUT_DIR=dist-intruoyi-test` -> PASS, 构建成功。
- GREEN: `rg -o "172\.30\.30\.(57|58):48081" yudao-ui-admin-vue3\dist-intruoyi-test\assets -g "*.js"` -> PASS, backendTargets=`172.30.30.57:48081`，未发现 `172.30.30.58:48081`。
- RED: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -k promote -q` -> FAIL, promotion 脚本缺少 `pnpm` 要求、正式目标前端重建、目标镜像包上传断言。
- GREEN: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -k promote -q` -> PASS，3 passed。
- RED: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -k rebuilds -q` -> FAIL，测试服源端 `docker save` 仍包含 `intruoyi-frontend:$imageTag`。
- GREEN: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -k rebuilds -q` -> PASS，源端导出已改为 backend-only。
- GREEN: `python -X utf8 -m pytest ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，19 passed。
- GREEN: `[System.Management.Automation.Language.Parser]::ParseFile(...)` on `promote-int-ruoyi-test-to-prod.ps1` -> PASS，Parse OK。

## 过程记录

- 任务开始：2026-05-24。
- 只读定位：
  - `yudao-ui-admin-vue3/vite.config.ts` 在构建期把 `import.meta.env.VITE_BASE_URL` 写入产物。
  - `yudao-ui-admin-vue3/src/config/axios/config.ts` 使用 `VITE_BASE_URL + VITE_API_URL` 作为 API base URL。
  - `ruoyi-vue-pro/script/deploy/publish-int-ruoyi-to-test.ps1` 构建前端时覆盖 `VITE_BASE_URL=http://${ServerHost}:$BackendPort`。
  - `ruoyi-vue-pro/script/deploy/promote-int-ruoyi-test-to-prod.ps1` 复用测试服 `intruoyi-frontend:$imageTag` 镜像；该镜像中的 backend 地址已经在测试构建期写死为 `172.30.30.58:48081`。
- 结论：
  - 改正式服 `.env` 或 compose 无法改变已构建 JS。
  - 必须重建并发布正式前端，使构建期 `VITE_BASE_URL=http://172.30.30.57:48081`。
  - 正式发布脚本要求显式 `PROD` 确认；未获得确认前不执行正式发布。
- 子 agent review：
  - 建议补强 promotion 脚本断言，覆盖 `pnpm` 前置条件、`VITE_BASE_URL` 目标、`VITE_OUT_DIR`、本地 `docker load/build/save`、上传 `$targetImageTarLocal`。
  - 建议测试服源端不再导出 `intruoyi-frontend:$imageTag`，避免测试前端镜像进入正式提升链路；已采纳并验证。
- 本次代码结论：
  - `promote-int-ruoyi-test-to-prod.ps1` 现在从测试服只导出已验证 backend 镜像。
  - promotion 本地按 `http://${TargetServerHost}:$TargetBackendPort` 构建前端静态产物并重建 `intruoyi-frontend:$imageTag`。
  - 最终上传正式服的是 `$targetImageTarLocal`，其中包含测试验证后的 backend 镜像和正式目标重建的 frontend 镜像。
