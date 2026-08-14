# Execution Log

## User Intent

- 2026-08-03：用户确认先切换成最新的 `int_main` 代码，解决当前 `48081` jar 未包含 DCC 受控打印记录接口的问题。

## Preflight

- 当前分支：`int_main`。
- 当前 HEAD：`6f5f52814547146d9c90cd70f34e8a274751ed32`。
- 当前主工作区存在并发未提交改动，因此按隔离构建门禁，不从 `E:\IntRuoyi` 脏目录直接打包。
- 当前 `48081` PID：`43876`，运行 jar：`E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260803-115911-rrm-m6-pqc-skip-submitted.jar`。
- 已复现登录态目标接口返回 `code=404`、`msg=请求地址不存在:admin-api/dcc/controlled-files/2054545668044052098/controlled-print/records`。

## BDD

BDD: 最新 int_main 运行态加载 DCC 受控打印记录接口 -> Given 当前 `int_main` 源码和 `origin/int_main` 均包含 `controlled-print/records` 后端映射 When 本机 `48081` 切换到当前 `int_main` 干净构建 jar Then 登录态请求目标受控打印记录接口不再返回“请求地址不存在”。

## RED / GREEN / REGRESSION

- RED: 登录态只读请求 `/admin-api/dcc/controlled-files/2054545668044052098/controlled-print/records` -> FAIL，当前旧运行 jar 返回 `code=404` 与“请求地址不存在”。
- GREEN: `mvn.cmd -pl yudao-server -am "-DskipTests" package` in `D:\IntRuoyiWorktree\20260803-int-main-runtime-switch\IntRuoyiBackend` -> PASS，`BUILD SUCCESS`，生成 `yudao-server\target\yudao-server-exec.jar`。
- GREEN: jar inspect -> PASS，`yudao-module-dcc-2026.04-SNAPSHOT.jar` 内 `DccControlledFileController.class` 包含 `controlled-print/records`。
- GREEN: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> PASS，`status=UP`。
- GREEN: 登录态只读请求 `/admin-api/dcc/controlled-files/2054545668044052098/controlled-print/records` -> PASS for route load，返回业务校验 `code=1080000189` / `Current controlled file cannot be printed as a controlled copy`，`RouteMissing=False`。

## Build And Runtime Evidence

- Detached worktree：`D:\IntRuoyiWorktree\20260803-int-main-runtime-switch`，HEAD `6f5f52814547146d9c90cd70f34e8a274751ed32`。
- Maven package：`mvn.cmd -pl yudao-server -am "-DskipTests" package`，Total time `06:41 min`，`BUILD SUCCESS`。
- Runtime jar copied to：`E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260803-int-main-6f5f52814.jar`。
- Runtime jar SHA256：`D097DB0D8A1A846C03860E3186EF833A9A12C3146CDC176DBFF9AF9CB6E48C6B`。
- Old runtime：PID `43876`，jar `backend-runtime-control-20260803-115911-rrm-m6-pqc-skip-submitted.jar`。
- New runtime：PID `42064`，jar `backend-runtime-control-20260803-int-main-6f5f52814.jar`。
- Startup command retains `--server.port=48081`, `--spring.profiles.active=local`, stable runtime logs under `E:\IntRuoyi\output\runtime\int_main\logs`, and repo root `E:\IntRuoyi\IntRuoyiBackend`.

## Verification

- Health check：`UP` after 5 attempts.
- Target URL no longer route-missing：`RouteMissing=False` for `/dcc/controlled-files/2054545668044052098/controlled-print/records`.
- Business result note：目标文件当前业务上不能作为受控副本打印，因此返回正式业务错误 `1080000189`；这与旧 jar 的“请求地址不存在”不同，证明 Controller 映射已加载。
- Cleanup：`git worktree remove --force D:\IntRuoyiWorktree\20260803-int-main-runtime-switch` -> `REMOVE_OK`。
- Experience consolidation：无需新增长期经验；本次场景已被 `docs/local-runtime.md#2026-07-24-隔离构建-jar-加载门禁` 和 `docs/experience-index.md` 中 DCC 受控打印关键词覆盖。

## Closeout Blocker

- 本任务运行态目标已完成，但未做提交/推送。
- 原因：主工作区在本任务开始前已有多项并发未提交改动且 `int_main...origin/int_main [ahead 2]`，包括 DCC 追溯、上传浏览页缓存、设备账号绑定等无关任务文件。
- 处理：为避免宽泛基线提交混入非本任务文件，本轮仅保留任务文档和运行态证据，状态标记为 `blocked`，不改写或清理并发任务改动。
