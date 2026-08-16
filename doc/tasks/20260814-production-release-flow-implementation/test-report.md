# Test Report

## Latest Incremental Review - int_main

- 验证日期：2026-08-16；验证角色：独立 P11/T11 tester。
- 当前目标：`E:\IntRuoyi` 的 `int_main` T11 真实多账号规格更新；HEAD：`ecb05caa615c384b3833dd9d7b9b9594df3ad30e`。
- 当前暂存区为空。根工作区包含并发任务的既有脏改动；本轮只审计未暂存的 `IntRuoyiFronted/tests/e2e/sp0-sp4-production-release-real-flow.spec.ts`，没有修改、暂存或清理任何并发文件。
- 本节为当前 P11 结论的权威增量记录；下方 worktree 记录保留为先前独立复核历史。

| 检查项 | 实际命令或审计 | 结果 | 独立结论 |
| --- | --- | --- | --- |
| Playwright 规格解析 | `pnpm exec playwright test tests/e2e/sp0-sp4-production-release-real-flow.spec.ts --list` | PASS，列出 1 个 Chromium 用例 | 只证明 runner 可解析规格，不能证明真实业务 E2E。 |
| SP-1 至 SP-4 named contracts | `pnpm test sp1-production-release-contract`、`sp2-pqc-production-release-contract`、`sp3-production-release-report-upload-contract`、`sp4-manager-release-trace-contract` | 全部 PASS | 四个前端阶段合同通过。 |
| TypeScript | `pnpm ts:check` | PASS | `vue-tsc --noEmit -p tsconfig.relaxed.json` 退出码 0。 |
| T11 规格格式 | `pnpm exec prettier --check tests/e2e/sp0-sp4-production-release-real-flow.spec.ts` | PASS | 当前未暂存规格格式通过。 |
| T11 规格差异 | `git diff --check -- IntRuoyiFronted/tests/e2e/sp0-sp4-production-release-real-flow.spec.ts` | PASS | 当前规格差异没有 whitespace error。 |
| 风险词扫描 | `fallback`、`mock`、`default-success`、`TODO`、`FIXME`、`password`、`secret`、`token` | PASS WITH REVIEW | `fallback/mock/default-success/FIXME/secret/token` 均为 0；`TODO=1` 是正式待办状态，`password=5` 均为登录字段或环境变量名，未发现明文秘密或临时成功逻辑。 |
| 入口与写入边界静态审计 | 检查真实登录入口、直接 API 方法、禁止写接口断言、SP-0 未完成进度门禁和 cleanup handoff | PASS | 登录和业务写入均由 Playwright 页面操作触发；无 `page.request.post/put/patch/delete`。唯一直接调用是最终状态核验的 `page.request.get`，符合只读辅助核验边界；规格显式断言五类禁止写接口为 0。 |
| 真实 E2E 前置 | 仅按变量名检查 `EDHR_FULL_E2E_*` 是否存在，未读取或输出任何值 | BLOCKED：required 30，present 0，missing 30 | 新增未完成订单 fixture 和 cleanup plan reference 后，所有账号、fixture、附件、租户、本地 URL、写入确认、签核证据和清理计划仍缺失。 |

本次规格更新增加并静态锁定：第三条未完成进度订单不得产生 SP-1 写请求、三个 fixture ID/工单号互异、所有关键 ID/附件哈希均为正式非空字符串、管理者只能最终批准、放行后执行只读最终状态核验、禁止 skip/删除待提交附件/保存待提交附件/最终拒绝/撤回写接口，以及外部真实页面清理交接记录。以上均为规格和静态门禁，不构成真实 E2E 成功证据。

## Prior Worktree Review (Historical)

- 验证日期：2026-08-16。
- 验证角色：P11/T11 独立 tester；与 T11 executor 分离。
- 任务目录：`E:\IntRuoyi\doc\tasks\20260814-production-release-flow-implementation`。
- 目标 worktree：`D:\IntRuoyiWorktree\pqc-production-release-flow`。
- 分支：`codex/pqc-production-release-flow`。
- HEAD：`336c8288717882087f0b7c0c8bdedb5bec3e7039`。
- 机器可读任务状态：`blocked`；当前阶段：`P11`；`P1` 至 `P10` 为 `completed`，`P11` 为 `blocked`。
- 目标 worktree 状态干净：`git status --short --branch --untracked-files=all` 仅输出分支名；暂存、未暂存和未跟踪文件均为空。
- `git diff --cached --name-status` 输出为空；独立 tester 未修改业务源码、其它任务文档、暂存区或并发 worktree 登记，也未执行 stage、commit、push、服务启动、登录或业务数据写入。

