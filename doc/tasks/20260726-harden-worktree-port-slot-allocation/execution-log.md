# Execution Log

## User Intent

- 用户确认实施端口隔离修复，目标是避免新 worktree 抢占 `int_main_d`、`int_main`、`int_batch`、`int_shedule`、`int_qms` 五组基准端口。

## Baseline

- 任务开始分支：`int_main`。
- 远端：`origin` 已配置。
- 任务开始前存在并行前端改动和其他任务文档；将按项目规则建立独立脏工作区基线提交，不修改其内容。
- GREEN: dirty-worktree baseline -> PASS，提交 `88016be5`（`chore: preserve pre-task dirty baseline`）保存了任务开始前的 3 个前端文件和 4 个其他任务目录，共 12 个文件；本任务目录未混入该提交。

## BDD

- BDD: 附加 worktree 只能使用所属 profile 的隔离端口段 -> Given 任一 runtime profile 和附加 worktree；When 请求 `slot 1..19`；Then 计算端口位于本 profile 的隔离段且不等于任何 profile 基准端口。
- BDD: 越界槽位必须立即失败 -> Given 任一 runtime profile；When 请求 `slot >= 20`；Then 启动上下文解析失败并明确报告允许范围。
- BDD: 登记端口冲突必须立即失败 -> Given worktree 登记项使用保留基准端口或与其他未释放登记项重复；When 解析运行时上下文；Then 启动失败且不得换端口。

## Milestone Log

- M1：完成，任务记录、经验门禁和脏工作区基线均已建立。
- M2：完成，新增 4 个回归场景并获得预期 RED。
- M3：完成，端口契约升级为 `2026-07-26-branch-runtime-v3`，实现 `slot 1..19`、活动登记全局唯一、互斥锁原子分配和基准路径严格解析。
- M4：完成，目标回归、port guard、主工作区与活动 worktree 解析、脚本语法、证据校验和经验索引均通过。
- GREEN: experience-preflight -> PASS，已读取 `docs\experience-index.md` 与命中的 `docs\worktree-memory.md`；确认槽位必须稳定保留、冲突必须 fail fast。

## Verification Evidence

- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py` -> FAIL，8 项中 4 项按预期失败：当前接受 `int_main slot=20 -> 8101/48101`、允许两个活动 worktree 共用 `int_main/2`、允许基准工作区请求 `slot=1`，且缺少 `scripts\runtime\reserve-worktree-slot.ps1`。
- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py` -> FAIL，新增主工作区 profile 断言后共 5 项失败；确认 `E:\IntRuoyi` 因路径末尾匹配缺陷被误判为 `int_main_d 8101/48101`。
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py` -> PASS，11 passed；覆盖越界、重复活动槽位、基准工作区非零槽位、主工作区 profile、最低空闲分配、并发分配和槽位耗尽。
- GREEN: `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，当前主工作区解析为 `int_main 8081/48081`。
- GREEN: `pwsh -NoProfile -File scripts\runtime\show-branch-runtime.ps1` -> PASS，契约版本 `v3`，主工作区 `slot=0`、`8081/48081`。
- GREEN: 从 `D:\IntRuoyiWorktree\edhr-latest-published-form` 调用新版 `show-branch-runtime.ps1` -> PASS，保持 `profile=int_main`、`slot=7`、`8088/48088`。
- GREEN: PowerShell parser -> PASS，`branch-runtime-profile.ps1`、`reserve-worktree-slot.ps1`、`branch-runtime-port-guard.ps1` 均无语法错误。
- RED: bug regression evidence validator -> FAIL，缺少必需的 `Verification` 章节；已按证据契约补充，不改变实现。
- GREEN: bug regression evidence validator -> PASS，证据结构完整。
- RED: PowerShell parser 编排命令 -> FAIL，命令字符串中的 `$target:` 触发 PowerShell 变量插值解析错误；改为 `${target}:` 后复跑，不涉及产品脚本。
- GREEN: PowerShell parser corrected command -> PASS。
- GREEN: project-experience-consolidation -> PASS，新增经验归入既有 `docs\worktree-memory.md`，并更新 `docs\experience-index.md` 路由；未新建长期经验文档。
- CONCURRENCY: 其他任务按脏工作区基线规则创建提交 `14dfbc66`（`chore: save pre-task workspace baseline`），其中包含本任务当时已存在的 v3 契约、测试、规则和任务证据。该提交未包含被 `.gitignore:28` 的 `**/runtime/` 忽略的新文件 `scripts\runtime\reserve-worktree-slot.ps1`；本任务不改写并发提交，将在任务实现提交中使用 `git add -f` 正式纳入该脚本。
- GREEN: `git check-ignore -v scripts\runtime\reserve-worktree-slot.ps1` -> PASS，确认忽略来源为 `.gitignore:28:**/runtime/`，属于合法运行时源码目录误命中。
- RED: cleanup preview -> FAIL policy intent，preview 会删除 `bug-regression-evidence.md`；原因是 `Cleanup Keep` 使用纯路径未被脚本识别，已改为 bullet 路径并重跑。
- RED: cleanup apply -> FAIL，脚本只识别裸文本 `ready_for_closeout`，不识别反引号包裹状态；已修正任务状态格式并重跑。
- GREEN: cleanup preview/apply -> PASS，保留 `task.md`、`execution-log.md`、`verification-report.md`、`bug-regression-evidence.md`，删除项为空。
- GREEN: implementation commit -> PASS，`f7649ac9`（`fix: isolate worktree runtime port slots`）补交被忽略的 `scripts\runtime\reserve-worktree-slot.ps1` 和收尾前证据。
- GREEN: final closeout status -> PASS，任务状态更新为 `completed`，等待最终 closeout commit 和 push。

## Blockers

- 无。
