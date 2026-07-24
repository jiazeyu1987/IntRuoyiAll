# Task: OAuth2 Access Token Redis 运行时回退修复

## Goal

修复 fresh login 成功后，后续受保护接口在读取 OAuth2 access token Redis cache
时一旦抛运行时异常就直接 500 的问题。目标是让 token 读取在 Redis 异常时回退
到 MySQL，而不是把整个 DCC/管理后台请求链路打断。

## Scope

- 在 backend 仓库创建任务文档、执行日志和 bug 回归证据。
- 仅修改 `yudao-module-system` 内与 OAuth2 access token 读取直接相关的服务/测试。
- 先写失败回归测试，再做最小修复。
- 不修改 token 业务语义，不改变登录接口返回结构，不引入 fallback token 数据。

## Previous Task Check

- Previous backend task:
  `doc/tasks/20260516-electronic-batch-record-import-analysis-tabs/task.md`
- Status before this task: completed.
- Related completed tasks:
  - `doc/tasks/20260516-dcc-finalizing-active-status-race-fix/task.md`
  - `doc/tasks/20260516-dcc-controlled-view-entry-watermark/task.md`
- Impact: no unfinished latest backend task blocks this OAuth2 runtime fix; the
  DCC tasks above now depend on this system-level token fallback to stabilize
  fresh authenticated browser runs.

## Milestones

- [x] M1: Create this task package and record the runtime blocker.
- [x] M2: Add RED regression coverage for Redis access-token runtime failure.
- [x] M3: Implement the minimal service-level fallback to DB.
- [x] M4: Run GREEN verification and update bug evidence.
- [x] M5: Commit only this backend task's files if verification fully passes.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system -Dtest=OAuth2TokenServiceRuntimeFallbackTest,OAuth2AccessTokenRedisDAOTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Completed on 2026-05-16. The access-token service now catches runtime Redis
lookup failures and falls back to the existing MySQL token lookup path instead
of surfacing a 500 to protected callers.

## Blocker And Impact

- Blocker: none for this backend task scope.
- Impact: fresh login plus protected DCC/API follow-up requests can now bypass
  Redis token-cache runtime faults and continue on the existing DB fallback
  path.

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system -Dtest=OAuth2TokenServiceRuntimeFallbackTest,OAuth2AccessTokenRedisDAOTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- Live runtime follow-up -> PASS, fresh login token can now read `/admin-api/dcc/file-categories` without the previous Redis `ClassCastException` 500.
