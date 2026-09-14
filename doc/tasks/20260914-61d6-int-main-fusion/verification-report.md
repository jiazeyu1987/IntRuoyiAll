# Verification Report

## Current Result

ready_for_closeout

冲突后复验通过：DCC query 131 + route 22，MES correction 10，server multipart 4，Python 154；前端 API 合同 10、检入 5、静态 E2E 合同/语法与 `pnpm ts:check` 全部通过。真实 E2E 不在本次验收范围。

## Evidence

- `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，当前分支/int_main profile 为 frontend `8160`、backend `48160`。
- `git diff --check` -> PASS，仅输出 CRLF 工作区提示，无 whitespace error。
- 后端脚本测试：`pytest --basetemp .pytest-tmp\20260914-61d6 IntRuoyiBackend\script\tests\test_publish_int_ruoyi_to_test_tooling.py IntRuoyiBackend\script\tests\test_restart_int_ruoyi_local_schema.py IntRuoyiBackend\script\tests\test_runtime_control_scripts.py` -> PASS，154 passed。
- 后端运行配置静态合同：`powershell -NoProfile -ExecutionPolicy Bypass -File IntRuoyiBackend\script\tests\test_dcc_download_encryption_runtime_config.ps1` -> PASS。
- 分支后端启动静态合同：`node scripts\tests\start-branch-backend-dcc-encryption-static.spec.cjs` -> PASS。
- 前端静态/语法合同：`node scripts\dcc-frontend-api-fail-closed-contract.test.mjs`、`node src\views\dcc\controlled-file\browser\checkin-main-flow.spec.cjs`、`node tests\e2e\dcc-controlled-file-protection.contract.test.js`、`node --check tests\e2e\dcc-controlled-file-protection.e2e.js` -> PASS。
- 前端类型检查：`pnpm ts:check` -> PASS。
- 后端定向 Maven：带引号参数的 `mvn -pl yudao-module-dcc,yudao-module-mes,yudao-server -am "-Dtest=..." "-Dsurefire.failIfNoSpecifiedTests=false" test` -> DCC 346 tests PASS，MES 19 tests PASS；yudao-server 被既有 reactor `maven-dependency-plugin:unpack` 生命周期阻断。
- yudao-server 单独目标测试：`mvn -pl yudao-server "-Dtest=cn.iocoder.yudao.server.UploadMultipartLimitConfigTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，4 tests。

## Known Boundaries

- 未运行真实 E2E；本轮用户要求为提交并融合，且项目规则要求 E2E 仅在当轮明确要求时执行。
- 未执行数据库写入、远程服务器、发布或服务重启。
