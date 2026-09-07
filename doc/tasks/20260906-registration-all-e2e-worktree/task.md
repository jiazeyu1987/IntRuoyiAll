# 注册证全量 E2E Worktree 验证

## Task Goal

根据 `E:\IntRuoyi\e2e_test\registration` 下全部注册证 E2E 验收文档，在专用 worktree 中完成真实 Playwright E2E 验证；逐个记录每个 E2E 用例结果；第一轮仅分析失败原因不修改业务代码；汇总完成后集中修复，再重复 E2E 验证和集中修复，直到全部可验证用例通过或明确 SKIPPED/BLOCKED 原因。

## Milestones

- [x] 创建专用 worktree 并预约独立运行端口。
- [x] 解析全部注册证 E2E 文档，形成用例矩阵。
- [x] 启动/确认 worktree 前后端运行态。
- [x] 第一轮执行全部 E2E，用例级记录 PASS/FAIL/SKIPPED/BLOCKED。
- [x] 对失败用例做代码级原因分析，第一轮不改代码。
- [x] 集中修复失败原因。
- [x] 重复执行 E2E 验证和集中修复，直到成功。
- [x] 完成收尾记录与验证报告。

## Expected Verification

- Playwright 真实页面操作覆盖验收文档中所有 E2E 小节。
- 每个 E2E 用例都有独立结果、证据路径、失败原因或跳过条件记录。
- 失败后先完成全量分析，再集中修改。
- 修复后重复一轮全量 E2E，并根据结果继续集中修复，直到全部通过或按文档条件标记 SKIPPED/BLOCKED。

## Current Status

completed

Round 27 completed on 2026-09-07 with all 15 executable registration E2E checks passing in the dedicated worktree. Supplemental reminder document verification also passed for notification settings, threshold delivery, recipient reconfiguration, and config restore. The verified changes were fused into `int_main` in commit `acfcd7a39`.

## Runtime Allocation

- Worktree: `D:\IntRuoyiWorktree\20260906-registration-all-e2e-worktree`
- Branch: `codex/20260906-registration-all-e2e-worktree`
- Profile: `int_main`
- Slot: `24`
- Frontend port: `8158`
- Backend port: `48158`

## 设计约束检查

- 使用 Playwright 操作真实前端页面；API/DB 仅允许最终只读核验。
- 不在第一轮失败分析期间修改业务代码。
- 不使用 mock、fallback、接口直调或 SQL 写入替代被验收业务动作。
- 用户已授权可重启当前 worktree 后端；不得影响 `E:\IntRuoyi` 主工作区 `8081/48081`。
- 未获得提交/推送授权，本任务不执行 Git commit/push。
