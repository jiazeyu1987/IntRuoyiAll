# Execution Log

## User Intent

用户要求将 `D:\IntRuoyiWorktree\` 下的 worktree 融合进 `int_main`，然后删除 worktree。

## Initial Command Intent

- 读取 worktree、任务收尾、PowerShell/Git 编排和编码规则。
- 检查主工作区、各 worktree、分支、remote、脏状态和合并关系。
- 在确认所有目标与风险后执行合并、验证、推送和 worktree 删除。

## BDD Scenarios

BDD: 所有目标分支的合并状态可追溯 -> Given `int_main` 与 `D:\IntRuoyiWorktree\` 下 worktree 均可由 Git 识别；When 逐项检查分支祖先关系、脏状态和合并结果；Then 每个分支都有明确的已合入、无需合入或阻塞结论。

BDD: 合并完成后 worktree 被安全删除 -> Given 目标分支已处理且不存在未保存改动；When 删除对应 Git worktree；Then Git worktree 清单与文件系统目录均不再包含该 worktree，且未删除 `E:\IntRuoyi` 主工作区。

## Milestone 1

Status: in_progress

### Rules Read

- `docs/worktree-restrictions.md`
- `docs/task-closeout-rules.md`
- `docs/powershell-memory.md`
- `docs/powershell-encoding.md`

### Initial Evidence

- 主工作区：`E:\IntRuoyi`
- 主分支：`int_main`
- 远端：`origin` 已配置
- 主工作区初始状态：存在大量 tracked、staged、unstaged 和 untracked 改动；`int_main` 初始状态为 `[ahead 20]`
- `docs/experience-index.md` 存在。
- 匹配经验已读取：`docs/worktree-memory.md` 的 Worktree 删除门禁、`docs/release-build-preflight-lessons.md` 的 closeout 状态与物理目录复核门禁。

GREEN: experience-preflight -> PASS，已确认路径边界、dirty 变更保存、合入祖先关系、Git 注册/物理目录/端口登记三重验证及 `ready_for_closeout` 状态要求。

### Dirty Workspace Baseline

- `git fetch origin` -> PASS；`origin/int_main` 无新增提交，其他远端分支引用已刷新。
- `scripts/preflight/branch-runtime-port-guard.ps1` -> PASS。
- staged 敏感文件名检查 -> PASS，未命中密码、token、私钥或环境密钥文件名。
- staged 大文件检查 -> PASS，最大新增未跟踪文件约 14 KB。
- `git diff --cached --check` -> WARN，既有 `doc/tasks/20260726-route-flow-add-form-click-count/bug-regression-evidence.md` 报告 EOF 多余空行；该文件按脏工作区基线原样保存，未修改并发任务内容。
- 基线提交：`4339e31e`（`chore: baseline workspace before worktree consolidation`）。
- 基线提交文件数：112；包含任务开始前主工作区全部 tracked、staged、unstaged 和 untracked 改动，不包含本任务目录 `doc/tasks/20260726-merge-worktrees-into-int-main/`。
- 基线提交后：`int_main...origin/int_main [ahead 21]`，主工作区仅剩本任务目录未跟踪。

### Dirty Worktree Verification

- `batch-record-v14-cell-rule-runtime`
  - `git diff --check` -> PASS。
  - `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellRuleSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，29 tests。
- `edhr-release-dossier-e2e-20260726`
  - `git diff --check` -> PASS。
  - `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，33 tests。
- `route-start-batch-record-attachments-e2e`
  - `git diff --check` -> PASS。
  - `node IntRuoyiFronted/tests/e2e/mes-route-flow-start-batch-record-attachments-static.spec.js` -> PASS。
  - `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProRouteBatchRecordAttachmentOwnerServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests。
- `codex-test-run-monitor-runtime`
  - RED: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestRunnerServiceImplTest,CodexTestRunnerControllerTest,CodexTestExecutionServiceImplTest,CodexTestCaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，12 tests 中 6 errors；共享测试夹具缺少正式 `project` 值。
  - 修复：`CodexTestCaseServiceImplTest.buildCaseReq` 设置 `project = 智能排产`。
  - GREEN: 同一 Maven 命令 -> PASS，12 tests / 0 failures / 0 errors。

### Port Registry Recovery

- `batch-record-v14-cell-rule-runtime` 与 `codex-test-run-monitor-runtime` 缺少 `D:\IntRuoyiWorktree\.ports\worktree-ports.json` 登记，首次提交前 guard 按规则失败。
- 已确认 `int_main` profile 的旧 slot 1、2 对应目录均已删除且登记项为 inactive/deleted，可正式复用。
- 新登记：
  - `batch-record-v14-cell-rule-runtime` -> slot 1，frontend `8082`，backend `48082`。
  - `codex-test-run-monitor-runtime` -> slot 2，frontend `8083`，backend `48083`。
- 登记表 UTF-8 JSON 解析 -> PASS；两个 worktree 的 branch runtime port guard -> PASS。
- 本任务未启动上述端口服务。

