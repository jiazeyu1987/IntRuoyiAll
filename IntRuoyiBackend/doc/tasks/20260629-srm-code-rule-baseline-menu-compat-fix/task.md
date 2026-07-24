# 任务：修复 SRM 编码规则基线菜单兼容以放行测试服发布

- Task ID: `20260629-srm-code-rule-baseline-menu-compat-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `in_progress`

## Task Goal

修复 `sql/mysql/20260618_srm_d7_1_code_rule_baseline.sql` 的菜单兼容契约，使测试服已存在旧标题 `供应商关系管理` 的 `991000` 菜单时，required SQL 仍能就地升级到 `SRM`，从而继续完成“已提交 git HEAD 发布到测试服务器”的真实 `publish-test`。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-security-starter-test-deps-release-fix\task.md`
- 状态：`completed`
- 处理说明：安全模块测试依赖修复已提交并通过验证，无遗留阻塞；当前进入新的 SQL 契约修复任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 PowerShell / 发布 / worktree 相关门禁，先核对后再改 SQL。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文文档与命令输出保持显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
  - 只提交本次 SQL 契约修复直接相关文件，不混入其他进行中候选改动。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。让 D7-1 baseline SQL 能兼容测试服真实旧菜单标题并在同一 migration 内完成标准化，而不是手工改库或跳过 required SQL。
- 是否存在临时补丁或绕过：否。

## Milestones

- M1: 建立任务文档并定位测试服真实冲突数据。状态：completed。
- M2: 先写失败验证，证明现有 SQL 要求 `991000.name='SRM'` 才能通过。状态：completed。
- M3: 最小修复 D7-1 baseline SQL 与配套测试，使旧标题可就地升级。状态：completed。
- M4: 提交本次 SQL 契约修复并回到真实 `build-release -> publish-test`。状态：in_progress。

## Expected Verification

- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_phase1_schema_sql.py`
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_d7_d10_sql_contract.py`
- `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro diff --cached --check`

## Current Blockers

- 暂无。修复已通过定向测试，待最小提交后重走真实发布链路。
