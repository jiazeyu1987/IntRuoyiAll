# 任务：SRM 与文控中心菜单改名（后端）

- Task ID: `20260629-menu-title-srm-dcc-rename`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

同步后端菜单种子与 SQL 契约中的正式菜单名称，将 `供应商关系管理` 改为 `SRM`，将 `DCC文控中心` 改为 `文控中心`。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-mes-work-order-material-demand-warning-clear\task.md`
- 状态：`completed`
- 处理说明：无未完成阻塞项。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`：任务开始前先命中相关经验并摘录。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：中文文档和输出保持 UTF-8。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。统一菜单 SQL 基线与相关契约测试中的正式名称。
- 是否存在临时补丁或绕过：否。

## Milestones

- M1: 建立任务文档与执行日志。状态：completed。
- M2: 更新菜单 SQL 与脚本测试契约。状态：completed。
- M3: 执行定向验证并回填证据。状态：completed。
- M4: 新增测试服可执行的菜单改名 migration。状态：completed。

## Expected Verification

- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_phase1_schema_sql.py`

## Current Blockers

- 暂无。

## Final Verification Result

- `python -X utf8` 定向读取 SQL 标题断言 -> PASS
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-menu-title-srm-dcc-rename\database-schema-evidence.md` -> 待执行