## Commit Boundary Review

| 提交 | 父提交 | 路径数 | 独立边界结论 |
| --- | --- | --- | --- |
| `b68db945ba6928b576907831fe001f9d454ed53c` | `3048b84e828203b6aea83bbf07a2c6caeab5f77e` | 8 | 仅包含 `AGENTS.md`、运行态合同文档、runtime guard/profile 脚本及对应测试；是 v4 slot `1..30` 治理提交。 |
| `5227b8c2ed466d518d6174b0beba902b5074370f` | `b68db945ba6928b576907831fe001f9d454ed53c` | 57 | 生产放行 T6-T10 后端、前端和测试实现；未混入任务文档或 runtime 登记修改。 |
| `336c8288717882087f0b7c0c8bdedb5bec3e7039` | `5227b8c2ed466d518d6174b0beba902b5074370f` | 4 | 仅包含真实多账号 Playwright 规格和三项相邻静态合同。 |

- 三个提交均为当前 HEAD 的线性祖先，顺序为 `b68db945b -> 5227b8c2e -> 336c82887`。

## Verification Results

| 检查项 | 实际命令或证据 | 结果 | 独立结论 |
| --- | --- | --- | --- |
| 相邻静态合同：放行追溯打印 | `node tests/e2e/edhr-release-flow-trace-print-static.spec.js` | PASS | 相邻回归已解除。 |
| 相邻静态合同：特殊节点附件操作 | `node tests/e2e/edhr-special-node-attachment-actions-static.spec.js` | PASS | 相邻回归已解除。 |
| 相邻静态合同：跳过签名 | `node tests/e2e/edhr-special-node-skip-signature-static.spec.js` | PASS | 相邻回归已解除。 |
| SP-1 命名合同 | `pnpm test sp1-production-release-contract` | PASS | 前端 SP-1 合同通过。 |
| SP-2 命名合同 | `pnpm test sp2-pqc-production-release-contract` | PASS | 前端 SP-2 合同通过。 |
| SP-3 命名合同 | `pnpm test sp3-production-release-report-upload-contract` | PASS | 前端 SP-3 合同通过。 |
| SP-4 命名合同 | `pnpm test sp4-manager-release-trace-contract` | PASS | 前端 SP-4 合同通过。 |
| Playwright 真实规格解析 | `pnpm exec playwright test tests/e2e/sp0-sp4-production-release-real-flow.spec.ts --list` | PASS，列出 1 个 Chromium 用例 | 只证明规格可解析，不能证明真实 E2E 通过。 |
| T11 四文件 Prettier | `pnpm exec prettier --check` 后跟三项相邻静态合同和真实 E2E 规格 | PASS | 提交 `336c82887` 的四个验收文件格式通过。 |
| T11 四文件 committed diff check | `git diff --check 336c82887^ 336c82887 --` 后跟上述四个路径 | PASS | 对已提交差异执行检查，无 whitespace error。 |
| Maven 目标回归 | `execution-log.md` Pass 24 与 `verification-report.md` | EVIDENCE VERIFIED：60 tests，0 failures，0 errors，0 skipped | 本 tester 核对既有命令和结果，未将其冒充真实 E2E。 |
| Maven reactor 编译 | `execution-log.md` Pass 24 与 `verification-report.md` | EVIDENCE VERIFIED：24 个 reactor modules，BUILD SUCCESS | 本 tester 核对既有证据，未重复编译。 |
| 角色 SQL | `execution-log.md` Pass 24 与 `verification-report.md` | EVIDENCE VERIFIED：6 passed | 本 tester 核对既有证据，未写数据库。 |
| TypeScript 检查 | `execution-log.md` Pass 24/25 与 `verification-report.md` | EVIDENCE VERIFIED：`pnpm ts:check` 退出码 0 | 本 tester 核对既有证据，未重复执行。 |
| branch runtime guard | `pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` | PASS | v4 guard 正确解析当前分支 `int_main`、slot `8`、frontend `8089`、backend `48089`；旧 guard blocker 已解除。 |
| 本地运行态监听 | 只读检查 `8089`、`48089` 的 LISTEN 数量 | BLOCKED：两端口均为 0 | guard 通过不等于服务已启动；本 tester 未启动服务。 |
| 真实 E2E 前置 | 仅按变量名检查 `EDHR_FULL_E2E_*` 是否存在，未读取或输出变量值 | FAIL：required 27，present 0，missing 27 | 七账号、任务 fixture、四附件、租户、本地 URL、写入确认、灭菌批号和签核证据均未提供。 |

## E2E Prerequisite Inventory

以下仅记录缺失变量名，不记录任何秘密值：

```text
EDHR_FULL_E2E_BASE_URL
EDHR_FULL_E2E_CLEANUP_PLAN_REFERENCE
EDHR_FULL_E2E_CONFIRM_WRITES
EDHR_FULL_E2E_FINISHED_OWNER_PASSWORD
EDHR_FULL_E2E_FINISHED_OWNER_USERNAME
EDHR_FULL_E2E_FINISHED_RECORD_PATH
EDHR_FULL_E2E_FINISHED_REPORT_PATH
EDHR_FULL_E2E_INCOMING_OWNER_PASSWORD
EDHR_FULL_E2E_INCOMING_OWNER_USERNAME
EDHR_FULL_E2E_INCOMING_REPORT_PATH
EDHR_FULL_E2E_INCOMPLETE_ACTIVE_ORDER_ID
EDHR_FULL_E2E_INCOMPLETE_WORK_ORDER_CODE
EDHR_FULL_E2E_MAIN_ACTIVE_ORDER_ID
EDHR_FULL_E2E_MAIN_WORK_ORDER_CODE
EDHR_FULL_E2E_MANAGER_PASSWORD
EDHR_FULL_E2E_MANAGER_SIGNOFF_EVIDENCE_HASH
EDHR_FULL_E2E_MANAGER_USERNAME
EDHR_FULL_E2E_OUTSIDER_PASSWORD
EDHR_FULL_E2E_OUTSIDER_USERNAME
EDHR_FULL_E2E_PQC_PASSWORD
EDHR_FULL_E2E_PQC_USERNAME
EDHR_FULL_E2E_REJECT_ACTIVE_ORDER_ID
EDHR_FULL_E2E_REJECT_WORK_ORDER_CODE
EDHR_FULL_E2E_STERILIZATION_BATCH_NO
EDHR_FULL_E2E_STERILIZATION_OWNER_PASSWORD
EDHR_FULL_E2E_STERILIZATION_OWNER_USERNAME
EDHR_FULL_E2E_STERILIZATION_REPORT_PATH
EDHR_FULL_E2E_TEAM_LEADER_PASSWORD
EDHR_FULL_E2E_TEAM_LEADER_USERNAME
EDHR_FULL_E2E_TENANT
```

## P11 Verdict

- 当前 `int_main` 规格更新已独立通过 Playwright 解析、SP-1 至 SP-4 named contracts、TypeScript、Prettier、差异检查、风险词和入口静态审计。
- 历史 worktree 的 v4 runtime guard、独立静态回归、SP-1 至 SP-4 命名合同、Playwright 规格解析和 T11 格式门禁也均有通过记录。
- 真实七账号页面链、非候选反向权限、两条任务自有活动订单、四次真实附件上传、管理者最终签核、双条件追溯和任务数据清理均未执行。
- `--list`、静态合同、Maven、SQL 或 API 证据不能替代真实 Playwright 页面 E2E。
- P11 verdict：`BLOCKED`。

## Remaining Blockers

1. 提供 30 项正式 E2E 前置，包括七个互相独立的测试账号、目标租户、本地 URL、明确写入确认、主链/拒绝链/未完成链三组任务自有 fixture、四个真实附件、灭菌批号、管理者签核 SHA-256 证据及 cleanup plan reference。
2. 前置齐备并经运行态来源门禁确认后，以当前 `int_main` 代码启动成对本地运行态，执行完整 Playwright 用例，并核验七账号正反权限、未完成进度零写入、四附件、最终放行、只读最终状态、双条件追溯和任务数据清理。

## Final Verdict

- Outcome: `BLOCKED`
- 原因：当前 `int_main` 规格更新的实现级和静态门禁均有通过证据，但 30 项真实 E2E 前置全部缺失，尚未进行真实本地运行态、多账号、附件、三组任务 fixture 和清理验收，因此不得标记 P11/T11、整项任务或生产放行为完成。
