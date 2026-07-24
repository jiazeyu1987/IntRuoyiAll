# 执行日志：修复 DCC 文件下载失败

BDD: 文件浏览页下载现行文件 -> Given 用户在 DCC 文件浏览页看到现行受控文件 / When 点击该行“下载” / Then 前端必须携带唯一下载请求号调用后端下载接口，后端返回文件内容、文件名和下载审计响应头。

BDD: 缺失下载请求号时失败 -> Given 下载接口未收到有效下载请求号 / When 后端处理下载请求 / Then 后端必须明确拒绝并返回业务错误，不得创建下载记录或返回未审计文件。

## 证据

- 2026-06-02：开始定位用户截图中的 `PD可编辑` 文件浏览页下载失败问题。
- 2026-06-02：用户切换到运行控制台“混滚版本”缺少发布责任人的新缺陷；本任务暂停为 blocked，避免与新的运行控制台修复混合提交。后续恢复时需继续完成下载链路回归与真实路径验证。
- 2026-06-03：恢复本任务。确认运行中 48081 Java 进程使用旧包 `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-20260602-200530.jar`，而当前源码与控制器单测已包含 `Content-Disposition` 暴露契约。
- RED: `node doc/tasks/20260602-dcc-download-failure/verify-download-response-headers.mjs` -> FAIL, expected reason: 旧运行包返回 `200` 和 DCC 文件字节，但 `Access-Control-Expose-Headers` 仅包含 DCC 证据头，缺少 `Content-Disposition`，前端无法读取服务端文件名。
- M2: Completed. 已用真实测试租户 `aoteman` 的现行 DCC 文件复现运行时响应头契约缺失。
- M3: Completed. 使用当前源码重新打包并启动本机后端，48081 运行 jar 更新为 `output/runtime/int_main/backend-runtime-control-20260603-085757.jar`。
- GREEN: `node doc/tasks/20260602-dcc-download-failure/verify-download-response-headers.mjs` -> PASS, controlledFileId=`2054545668044046252`, fileNumber=`CODEX-E2E-FOURTH-5662414`。
- GREEN: `mvn --% -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 7 tests。
- E2E-PRECONDITION: 登录页动态模块曾因前端 `node_modules/@typescript-eslint/parser/dist/index.js` 缺失返回 500；执行 `pnpm install --frozen-lockfile` 恢复锁文件依赖并重启 8081 后继续真实路径验证，未修改前端源码或 lockfile。
- M4: Completed. Playwright 使用测试租户从 `http://localhost:8081` 登录，点击左侧菜单 `DCC文控中心 -> DCC受控浏览`，在目录 `DCC E2E Documents` 点击现行文件 `CODEX-E2E-FOURTH-5662414` 的“下载”。
- GREEN: Playwright 真实前端路径 -> PASS, downloadUrl=`http://127.0.0.1:48081/admin-api/dcc/controlled-files/2054545668044046252/download?...`, suggestedFilename=`codex-e2e-stamped.pdf.dcc`, downloadedBytes=`1143`, `Content-Disposition=attachment; filename="codex-e2e-stamped.pdf.dcc"`, `Access-Control-Expose-Headers` 包含 `Content-Disposition`, failureToastCount=`0`。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260602-dcc-download-failure\bug-regression-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-dcc-download-failure --mode preview` -> PASS, delete `<none>`, blocked `<none>`, warnings `<none>`。
- M5: Completed. 缺陷证据已通过校验，收尾清理预览无待删临时产物或阻塞项。
