# Verification Report

## Scope

- 用户请求提交前后端代码并推送当前 `int_main`。
- 本轮提交范围为当前工作区已存在的前后端源码、测试、运行脚本、任务证据和经验文档改动；按脏工作区基线门禁作为独立基线提交保存。

## Verification Results

- `git status --short --branch` initial: PASS, `## int_main...origin/int_main`，存在前后端工作区脏改动。
- `git branch --show-current`: PASS, `int_main`。
- `git remote -v`: PASS, `origin` fetch/push 指向 `https://github.com/jiazeyu1987/IntRuoyiAll.git`。
- `git diff --check`: PASS，无空白错误。
- UTF-8 readback for task docs: PASS。
- First commit: PASS, `9af3ef9a docs: record current code push task`。
- Post-first-commit rescan: PASS, branch ahead 1 and no unstaged residual diff.
- `task-closeout-cleanup` preview: PASS, keep task records, delete none, blocked none, warnings none.
- User scope confirmation: PASS, user replied `确认` to include later non-task current changes.
- Backend minimal regression: PASS, `mvn -pl yudao-module-mes -am "-Dtest=Sheet1RouteExcelParserTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` ran 4 tests with 0 failures and 0 errors.
- Branch runtime port guard: PASS, `scripts\preflight\branch-runtime-port-guard.ps1` passed for `int_main/int_main` ports `8081/48081`.
- Sensitive scan: PASS, no raw credential material found in newly included files.
- Stale backend blocker recheck: PASS, route generation JSON compile target Maven tests ran 3 tests with 0 failures and 0 errors.
- Downstream stale blocker recheck: PASS, extra-form switch target Maven tests ran 2 tests with 0 failures and 0 errors.
- Branch runtime port guard: PASS, `scripts\preflight\branch-runtime-port-guard.ps1` passed for `int_main/int_main` ports `8081/48081`.
- Staged whitespace check: PASS, `git diff --cached --check` returned no whitespace errors.
- Staged large-file scan: PASS, no staged file exceeded 100 MB.
- Current workspace baseline commit: PASS, `91441260 chore: baseline current frontend backend changes`.
- Post-baseline rescan: PASS, `git diff --name-status` empty and branch ahead 1.
- Cleanup preview/apply: PASS, keep task records, delete none, blocked none, warnings none.
- Task closeout commit: PASS, `a4faf67d docs: close current frontend backend commit task`.
- Push large-object gate: PASS, no objects over 100 MB in `origin/int_main..HEAD`.
- Push first attempt: BLOCKED, normal `git push origin int_main` failed twice with `Recv failure: Connection was reset`.
- Root cause: global Git config routes GitHub through `http://127.0.0.1:8902`; the local proxy accepts CONNECT then resets the TLS stream. Browser GitHub access still works because it is not proof that Git is using the same proxy path.
- Push final: PASS, `git -c http.https://github.com.proxy= -c http.proxy= push origin int_main` succeeded.

## Blocker

- Previous push pause is resolved by the user's `继续` instruction and the dirty-worktree baseline policy. The current workspace changes to be committed as a separate baseline are:
- `D IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/importer/Sheet1RouteExcelImportServiceImplDbTest.java`
- `D IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/importer/Sheet1RouteExcelImportServiceImplTest.java`
- `M IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/importer/Sheet1RouteExcelParserTest.java`
- `D IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/route/importer/Sheet1RouteExcelTestFixtures.java`
- `?? doc/tasks/20260728-restart-local-runtime/execution-log.md`
- `?? doc/tasks/20260728-restart-local-runtime/task.md`
- Resolved: user confirmed these changes are in scope for current-code commit/push.

## Final Closeout Verification

- Cleanup apply passed.
- Final closeout commits through `5946a5b6` were pushed successfully with the GitHub proxy override disabled for that command.
- Completion-record commit `04643b9d` is local only because the follow-up no-proxy push could not connect to `github.com:443`.
- Task status remains blocked until the final local commit(s) are pushed and `git status --short --branch` no longer reports ahead.
