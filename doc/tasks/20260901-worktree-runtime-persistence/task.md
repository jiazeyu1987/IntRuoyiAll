# Worktree 运行态保留规则

## Task Goal

在项目根 `AGENTS.md` 中加入长期约束，防止 Agent 在 E2E 或任务收尾时擅自停止、重启或释放 worktree 前后端运行态。

## Milestones

- [x] M1：核对现有 worktree、运行态和收尾规则是否冲突。
- [x] M2：写入用户明确批准的长期基线。
- [x] M3：完成结构与差异验证。

## Expected Verification

- `AGENTS.md` 只新增一条运行态保留基线，不覆盖现有未提交内容。
- 基线明确要求用户当轮授权、禁止冲突时强杀，并要求收尾保留运行态和报告健康状态。
- `git diff --check -- AGENTS.md` 通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；将运行态所有权约束固化到项目根规则。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed
