# 测试服第三方报工导入不生效原因验证报告

## Conclusion

- 结论：不是测试服数据首先导致，也不是本机修复无效；当前已确认的根因是测试服发布包没有包含本机已验证的第三方报工修复提交。
- 本机成功来自工作区源码和本地 runtime Jar；测试服运行的是 clean release worktree 产物，source commit 为 `b99246f58`，该提交缺少 `DirectWorkstationResolution` 修复。

## Evidence

- Test server current release: `release-20260802-intmain-head-test-r260802b-r1`.
- Test server release-info source commit: `b99246f58ff7d556caee24307ec89b662d0427e3`, `dirty=false`.
- Current test server backend health: `UP`.
- Current test server frontend HTTP: `200`.
- Fix commit: `b8533d59a fix: persist direct feedback import progress`.
- Fix branch: `codex/third-party-feedback-import-20260802`.
- `b8533d59a` is not an ancestor of `b99246f58`.
- `b8533d59a` is not an ancestor of `origin/int_main`.
- `HEAD` and `origin/int_main` versions of `ThirdPartyFeedbackImportServiceImpl.java` do not contain `resolveDirectFeedbackWorkstation` / `DirectWorkstationResolution`.
- Working tree version of `ThirdPartyFeedbackImportServiceImpl.java` does contain `resolveDirectFeedbackWorkstation` / `DirectWorkstationResolution`.

## Impact

- Any test release built from `b99246f58` will miss the direct-work-report workstation resolution fix.
- The test server can be healthy and still fail this business path because the running image is an older business implementation.

## Required Fix Path

- Merge or cherry-pick `b8533d59a` into the actual release branch/source.
- Commit and push through normal Git policy.
- Rebuild a new clean releaseTag from a clean release worktree.
- Publish that new releaseTag to test server.
- Re-run the real `李萍.xlsx` third-party import path on test server and verify formal feedback list plus schedule order progress.
