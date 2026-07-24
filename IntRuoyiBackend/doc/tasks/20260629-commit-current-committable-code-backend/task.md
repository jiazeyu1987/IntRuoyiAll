# 任务：提交当前可提交的后端代码

## 任务目标

- 在 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 中筛出截至 2026-06-29 已完成、具备验证证据、且不与进行中任务混杂的后端改动。
- 仅提交当前“能安全提交”的后端代码；对混入 `in_progress` / `blocked` 任务的文件只做 hunk 级提交或暂不提交。
- 为每次后端 commit 提供明确的 TDD 证据与任务归属，满足仓库 pre-commit 门禁。

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-local-quartz-smoke-enable\task.md`
- 状态：`in_progress`
- 处理说明：该任务仍有运行态验证未收口；本次只提交已经闭环的其它任务代码，不把 Quartz 相关进行中改动带入提交。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 本次命中 PowerShell / 中文编码与 worktree 提交边界门禁。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - PowerShell 5.1 输出与台账读写统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
  - 同文件混入其他任务 hunk 时，不得整文件暂存。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；只提交已经闭环的正式代码，不以“一把全提”掩盖未完成事项。
- `是否存在临时补丁或绕过`：否；不绕过 pre-commit TDD 门禁，不把混杂文件整文件强提。

## 里程碑

1. M1：建立后端提交收口任务并确认可提交候选集。`completed`
2. M2：提交 DCC 识别账本 / 文件级认领 / 元数据导入导出闭环。`completed`
3. M3：提交后端菜单文案、SRM/NAS、角色权限等已闭环改动。`completed`
4. M4：记录剩余未提交后端改动与原因。`completed`

## 预期验证

- `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro diff --cached --check`
- `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro diff --cached --name-only`
- 每批待提交代码都必须能回溯到已完成任务的 RED/GREEN 证据。

## 最终验证结果

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_dcc_subtab_four_char_menu_sql.py -q` -> PASS
- `python -X utf8` 定向读取 `sql/mysql/20260515_dcc_governance_split_menu.sql`、`script/e2e/dcc_screenshot_navigation_e2e.py`、`script/e2e/dcc_approval_print_template_r12_e2e.py` 中的残留 DCC 文案 -> PASS
- `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro diff --cached --check` -> PASS

## 当前状态

- `completed`

## 当前阻塞

- 无新的提交阻塞；剩余未提交改动均为 `in_progress` / `blocked` 任务，或与进行中需求混杂，按规则继续留在工作区。
