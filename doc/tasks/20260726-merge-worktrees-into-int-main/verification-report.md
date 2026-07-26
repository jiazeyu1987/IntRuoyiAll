# Verification Report

## Scope

将 `D:\IntRuoyiWorktree\` 下六个已登记 worktree 分支正式合入 `int_main`，验证分支历史、目标行为和工作区状态，并在推送后安全删除对应 worktree。

## Merge Result

- `codex/edhr-personal-console-open-task-status` -> merge `7a6992dc`
- `codex/batch-record-v14-cell-rule-runtime` -> merge `d1a35804`
- `codex/edhr-release-dossier-e2e-20260726` -> merge `98ebfefb`
- `codex/route-start-batch-record-attachments-e2e` -> merge `c66747ab`
- `codex/work-order-field-cell-link-20260726` -> merge `b71d15cd`
- `codex/codex-test-run-monitor-runtime` -> merge `0f2b7442`

六个目标分支 tip 均已通过 `git merge-base --is-ancestor <branch> int_main`，六个 worktree 在删除前均为 clean。

## Verification

- MES 聚焦合并回归：75 tests passed。
- System Codex 聚焦合并回归：15 tests passed。
- Frontend `pnpm ts:check`：PASS。
- Batch record cell-link 静态合同与真实 E2E 脚本语法检查：PASS。
- Route START attachment、END release owner、release owner candidate 静态合同：PASS。
- Codex 测试管理静态合同：PASS。
- `git diff --check`：PASS。
- Branch runtime port guard：PASS。

## Residual Risk

一次扩大到完整 `MesProEdhrBatchExecutionServiceTest` 与 `MesProEdhrWorkTaskServiceImplTest` 的 271 项回归出现两个既有失败：放行待审批旧动作断言不一致，以及旧测试未设置登录用户。`4339e31e..HEAD` 的六个目标分支未修改第一项对应代码，第二项目标分支仅新增终态待办过滤测试和查询 Mapper 改动，未修改失败路径。目标行为的 75 项 MES 聚焦回归全部通过，本任务不扩大范围修复这两个既有测试问题。

## Pending Closeout

- 六个 worktree 已删除，Git 注册与物理目录均已复核不存在。
- 六个端口登记项已标记 `active=false`，并记录删除时间和收尾任务。
- 本次测试生成的 `resources/批记录压力泵.doc` 已清理。
- cleanup preview/apply 已通过，仅删除本任务额外证据文件，核心任务记录保留。
- 并发 eDHR 脏改动已独立保存为 baseline `fc603595`，未混入本任务 closeout。
- closeout commit `82634b11` 已推送到 `origin/int_main`，推送后分支不再 ahead。
- 最终 Git worktree、物理目录、目标端口和端口登记表复核通过；任务状态 `completed`。
