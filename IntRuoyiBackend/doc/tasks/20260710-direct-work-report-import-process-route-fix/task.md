# 直接报工导入工艺工序匹配修复

## Task Goal

修复导入 `李萍.xlsx` 时，排产快照旧工序 ID 与当前路线工序新 ID 不一致导致的工艺工序配置误判。

## Milestones

1. 解析真实 Excel 并只读核对真实数据关系。
2. 新增失败回归测试。
3. 实现快照工序规范化。
4. 执行服务与导入主路径回归。
5. 提交实现并完成收尾。

## Expected Verification

- 目标测试修复前抛出 `1040506008`，修复后通过。
- `MesProFeedbackServiceImplTest` 全量通过。
- 直接报工导入主路径测试通过。
- `git diff --check` 通过。

## 经验门禁

- PowerShell 和中文文件显式 UTF-8，不使用 `&&`。
- 数据库仅执行 `SHOW TABLES` / `DESCRIBE` 后的参数化只读查询。
- 不修改芋道源码租户数据，不用 mock 或默认工序掩盖配置错误。
- 先 RED、后最小 GREEN、再回归。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；以排产快照 `route_process_id` 作为当前路线工序的稳定身份。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

实现提交 `3abf4cec5b` 已完成；收尾预览和应用通过，仅保留 `task.md`、`execution-log.md` 与 `verification-report.md`。

## Final Verification

- 目标 RED/GREEN、报工服务 15/15、直接导入主路径 1/1、`git diff --check` 全部通过。
- Bug regression evidence validator 通过，临时证据文件已按收尾规则清理。
- 未修改数据库、租户数据、服务器或前端。
