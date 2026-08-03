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

## Build And Runtime Evidence

- Pending.

## Verification

- Pending.

