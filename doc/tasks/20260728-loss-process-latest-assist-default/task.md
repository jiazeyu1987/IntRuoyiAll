# 损耗单和过程检验记录最新版本默认辅助表单

## Task Goal

给“损耗单”和“过程检验记录”的最新版本表单各创建一个默认辅助表单，且各只有一个填写人，方便后续手动测试和调整。

## Milestones

- [x] 核对最新版本表单的数据来源和 schema。
- [x] 编写只作用于目标表单的初始化脚本，先 dry-run/verify。
- [x] 写入默认辅助表单配置。
- [x] 复验目标表单均存在 1 个默认填写人的辅助表单映射。
- [x] 更新任务记录和验证报告。

## Expected Verification

- 只读查询确认目标表单最新版本。
- 初始化脚本 `--dry-run` 输出目标与预计映射数量。
- 初始化脚本执行写入。
- 初始化脚本 `--verify` 确认每个目标都有 1 个填写人和辅助映射。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务是一次性正式数据初始化，后续仍走手动配置。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- `doc/tasks/20260728-loss-process-latest-assist-default/initialize_latest_template_assist_default.py`
- `doc/tasks/20260728-loss-process-latest-assist-default/output/latest-template-assist-default-backup-20260728220606.json`
