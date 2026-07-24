# 任务：提交当前可提交的前端代码

## 任务目标

- 在 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 中筛出截至 2026-06-29 已完成、具备验证证据、且不与进行中任务混杂的前端改动。
- 仅提交当前“能安全提交”的前端代码；将排产冒烟、奖项生图等仍处于 `in_progress` / `blocked` 的改动继续留在工作区。
- 为前端每次提交保留清晰的任务归属、验证证据与剩余未提交范围说明。

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-showroom-award-generate-cover-version\task.md`
- 状态：`in_progress`
- 处理说明：奖项生图功能仍待真实页面验收，本次只提交已经闭环的其它前端任务，不把该进行中改动带入提交。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 本次命中 PowerShell/中文编码与提交边界门禁。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - PowerShell 5.1 输出与台账读写统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
  - 提交判断以前端仓库自身为准；若同文件混入其它任务 hunk，不做整文件强提。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；只提交已完成且已验证的正式前端代码，不以临时批量提交掩盖进行中需求。
- `是否存在临时补丁或绕过`：否。

## 里程碑

1. M1：建立前端提交收口任务并确认可提交候选集。`completed`
2. M2：提交菜单文案、NAS 进度展示、Word 导入入口等已完成改动。`completed`
3. M3：视边界情况继续提交其它已闭环前端改动。`completed`
4. M4：记录剩余未提交前端改动与原因。`completed`

## 预期验证

- `git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 diff --cached --check`
- `git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 diff --cached --name-only`
- 每批待提交代码都必须能回溯到已完成任务的 RED/GREEN 证据。

## 最终验证结果

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\electronic-batch-record-master-detail-layout-static.spec.js` -> PASS
- `python -X utf8` 定向校验 `tests/e2e/srm/*.spec.*` 与 `tests/e2e/srm/supplier-access-risk-real-flow.e2e.js` 中旧菜单标题 -> PASS
- `git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 diff --cached --check` -> PASS

## 当前状态

- `completed`

## 当前阻塞

- 无新的提交阻塞；剩余前端改动要么仍处于 `in_progress` / `blocked`，要么尚未补齐独立任务台账与验证证据，因此继续保留在工作区。
