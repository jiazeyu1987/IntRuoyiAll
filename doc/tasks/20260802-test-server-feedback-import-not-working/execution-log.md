# 测试服第三方报工导入不生效原因排查执行日志

## User Intent

- 用户反馈：修改后本机可以，发布到测试服务器后第三方报工仍报不上，询问原因。

## Rule And Skill Evidence

- Read `bug-regression-fix-loop` and `references/bug-contract.md`.
- Read `docs/server-access.md`, `docs/release-backup-restore.md`, `docs/backend-development.md`, `docs/database-rules.md`, `docs/task-closeout-rules.md`, `docs/powershell-encoding.md`, `docs/local-runtime.md`, `docs/worktree-restrictions.md`.
- Read `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\test-release-preflight.md` and `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`.

## BDD

- `BDD: 测试服第三方直报应加载同一本机已验证修复 -> Given 本机导入李萍.xlsx 已产生正式报工并更新排产进度, When 用户把当前版本发布到测试服后在测试服执行同一路径导入, Then 测试服发布包必须包含修复提交且导入成功后正式报工列表/排产工单进度更新；若未更新，应能定位为发布包、运行态版本或正式数据条件问题。`

## Investigation Log

- `git status --short --branch` shows `int_main...origin/int_main [ahead 2]` with many unrelated dirty files; current app HEAD is `b99246f58`.
- `git grep ... HEAD -- ThirdPartyFeedbackImportServiceImpl.java` did not find `DirectWorkstationResolution` or `resolveDirectFeedbackWorkstation`.
- `git grep ... origin/int_main -- ThirdPartyFeedbackImportServiceImpl.java` did not find `DirectWorkstationResolution` or `resolveDirectFeedbackWorkstation`.
- Working tree file `ThirdPartyFeedbackImportServiceImpl.java` does contain `resolveDirectFeedbackWorkstation` and `DirectWorkstationResolution`, proving the local runtime was built from uncommitted working-tree source.
- Test server frontend `http://172.30.30.58:8081/release-info.json` returns releaseTag `release-20260802-intmain-head-test-r260802b-r1`.
- Test server release-info sourceRepos backend/admin-frontend commit is `b99246f58ff7d556caee24307ec89b662d0427e3`, `dirty=false`.
- `git log --all --grep="persist direct feedback import progress"` found fix commit `b8533d59a fix: persist direct feedback import progress`.
- `git branch --all --contains b8533d59a` shows only `codex/third-party-feedback-import-20260802` and `origin/codex/third-party-feedback-import-20260802`.
- `git merge-base --is-ancestor b8533d59a b99246f58` -> not ancestor.
- `git merge-base --is-ancestor b8533d59a origin/int_main` -> not ancestor.
- Backend health on test server: `http://172.30.30.58:48081/actuator/health` -> `{"status":"UP"}`.
- Frontend HTTP on test server: `http://172.30.30.58:8081/` -> HTTP `200`.

## Root Cause

- The code is implemented and proved locally, but it was not part of the clean release source used by the test server.
- The test server currently runs `release-20260802-intmain-head-test-r260802b-r1`, built from app commit `b99246f58`.
- Commit `b99246f58` does not contain the workstation-resolution fix required by the successful local import path.
- Therefore test server still behaves like the pre-fix code path for the Li Ping direct work report import.

## Next Required Work

- Merge or cherry-pick `b8533d59a` from `codex/third-party-feedback-import-20260802` into the release source branch after dealing with current dirty workspace policy.
- Run the focused backend regression for `ThirdPartyFeedbackImportServiceImplTest`.
- Build a new clean releaseTag from a clean release worktree and publish it to test server.
- Verify the new test release `release-info.json` sourceRepos commit contains `b8533d59a`, then run the real import path on test server.
