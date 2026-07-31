# 创建批次执行 batchCode 单元格链接来源缺失修复

## Task Goal

修复创建批次执行时报错 `批记录单元格链接自动落库缺少来源值：executionId=32，ruleId=16，sourceField=batchCode，targetCell=4:1` 的后端根因，确保已配置的 `PRODUCTION_WORK_ORDER.batchCode` 单元格链接在创建/打开批记录执行记录时能够从正式来源读取并自动落库；来源真实缺失时继续 fail fast。

## Milestones

- [x] 建立任务记录并保全既有脏工作区基线。
- [x] 定位 `sourceField=batchCode` 在批次执行创建链路中的来源读取与落库边界。
- [x] 编写先失败的后端回归测试，覆盖创建批次执行时批号来源应可落库。
- [x] 实施最小正式修复，不引入 fallback、默认值或吞异常。
- [x] 运行目标测试和相关回归验证。
- [x] 完成任务收尾、经验沉淀、提交与推送。

## Expected Verification

- RED：目标后端回归测试在修复前因 `batchCode` 来源缺失或未落库失败。
- GREEN：目标后端回归测试修复后通过。
- REGRESSION：运行受影响 MES 模块定向 Maven 测试；如涉及静态契约，同步运行对应 Node/Python 验证。
- 证据：记录错误复现、根因、测试命令、提交 hash、推送状态。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是修复创建/打开执行记录的正式来源读取和字段审计落库链路。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 命中 `docs/experience-index.md`：eDHR 批记录单元格链接预填落库、`PRODUCTION_WORK_ORDER.batchCode`、字段审计链、创建/打开执行记录自动落库、来源缺失 fail-fast。
- 执行约束：禁止以前端 `/prefill`、`hydrateDraftState`、空值兜底、直接 SQL 回填或绕过字段审计链代替后端创建/打开执行记录落库修复。
- 已沉淀：`docs/backend-development.md#批记录单元格链接预填落库边界` 增补 `PRODUCTION_WORK_ORDER.batchCode` 必须读取执行上下文 `mes_pro_batch_record_execution.batch_code` 的来源边界；`docs/experience-index.md` 已补索引关键词。

## Final Verification Result

- RED/GREEN/REGRESSION 均已完成并通过最终回归。
- Cleanup preview/apply 已完成，未删除任何文件。
- Commit/push 证据见 `execution-log.md`。

## Cleanup Keep

- doc/tasks/20260728-batch-cell-link-batchcode-source/bug-regression-evidence.md
