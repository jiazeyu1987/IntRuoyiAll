# 验证报告：批次执行历史追溯详情系统异常

## 结果

根因是放行审计写入端遗漏正式 `releaseTransactionId`，详情读取端又只按该 JSON 字段找正式放行事务。旧历史行虽保留唯一正式业务关联 `batchExecutionId`，但此前未使用，因此缺少快照字段即抛出 `ACTIVE_ORDER_MARKET_RELEASE_TRANSACTION_ID_MISSING` 并被页面显示为“系统异常”。

审计记录器现写入正式事务 ID。详情改为按审计行批次执行 ID查询正式事务；旧 JSON 缺字段时利用唯一正式关系读取签名，若 JSON 中事务 ID存在则校验一致性，错配时 fail-fast。

## 验证证据

- BDD 与三项 RED / 一项 GREEN 记录见 `execution-log.md`。
- 最终定向命令：`mvn -pl yudao-module-mes -am '-Dtest=MesTeamLeaderActiveOrderDetailServiceImplTest#marketReleaseOperationFactReadsSignatureIdFromReleaseTransaction+marketReleaseOperationFactResolvesFormalTransactionForLegacyAuditMetadata+marketReleaseOperationFactRejectsConflictingMetadataTransactionId,MesTeamLeaderActiveOrderReleaseAuditRecorderTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`。
- 最终重跑结果：BUILD SUCCESS；详情 3 项、审计记录器 4 项通过，合计 7 项，Failures=0、Errors=0。
- 较宽相邻回归：审计记录器 4 项通过；详情测试类 26 项中 2 项因测试装配 `pickListMapper` 为空而 NPE。该结果如实记录，不作为全类通过；目标定向用例均通过。
- 对受影响源文件、测试和经验文档运行 `git diff --check`，退出码 0；Git 提示的仅为现存 CRLF/LF 转换告警。
- `bug-regression-evidence.md` 与 `backend-api-evidence.md` 分别通过正式 validator；两个 validator 的 self-test 均通过，结果已归档于 `execution-log.md`。
- 对受影响源文件、测试和经验文档运行 `git diff --check`，退出码 0；Git 提示的仅为现存 CRLF/LF 转换告警。
- `task-closeout-cleanup` preview/apply 均通过。Preview 显示只删除两份临时 skill evidence 文件；保留 `task.md`、`execution-log.md` 和本报告，且无 blocked/warnings。当前为主工作区，未涉及合并或 worktree 删除。
- 实现提交 `7f33d911afe225e46dd25f425b2f10f1cdc1f37a`，收尾记录提交 `b4ac507c3`；均已推送到 `origin/int_main`，远端与本地 HEAD 一致。
- 只读运行态检查：48081 Java PID 64784 使用 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260923-135239.jar`，命令行 repo-root 为当前 `IntRuoyiBackend`；health 为 HTTP 200 / `UP`。
- 未运行 E2E；本轮未要求 E2E。未写数据库、未重启或部署 `int_main`。

## 未完成事项

- 变更已提交并推送，但尚未加载到运行环境。标准重启脚本会执行完整 `yudao-server -am package` 及本地 schema、MinIO 和依赖端口检查；当前验证仅覆盖 MES 模块定向测试，且主工作区有大量并行脏改动，本轮因此未触发标准重启，任务状态为 `blocked`。需完成服务器聚合构建验证并确认其影响范围后再重启后端。
- 更宽详情测试类存在上述两个测试装配 NPE；这不影响通过的三项放行详情定向回归，但需单独处理测试夹具后才能宣称该测试类全绿。
