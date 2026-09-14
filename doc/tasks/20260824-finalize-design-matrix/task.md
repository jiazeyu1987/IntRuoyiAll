# 岗位需求矩阵变更收口

## Task Goal

按用户明确要求处理 int_main 唯一剩余的已跟踪岗位需求 Excel 变更：不再保留为未决 dirty，选择提交并推送到 origin/int_main。

## Milestones

| Milestone | 内容 | 状态 |
| --- | --- | --- |
| M1 | 核对分支、HEAD、远端和剩余 dirty 文件 | completed |
| M2 | 只暂存岗位需求矩阵并完成 staged 检查 | completed |
| M3 | 提交并推送 int_main | completed |
| M4 | 复核远端和工作树状态 | completed |

## Expected Verification

- 暂存区只包含 design_doc/岗位需求分解矩阵.xlsx。
- 提交前通过 git diff --cached --check。
- 记录 commit、push 结果、远端 HEAD 和最终 git status。
- 不修改、不删除、不忽略其它并行任务实体文件。

## Current Status

completed
