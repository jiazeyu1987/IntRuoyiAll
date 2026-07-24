# Verification Report: 限制 IntRuoyi worktree 创建目录

## Result

PASS

## Evidence

- `AGENTS.md` 已新增 `Worktree root: D:\IntRuoyiWorktree\`。
- `AGENTS.md` 已明确所有 IntRuoyi task worktrees 只能创建在 `D:\IntRuoyiWorktree\` 下。
- `AGENTS.md` 已明确创建前必须解析绝对目标路径，并验证目标路径是 `D:\IntRuoyiWorktree\` 的子路径。
- `AGENTS.md` 已明确目录外路径必须 fail fast，不得创建 worktree。
- `AGENTS.md` 已明确 `D:\IntRuoyiWorktree\` 缺失或不可写时必须阻塞，不得选择其他目录。
- 已确认并创建 `D:\IntRuoyiWorktree` 目录。
- `python -X utf8` 校验 `AGENTS.md` UTF-8 读取和关键约束均通过。

## Notes

- 本任务未创建 Git worktree、未操作服务器、未执行 E2E、未触碰数据库。
- `task-closeout-cleanup` preview/apply 均通过，无删除项、无阻塞项。
- 当前 `docs\` 下无合适 worktree 长期经验文档；未获用户明确授权，未新建经验文档。
