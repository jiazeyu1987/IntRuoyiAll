# f2d2 int_main Integration Verification

## Result

PASS. f2d2 工作树 HEAD `6c6487c9f` 已是 `int_main` 祖先，当前 `int_main` 已包含该工作树提交。本轮继续清理冗余重放留下的 MES 合并索引冲突，并保留主线新增逻辑。

## Evidence

- `git merge-base --is-ancestor 6c6487c9f int_main` -> PASS。
- `git rev-list --left-right --count codex/20260913-f2d2-int-main-integration...int_main` -> `0 9`，证明 `int_main` 已包含源分支且领先 9 个提交。
- `git diff --check origin/int_main..HEAD` -> PASS。
- `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`int_main/int_main` frontend 8081 backend 48081。
- MES conflict resolution: `MesProEdhrNonconformanceReviewServiceImpl` 同时保留 QA 处置电子签名快照、工单冻结生命周期窗口计算和 `selectFreezeLifecycleByWorkOrderId` 查询。

## Static Verification

- `node IntRuoyiBackend\yudao-module-dcc\src\test\js\dcc-static-024-training-session-concurrency-contract.spec.cjs` -> PASS。
- `node IntRuoyiFronted\tests\e2e\dcc-static-022-remark-only-checkin-static.spec.cjs` -> PASS。
- `node IntRuoyiFronted\tests\e2e\dcc-browser-checkout-static.spec.js` -> PASS。
- `mvn -pl yudao-module-dcc "-Dtest=DccApprovalRouteAdminServiceImplTest,DccControlledFileMetadataUpdateServiceTest,DccControlledFileWorkflowServiceImplTest,DccTrainingTaskServiceTest" test` -> PASS，176 tests，0 failures，0 errors，0 skipped。

## Scope Notes

- 未执行 E2E。
- 未启动或重启服务。
- 未写数据库、未操作远程服务器。
- 未提交 `resource/` Office 草稿或 `IntRuoyiFronted/tsconfig.route-production-migration.tmp.json`。
