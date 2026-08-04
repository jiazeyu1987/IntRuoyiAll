# Bug Regression Evidence

## Bug Summary

自动重排当前遇到任一阻断 issue 时会中止整批应用，导致没有问题的工单也无法重排；用户期望只有存在阻断的工单标红并显示原因，其它工单继续重排。

## Expected Behavior

- 可归因到单个工单的 BLOCKING issue 只阻断该工单。
- 没有阻断的工单继续按自动重排结果应用。
- 阻断工单写入并展示阻断原因。
- 全局或无法归因阻断仍 fail fast。

## Reproduction

- Pending RED command.

## Root Cause

- Pending.

## Regression Test

- Pending.

## RED

- Pending.

## GREEN

- Pending.

## Risk And Regression Scope

- 排产应用删除/重建任务范围必须收敛到可应用工单，避免删除阻断工单已有任务。
- issue 持久化必须刷新本次涉及工单的阻断原因，避免旧问题误导 UI。
- 日历 token、全局 preflight 和无法归因阻断仍必须保持 fail-fast。

## Blockers And Follow-Up

- Pending.
