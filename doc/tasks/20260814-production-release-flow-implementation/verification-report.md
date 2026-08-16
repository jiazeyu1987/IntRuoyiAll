# 生产放行全流程验证报告

## Verdict

- 结论：`BLOCKED`。
- 已证明：T1-T10 实现级目标测试、24 模块编译、SP-1 至 SP-4 前端合同、类型检查、角色 SQL 和 T11 Playwright 规格解析通过。
- 未证明：真实本机运行态下的七账号页面主链、反向权限、真实附件上传、最终放行即时追溯和任务自有数据清理。
- 禁止结论：Playwright `--list`、静态合同、Maven 或 API 结果均不能替代 TC-13 真实 E2E。

## Environment

- 目标 worktree：`D:\IntRuoyiWorktree\pqc-production-release-flow`。
- 分支/HEAD：`codex/pqc-production-release-flow` / `336c82887`。
- 集成目标：`E:\IntRuoyi` 的 `int_main`，HEAD `ecb05caa615c384b3833dd9d7b9b9594df3ad30e`。
- 集成方式：独立集成 worktree 生成双父融合提交后，`int_main` 使用 `git merge --ff-only` 快进；未覆盖主工作区既有并行改动。
- 目标端口：frontend `8089`、backend `48089`；审计时均无监听。
- 运行守卫：PASS；v4 `1..30` 合同已通过独立提交 `b68db945b` 同步，slot 8 正式解析为 frontend `8089`、backend `48089`。
- E2E 前置：30 个 `EDHR_FULL_E2E_*` 正式变量缺失，未读取或记录任何秘密；新增缺口为未完成进度订单 ID/工单号和 cleanup plan reference。

## Requirement Audit

| 范围 | 自动化证据 | 真实系统证据 | 门禁结论 |
| --- | --- | --- | --- |
| AC-01 至 AC-06，SP-1 组长门禁、申请、幂等和回执 | 目标 JUnit、SP-1 合同和组合回归通过 | 未以真实组长账号和任务自有双 100% 活跃订单执行页面提交 | BLOCKED |
| AC-07 至 AC-13，SP-2 PQC 权限、拒绝、唯一批次和三类正式来源 | PQC、批次、三类 writer、角色 SQL 和 SP-2 合同通过 | 未以 `zhulijiang` 与非候选账号执行真实页面正反路径 | BLOCKED |
| AC-14 至 AC-19，SP-3 四任务、附件、版本和第四份原子交接 | report service、WorkTask、special-node 和 SP-3 合同通过 | 未以 1/1/2 三负责人上传四个真实附件并核对任务清零 | BLOCKED |
| AC-20 至 AC-31，SP-4 管理者授权、快照、CAS、审计和 trace | manager/release/trace JUnit、SP-4 合同和类型检查通过 | 未以 `xujianhai` 与非候选账号执行最终放行和即时 trace | BLOCKED |
| AC-32，真实端到端主链路 | 新规格可被 Playwright 识别 | 未启动运行态、未登录、未执行任何业务写请求 | BLOCKED |
| AC-33 至 AC-34，三链路和数据库约束回归 | 正式来源、schema、CAS、SQL 目标测试及三项相邻静态回归通过 | 独立 tester 已复验静态门禁；真实运行态仍未建立 | BLOCKED |

## Verification Evidence

- `mvn -o -pl yudao-module-mes "-Dtest=MesProductionReleaseApplySp1Test,MesPqcReleaseBatchExecutionServiceTest,MesProductionReleaseReportServiceTest,MesProductionReleaseManagerApprovalServiceTest,MesProductionReleaseTraceContractTest,MesProEdhrReleaseServiceImplTest" "-DforkCount=0" test` -> PASS，60 tests。
- `mvn -o -pl yudao-module-mes -am "-DskipTests" compile` -> PASS，24 reactor modules。
- SP-1、SP-2、SP-3、SP-4 命名前端合同 -> PASS。
- `pnpm ts:check` -> PASS。
- `python -X utf8 -m pytest script/tests/test_mes_production_release_roles_sql.py -q` -> PASS，6 tests。
- `pnpm exec playwright test tests/e2e/sp0-sp4-production-release-real-flow.spec.ts --list` -> PASS，仅证明规格可解析。
- `node tests/e2e/edhr-release-flow-trace-print-static.spec.js` -> PASS。
- `node tests/e2e/edhr-special-node-attachment-actions-static.spec.js` -> PASS。
- `node tests/e2e/edhr-special-node-skip-signature-static.spec.js` -> PASS。
- `test-report.md#p11-verdict` -> 独立 tester 增量复验完成；三个提交边界、干净工作树、v4 guard 和静态门禁均通过，真实 E2E 未执行，结论 `BLOCKED`。
- `git diff --check` -> PASS。
- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py -q` -> PASS，14 tests。
- `pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS。
- T6-T10 实现提交：`5227b8c2e`，57 个精确核对文件；T11 验收资产提交：`336c82887`，4 个精确核对文件。
- 融合提交：`ecb05caa6`，父提交为原 `int_main` `1e8ec9b81` 和功能分支 `336c82887`；功能分支已成为 `int_main` 祖先。
- 融合后 `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS，24 reactor modules。
- 融合后生产放行六组测试加 `MesTeamLeaderActiveOrderServiceTest` -> PASS，90 tests，0 failures，0 errors，0 skipped。
- 融合后 SP-1 至 SP-4、`pnpm ts:check`、三项相邻静态合同、角色 SQL 6 tests、Playwright `--list` 和 runtime guard -> 全部 PASS。
- 临时集成 worktree `D:\IntRuoyiWorktree\pqc-production-release-flow-integration` 已从 Git 登记和磁盘删除；slot 24 登记已标记 `active=false`，未影响其它 worktree。
- T11 当前规格和独立复核：未完成进度零写入、交叉反向权限、字符串 ID/附件哈希、禁止绕过端点、最终只读核验和 cleanup handoff 合同均通过；Playwright `--list`、SP-1 至 SP-4、`pnpm ts:check`、Prettier 和差异检查均 PASS。真实 E2E 没有执行。
- T11 规格提交：`8ca580be3`，仅包含 `sp0-sp4-production-release-real-flow.spec.ts`；在并发任务推进后的 `fe117216c` 上重新执行 Prettier、Playwright `--list`、SP-1 至 SP-4 和 `pnpm ts:check`，全部 PASS。

## Open Blockers

- 30 项真实 E2E 前置变量全部缺失，无法建立七账号、主链/拒绝/未完成三组任务 fixture、四附件、最终签核和页面清理计划的本机运行态；当前 8081/48081 监听来源也未证明为 `ecb05caa6`。
- 用户已授权先融合到 `int_main` 并在后续自行手动测试；当前没有真实页面验收结果可记录。

## Required To Unblock

1. 用户在 `int_main` 当前代码上完成真实页面手动测试，至少覆盖七角色正反权限、两条订单、四附件、最终签核、双条件追溯和测试数据清理，并提供结果证据。
2. 若改为自动验收，则提供七个测试账号、两个任务自有双 100% 活跃订单、四份测试附件、灭菌批号、签核证据、本机入口、租户和显式写入确认；不得记录秘密。
3. 真实验收完成后更新 `test-report.md`，再决定 P11 PASS 或记录具体失败。
