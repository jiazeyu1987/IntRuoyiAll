# Bug Regression Evidence

## Bug

AC-M16 生产班组长确认员工报工存在三类代码级缺口：`PRODUCTION_SUBMIT` 可通过通用复核接口直接 `APPROVED`，已退回复核后的生产报工仍可能进入分配链路，非生产组长的 `leaderType` 会在加载事件后才被后续校验间接拒绝。

## Expected

生产报工通过必须使用报工分配确认链路；退回必须保留原始提交和退回原因；同一提交事件已有任意终态复核后，重复确认、覆盖原始记录或退回后继续分配必须 fail-fast。

## Reproduction

RED: `java @doc\tasks\20260805-ac-m16-report-confirmation-hardening\junit-console-red.args` -> FAIL, 新测试配旧服务实现时 13 个服务测试中 3 个失败：通用生产通过未抛异常、非生产组长返回事件上下文错误、退回后继续分配落到 PQC 绑定错误。

## Root Cause

通用提交复核服务只在 PQC `APPROVED` 时触发汇集，没有禁止 `PRODUCTION_SUBMIT` 的 `APPROVED` 绕过正式报工分配确认；报工分配确认服务只检查 allocation 明细重复，未先锁定 `mes_pro_process_pool_submission_review` 的事件终态；确认入口也没有在加载事件前限定 `leaderType=PRODUCTION`。

## GREEN:

`java @doc\tasks\20260805-ac-m16-report-confirmation-hardening\junit-console-green.args` -> PASS，`MesTeamLeaderSubmissionReviewServiceTest` 与 `MesTeamLeaderReportConfirmationServiceTest` 共 13 个测试全部成功。

## Verification

修复后新增/更新的回归覆盖：生产报工通用通过必须拒绝、退回后继续分配必须拒绝、非生产组长确认报工必须在加载事件前拒绝；同时保留 PQC 复核通过/退回、生产手工分配、重复 allocation、分配总量不符等相邻路径。

## Blockers

标准 Maven 命令 `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesTeamLeaderReportConfirmationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 与不带 `-am` 版本多次超时；当前机器存在并行 Maven 进程占用同一模块/本地仓库，已记录为环境阻塞，未冒充 Maven 通过。
