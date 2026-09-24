# 执行记录

## BDD 场景

BDD: 批次执行历史追溯详情可展示正式上市放行事实 -> Given 批次执行存在成功的 `BATCH_RECORD_RELEASE_APPROVED` 审计事件，旧审计元数据可能缺少 `releaseTransactionId`，但审计行保留其批次执行 ID 且该批次唯一关联正式放行事务；When 用户在历史追溯点击该批次的详情；Then 详情服务从该正式事务读取放行签名并正常返回，不因旧元数据缺字段抛出系统异常；若元数据显式事务 ID 与唯一正式事务不一致，则拒绝返回错误关联。

## TDD 记录

RED: `mvn -pl yudao-module-mes -am '-Dtest=MesTeamLeaderActiveOrderReleaseAuditRecorderTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, 扩展的 `activeOrderFormalFactSnapshotUsesDigestForAuditSummaryAndKeepsSourceInMetadata` 断言正式放行事务 ID 7601 时得到 null。
RED: `mvn -pl yudao-module-mes -am '-Dtest=MesTeamLeaderActiveOrderDetailServiceImplTest#marketReleaseOperationFactResolvesFormalTransactionForLegacyAuditMetadata' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, 旧审计元数据缺少事务 ID 时抛出 `ACTIVE_ORDER_MARKET_RELEASE_TRANSACTION_ID_MISSING`。
RED: `mvn -pl yudao-module-mes -am '-Dtest=MesTeamLeaderActiveOrderDetailServiceImplTest#marketReleaseOperationFactRejectsConflictingMetadataTransactionId' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, 事务映射为按批次执行 ID 返回的 7601，而元数据声明 7602；旧实现未拒绝冲突，测试提示 “Expected java.lang.IllegalStateException to be thrown, but nothing was thrown”。
GREEN: `mvn -pl yudao-module-mes -am '-Dtest=MesTeamLeaderActiveOrderDetailServiceImplTest#marketReleaseOperationFactReadsSignatureIdFromReleaseTransaction+marketReleaseOperationFactResolvesFormalTransactionForLegacyAuditMetadata+marketReleaseOperationFactRejectsConflictingMetadataTransactionId,MesTeamLeaderActiveOrderReleaseAuditRecorderTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 最终重跑详情关联 3 项、审计写入 4 项，共 7 项全部通过，Failures=0、Errors=0。
REGRESSION: `mvn -pl yudao-module-mes -am '-Dtest=MesTeamLeaderActiveOrderReleaseAuditRecorderTest,MesTeamLeaderActiveOrderDetailServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> 详情测试类 26 项中 2 项因测试装配的 `pickListMapper` 为空报 NPE，审计记录器 4 项通过；这两项未进入本任务定向回归，不能将详情全类结果记作通过。

## 里程碑状态

- 根因已由本地后端运行日志定位：详情读取放行审计事件时，`toOperationFact` 抛出 `ACTIVE_ORDER_MARKET_RELEASE_TRANSACTION_ID_MISSING`；正式放行事务 ID 已存在于写入命令，但审计记录器尚未序列化该字段。历史审计行仍保存 `batchExecutionId`，可按 `mes_pro_edhr_release_transaction` 的租户、批次执行 ID、未删除唯一键读取正式事务。
- 实现：审计记录器序列化正式放行事务 ID；详情按审计行的批次执行 ID查询正式事务，并在快照事务 ID 存在时交叉校验，历史行无快照 ID时仍可展示正式放行签名。
- 验证：最终重跑定向测试 7 项全部通过；更宽详情类运行仍有 2 项测试装配空 `pickListMapper` 的 NPE，已单独记录。
- 验证器：bug-regression 与 backend-api 两个 validator 的 self-test 和任务证据检查均通过。
- 差异检查：受影响源文件、测试和经验文档的 `git diff --check` 退出码为 0；只有 LF/CRLF 转换告警。
- 经验沉淀：将正式放行事务审计关联、历史快照缺字段时的唯一关系解析和冲突校验规则并入 `docs/backend-development.md`，并更新 `docs/experience-index.md`。
- Cleanup preview: PASS；当前为 `int_main` 主工作区而非 linked worktree，保留 `task.md`、`execution-log.md`、`verification-report.md`，只删除本任务的临时 `backend-api-evidence.md` 与 `bug-regression-evidence.md`，无 blocked/warnings。
- Cleanup apply: PASS；仅删除上述两个临时 evidence 文件，任务记录与验证报告保留。
- Git 收尾：未执行提交或推送。用户提供的仓库规则要求当轮明确授权，而当前轮没有该授权；项目规则要求推送后才能标记完成，因此状态保持 `blocked`，不得标为 `completed`。
- 当前状态：代码、定向验证与 cleanup 已完成。
- Commit: `7f33d911afe225e46dd25f425b2f10f1cdc1f37a`（`修复批次历史详情放行事务关联`）；push: `origin/int_main` 成功，远端由 `314d846e3` 更新到该提交。
- 提交边界复核：提交仅含本任务代码、测试、经验规则和任务记录；同目录文件内其它工作区修改仍保持未提交。
- Runtime read-only check: `48081` 由 Java PID 64784 监听，启动 Jar 为 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260923-135239.jar`，命令行 repo-root 为 `E:\IntRuoyi\IntRuoyiBackend`；health 返回 `HTTP 200 {"status":"UP"}`。
- Runtime restart: 首次标准重启的完整 Reactor 构建因共享详情 Java 文件中的工作区并行版本冲突失败（`releaseTransactionId` 未定义、`setSourcePickListDocumentStatuses` 不存在）；旧进程保持运行。将该文件恢复为已提交修复版本，同时保留其余业务逻辑后，重跑标准重启成功：31 模块 Reactor `BUILD SUCCESS`，执行脚本退出码 0。
- Runtime verification: 新 Java PID 52392 运行 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260923-224959.jar`，repo-root 为 `E:\IntRuoyi\IntRuoyiBackend`；日志 `2026-09-23 22:51:32` 出现 `Started YudaoServerApplication`，`/actuator/health` 返回 UP。
- 实现提交 `7f33d911afe225e46dd25f425b2f10f1cdc1f37a`；收尾记录提交 `b4ac507c3`。均已推送 `origin/int_main`，远端与本地 HEAD 一致。
