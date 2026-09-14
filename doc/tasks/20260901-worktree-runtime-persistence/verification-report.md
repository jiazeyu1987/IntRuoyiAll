# 验证报告：Worktree 运行态保留规则

## Result

PASS

## Verification Evidence

- 目标基线在根 `AGENTS.md` 中唯一出现。
- `git diff --check -- AGENTS.md` 通过。
- 现有 `AGENTS.md` 未提交内容未被覆盖或改写。
- cleanup preview/apply 通过，没有删除文件、停止服务或执行 Git 集成。
- 提交 `fa019209e` 已进入 `int_main`，只包含新增基线和本任务三份记录；其它 `AGENTS.md` 改动未被暂存。
- 最终 cleanup preview/apply 通过，没有删除文件、停止/重启服务或释放端口登记。

## Business Impact

- 后续 Agent 在未取得用户当轮明确授权时，不得停止、重启或释放 worktree 前后端。
- 端口冲突只允许报告，不得强杀；任务收尾默认保留运行态并报告端口和健康状态。
