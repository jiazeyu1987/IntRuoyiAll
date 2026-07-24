# Execution Log

BDD: 预览重排同编码快照别名归属 -> Given 当前重排范围同时包含同一工序编码的已删除快照目标和唯一当前有效目标，When 资源别名旧工序需要建立身份映射，Then 显式目标保持自身身份，外部旧别名归属到唯一当前有效目标，不能抛出不可用歧义。

RED: mvn -pl yudao-module-mes -Dtest=MesProRouteProcessServiceImplTest#getProcessIdentityMap_shouldMapExternalAliasToOnlyActiveTargetWhenDuplicateCodeContainsDeletedSnapshot test -> FAIL，expected reason: `900400/Z3710` 外部旧工序别名在目标集 `[922864(deleted), 922895(active)]` 中被误判为 `PRO_ROUTE_PROCESS_IDENTITY_AMBIGUOUS`。

GREEN: mvn -pl yudao-module-mes -Dtest=MesProRouteProcessServiceImplTest#getProcessIdentityMap_shouldMapExternalAliasToOnlyActiveTargetWhenDuplicateCodeContainsDeletedSnapshot test -> PASS，外部旧工序 `900400/Z3710` 已归属到唯一当前有效目标 `922895`，显式目标 `922864/922895` 保留自身身份。

REGRESSION: mvn -pl yudao-module-mes -Dtest=MesProRouteProcessServiceImplTest test -> PASS，12 tests。

REGRESSION: python -X utf8 -m pytest script/tests/test_mes_scheduling_identity_keys.py script/tests/test_mes_scheduling_domain_contracts.py -> PASS，5 passed。

GREEN: script\deploy\restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main -> PASS，本机后端重启成功，`48081` 健康检查返回 `{"status":"UP"}`。

GREEN: runtime-class-hash -> PASS，运行 jar `D:\ProjectPackage\Int\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260711-202016.jar` 内 `MesProRouteProcessServiceImpl.class` SHA256 与本地编译 class 均为 `18B1C40263C088662E5091DF2E0805C6565471AAB75494F32D1416BAE6E83E63`。

GREEN: runtime-log-check -> PASS，最新运行日志存在 `Started YudaoServerApplication`，未发现新增 `sourceProcessId=900400`、`processCode=Z3710`、`candidateRouteProcessIds=[922864, 922895]` 歧义标记。

GREEN: implementation-commit -> PASS，已提交 `2de996340c 任务: 修复预览重排快照工序别名归属`。

GREEN: task-closeout-cleanup preview -> PASS，保留核心任务记录，无删除项、无阻塞、无告警。

GREEN: task-closeout-cleanup apply -> PASS，当前仓库为主 worktree，未执行融合或 worktree 删除，无删除项、无阻塞、无告警。
