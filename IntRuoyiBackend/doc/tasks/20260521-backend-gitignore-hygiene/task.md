# 任务：整理后端 Git Ignore 规则

## Goal

- 为 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 增加合适的 `.gitignore` 规则。
- 让后端仓库忽略当前明确属于临时目录、缓存目录和一次性任务产物的文件。
- 不误伤真实源码、SQL、正式测试和任务主记录文件。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.gitignore`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-backend-gitignore-hygiene\**`

## Non-Scope

- 不修改任何 Java / SQL / YAML / 脚本业务实现。
- 不删除现有文件，只通过 `.gitignore` 收敛噪声。
- 不忽略 `doc/tasks/**/task.md` 与 `doc/tasks/**/execution-log.md`。
- 不为了“看起来干净”而隐藏真实源码改动。

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-backend-compile-blocker-fix\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact on this task: 无阻塞，可继续推进。

## Milestones

- [x] M1：检查后端仓库当前脏文件来源、现有 `.gitignore` 和上一任务状态。
- [x] M2：写出本次任务文档与 BDD/TDD 验证计划。
- [x] M3：补充 `.gitignore` 规则并验证命中效果。
- [x] M4：更新任务文档、提交本次后端改动并执行收尾预览。

## Expected Verification

- `git status --short`
- `git check-ignore -v .review-fix-loop/runs/20260520T034611Z-1a052a/run.json`
- `git check-ignore -v script/tests/__pycache__/test_dcc_sql_scripts.cpython-312-pytest-8.4.2.pyc`
- `git check-ignore -v yudao-module-showroom/tmp/showroom-cp.txt`
- `git check-ignore -v tmp/showroom-cp.txt`
- `git check-ignore -v doc/tasks/20260516-six-route-report-doc-consistency-review/artifacts/debug-view-page.png`

## Current Status

Completed on 2026-05-21

## Current Findings

- 当前后端工作区的主要噪声来自：
  - `.review-fix-loop/`
  - 多个模块下的 `tmp/`
  - `script/tests/__pycache__/`
  - `doc/tasks/**/artifacts/`
- 当前不计划忽略整份任务目录，也不忽略 `task.md` / `execution-log.md`。
- 已验证真实源码文件 `yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/workflow/service/ShowroomAssignmentService.java` 不会被新规则误忽略。
- 当前 `git status --short` 中仍保留的未跟踪任务目录，实质上是仅包含 `task.md` / `execution-log.md` 的任务主记录，这一行为符合本次设计。

## Final Verification Result

- PASS: `git check-ignore -v .review-fix-loop/runs/20260520T034611Z-1a052a/run.json`
- PASS: `git check-ignore -v script/tests/__pycache__/test_dcc_sql_scripts.cpython-312-pytest-8.4.2.pyc`
- PASS: `git check-ignore -v yudao-module-showroom/tmp/showroom-cp.txt`
- PASS: `git check-ignore -v tmp/showroom-cp.txt`
- PASS: `git check-ignore -v doc/tasks/20260516-six-route-report-doc-consistency-review/artifacts/debug-view-page.png`
- PASS: `git check-ignore yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/workflow/service/ShowroomAssignmentService.java`
- PASS: `python tool/verify_tdd_compliance.py --task-dir D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-backend-gitignore-hygiene`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-backend-gitignore-hygiene --mode preview`

## Closeout Preview

- keep:
  - `doc/tasks/20260521-backend-gitignore-hygiene/task.md`
  - `doc/tasks/20260521-backend-gitignore-hygiene/execution-log.md`
- delete: none
- blocked: none
