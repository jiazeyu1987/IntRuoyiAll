# 主干提交范围甄别

## Task Goal
按用户“提交推送主干代码”“你来甄别，然后进行合适的处理”的授权，识别不合格评审与并行任务改动归属，确认可提交版本并安全推送 int_main。

## Milestones
- M1 completed：核实 Git、并行任务及冲突归属。
- M2 completed：形成文件分类与提交条件，保留并行工作。
- M3 in_progress：按用户再次要求提交业务快照并推送，保留未完成验证说明。

## Expected Verification
- Git 未合并项为零；提交文件归属与依赖完整。
- 当前源码编译及对应任务验收通过，不能以旧字节码结果替代。
- 提交前核对暂存清单及差异；推送后核对远端 SHA。
- 本轮未请求 E2E，不执行 E2E；既有 NCR E2E 未完成状态仍保留。

## Current Status
in_progress：用户再次明确要求提交推送，执行业务代码快照；业务验证未完成。

## 设计约束检查
- 本轮已授权提交推送；授权不表示验收通过。
- 不覆盖、stash、撤销并行文件；仅将用户再次要求提交时的业务内容保存为快照。
- 不全量暂存；AOCI资产、临时产物、无关演示与备份不纳入本任务。
- 不改生产代码，因此不新增业务BDD/TDD；本任务为集成范围核验。
- 不把本次甄别完成等同业务交付完成。

## Cleanup Keep
- doc/tasks/20260928-ncr-main-submit-triage/file-classification.tsv