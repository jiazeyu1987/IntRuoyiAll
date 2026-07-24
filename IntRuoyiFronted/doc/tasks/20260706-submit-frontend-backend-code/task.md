# 提交前后端代码任务

## 任务目标

检查前端 `yudao-ui-admin-vue3` 与后端 `ruoyi-vue-pro` 当前待提交内容，只提交本次可归属、已验证、非临时产物的前后端代码或测试文件。

## 里程碑

1. 读取 PowerShell 经验并检查前后端 Git 状态。
2. 区分可提交代码、任务证据、临时日志和失败项。
3. 执行提交前验证。
4. 分仓库提交可放行改动；阻塞不可放行改动。

## 预期验证

- 前端新增 E2E 脚本通过 `node --check` 语法验证。
- 后端若存在生产代码改动，必须有对应 PASS 证据后才提交。
- 不提交临时日志、截图输出目录或失败评审候选。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，本任务仅做提交归属和放行检查，不改业务逻辑。
- 是否存在临时补丁或绕过：否。

## 经验门禁

- PowerShell 命令先读取 `docs/powershell-memory.md`，并显式设置 UTF-8 输入输出。
- 不使用 `&&` 串联命令。
- 验证失败或任务状态未完成的内容不得硬提交。

## 当前状态

- 状态：已完成。
- 已确认后端 `20260705-batch-record-layout-ratio-branch-review` 记录最终评审 FAIL，暂无可合入后端代码，本轮不提交后端。
- 已确认前端新增 `tests/e2e/mes-direct-work-report-import-real-flow.e2e.js` 通过语法验证。

## 最终验证

- GREEN: frontend-e2e-syntax -> PASS，`node --check tests/e2e/mes-direct-work-report-import-real-flow.e2e.js` 通过。
- BLOCKER: backend-commit -> 后端待提交内容包含失败评审任务证据和 REVIEW ONLY SQL 草案，不满足提交前置条件。