### Preserved Worktree Commits

- `codex/batch-record-v14-cell-rule-runtime` -> `cdcabeee`，3 files。
- `codex/codex-test-run-monitor-runtime` -> `2968ee48`，24 files。
- `codex/edhr-release-dossier-e2e-20260726` -> `2f8cf5cf`，1 file。
- `codex/route-start-batch-record-attachments-e2e` -> `192e3973`，4 files。
- 四个提交前均检查 staged 文件清单、`git diff --cached --check` 和 branch runtime port guard。

### Merge Progress And Integration RED/GREEN

- `codex/edhr-personal-console-open-task-status` -> merged。
- `codex/batch-record-v14-cell-rule-runtime` -> merged。
- `codex/edhr-release-dossier-e2e-20260726` -> merged。
- `codex/route-start-batch-record-attachments-e2e` -> merged。
- `codex/work-order-field-cell-link-20260726` -> 发生两个文本冲突：
  - `MesProRouteFlowConfigServiceImpl.java` 仅为同一方法调用的换行格式冲突，保留主线格式。
  - `docs/experience-index.md` 合并双方三条有效门禁索引。
- RED: work-order 合并态 Maven 目标验证 -> FAIL，通用 `parseCandidateSourceNames` 被独立附件解析变更误删，仍有两处正式调用。
- RED: `pnpm ts:check` -> FAIL，`flowconfig.ts` 三组附件 API 属性重复。
- 修复：恢复通用候选名称解析方法，同时保留附件专用解析；移除第二组重复 API 定义。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordCellLinkSchemaTest,MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，38 tests。
- GREEN: `node tests/e2e/mes/batch-record-cell-link-static.spec.js`、`node --check tests/e2e/mes/batch-record-cell-link-work-order-field-readonly.e2e.mjs`、`pnpm ts:check` -> PASS。

## Completed Merge Commits

- `codex/edhr-personal-console-open-task-status` -> `7a6992dc`
- `codex/batch-record-v14-cell-rule-runtime` -> `d1a35804`
- `codex/edhr-release-dossier-e2e-20260726` -> `98ebfefb`
- `codex/route-start-batch-record-attachments-e2e` -> `c66747ab`
- `codex/work-order-field-cell-link-20260726` -> `b71d15cd`
- `codex/codex-test-run-monitor-runtime` -> `0f2b7442`

### Codex Merge Resolution

- 冲突仅涉及 `CodexTestRunnerControllerTest` 与 `CodexTestRunnerServiceImplTest`。
- 保留较新的 Runner progress/status 回归覆盖，并完成 merge commit `0f2b7442`。
- 提交前 `git diff --cached --check` 与 `scripts/preflight/branch-runtime-port-guard.ps1` 均通过。

## Final Integration Verification

- GREEN: 六个目标分支逐项执行 `git merge-base --is-ancestor <branch> int_main` -> PASS。
- GREEN: 六个 worktree 逐项执行 `git status --short --branch` -> clean。
- REGRESSION: 宽范围 MES 类级回归执行 271 tests，出现 1 failure + 1 error：
  - `MesProEdhrBatchExecutionServiceTest#get_releasePendingApproval_locksNormalTaskActions` 期望空动作，实际为 `[OPEN_FORM, SAVE_FORM, SUBMIT]`。
  - `MesProEdhrWorkTaskServiceImplTest#completeRouteFormFillAndCreateNextFill_marksFormCenterFillDoneAndCreatesNextFill` 未设置登录用户，抛出 `账号未登录`。
- Scope analysis: `4339e31e..HEAD` 未修改第一项对应生产代码/测试；第二项对应旧失败测试与生产服务未被目标分支修改，目标 eDHR 分支仅新增终态待办过滤测试并修改查询 Mapper。两项作为既有宽回归残余风险记录，不扩大本次 worktree 融合范围修改。
- GREEN: 目标 MES 聚焦回归 -> PASS，75 tests / 0 failures / 0 errors。
- GREEN: Codex System 聚焦回归 -> PASS，15 tests / 0 failures / 0 errors。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: batch-record cell-link 静态合同与真实 E2E 脚本语法检查 -> PASS。
- GREEN: route START attachment、END release owner、release owner candidate 静态合同 -> PASS。
- GREEN: Codex 测试管理静态合同 -> PASS。
- `resources/批记录压力泵.doc` 由本次宽 MES 测试运行生成，登记为本任务临时测试产物，待 closeout 清理。
- GREEN: project-experience-consolidation -> PASS，已将多 worktree dirty 保存、逐分支 merge、ancestor 验证和宽/聚焦回归分层门禁合并到 `docs/worktree-memory.md`，并更新 `docs/experience-index.md` 路由。

## Current Remaining Work

- 运行项目经验沉淀，提交任务验证记录并推送 `int_main`。
- 确认目标路径无进程引用后删除六个 worktree，执行 `git worktree prune`。
- 更新端口登记表为 inactive/deleted，完成三重删除复核。
- 执行 task-closeout preview/apply，标记任务完成并最终推送。
