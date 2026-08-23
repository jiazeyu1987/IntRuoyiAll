# 主线 Dirty/Untracked 分类与收口

## Task Goal

盘点 `E:/IntRuoyi` 当前主线工作树的 tracked dirty 与 untracked 文件，按主干代码、并行任务成果、任务文档、生成物/运行产物和未知归属分类；只对归属明确且允许收口的内容选择性提交，对确认属于生成物的内容补充忽略规则，避免删除或覆盖并行任务成果。

## Milestones

| Milestone | 内容 | 状态 |
| --- | --- | --- |
| M1 | 读取主仓库规则并冻结 dirty 基线 | completed |
| M2 | 按路径、任务目录和文件类型完成分类并核对归属 | in_progress |
| M3 | 选择性提交明确归属内容并补充必要 ignore | pending |
| M4 | 复核主线状态、剩余文件和提交清单 | pending |

## Expected Verification

- 记录 baseline 的 tracked modified、deleted 和 untracked 数量。
- 每个大类有路径清单、归属判断和处置结论。
- 不使用 `reset`、`checkout`、`clean`、整体删除或整体 `git add -A`。
- 提交前核对 staged 文件清单和 `git diff --cached --check`。
- 提交后复核 `git status --short --branch`、主线 HEAD、剩余 dirty/untracked 数量和 ignore 命中结果。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，按归属和生成物来源分类，而不是用一条宽泛 ignore 隐藏未知文件。
- 是否存在临时补丁或绕过：否。

## Current Status

in_progress
