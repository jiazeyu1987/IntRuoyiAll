# Execution Log

BDD: 测试服 backend 切换到当前最新代码 -> Given 本机 `ruoyi-vue-pro` 已包含 DCC viewer token 固定密钥修复且本地测试通过 / When 执行统一发布脚本向测试服进行 code-only 发布 / Then 测试服 backend 镜像标签、运行容器和健康检查应更新到本次发布版本。

SETUP: 测试服发布前只读状态 -> `show-int-ruoyi-test-status.bat` 显示当前 release package 为 `26-06-02_20-13-57`，`intruoyi-backend` 运行镜像为 `intruoyi-backend:26-06-02_20-13-57`。

BLOCKED: 首次执行统一发布脚本 -> FAIL，`yudao-ui-admin-vue3` 前端构建阶段报错 `Cannot find module '@babel/helper-validator-identifier'`，导致发布未完成。

DIAGNOSIS: `pnpm-lock.yaml` 已声明 `@babel/helper-validator-identifier@7.25.9`，但本机 `node_modules` 目录处于损坏状态；`pnpm build:test` 与 `node -e "require('@babel/helper-validator-identifier')"` 均失败。

GREEN: `pnpm install --force` -> PASS，重建 `yudao-ui-admin-vue3` 依赖目录。

GREEN: `pnpm build:test` -> PASS，前端测试构建恢复正常。

GREEN: `powershell -ExecutionPolicy Bypass -File script\deploy\publish-int-ruoyi.ps1 -Environment test -SkipDatabaseSync -SkipMinioSync -Tag 20260602_dcc_viewer_token_latest_f9f50cf157 ...` -> PASS，统一发布脚本完成测试服 code-only 发布，远端 backend / frontend / website / onlyoffice 健康检查全部通过。

GREEN: 发布后只读核对 -> `show-int-ruoyi-test-status.bat` 显示当前 release package 为 `20260602_dcc_viewer_token_latest_f9f50cf157`，`intruoyi-backend` 运行镜像为 `intruoyi-backend:20260602_dcc_viewer_token_latest_f9f50cf157`，`http://172.30.30.58:48081/actuator/health` 返回 `{"status":"UP"}`。
