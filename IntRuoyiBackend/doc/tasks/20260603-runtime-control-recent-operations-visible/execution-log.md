# 执行日志：修复运行控制台最近操作状态目录漂移

BDD: 本机运行控制状态目录重启后保持稳定 -> Given 运行控制台历史操作记录保存在 `ruoyi-vue-pro/runtime/runtime-control` / When 本机后端通过 `restart-int-ruoyi-local.ps1` 重启 / Then 后端必须继续使用该稳定目录读取 `/infra/runtime-control/operations`，不得切换到 `output/runtime/<worktree>/runtime-control`。

BDD: 运行产物目录仅保存 jar 和进程日志 -> Given 本机重启脚本需要保存 jar、stdout、stderr 等运行产物 / When 脚本生成本次启动产物 / Then 这些产物仍保存在 `output/runtime/<worktree>`，但运行控制操作状态不随产物目录漂移。

REPRO: Playwright 真实页面刷新 -> FAIL，`/admin-api/infra/runtime-control/operations` 返回 `{"code":0,"msg":"","data":[]}`，页面“最近操作”表格为空。

DIAGNOSIS: 当前 Java 进程参数 -> `--yudao.runtime-control.state-dir=D:\ProjectPackage\Int\IntRuoyi\output\runtime\int_main\runtime-control`；历史操作记录 JSON 位于 `ruoyi-vue-pro/runtime/runtime-control`。

RED: `python -m pytest script/tests/test_runtime_control_scripts.py -q` -> FAIL，当前本机重启脚本仍使用 `$RuntimeControlStateDir = Join-Path $RuntimeDir 'runtime-control'`，会把状态目录切到 `output/runtime/<worktree>/runtime-control`。

GREEN: `python -m pytest script/tests/test_runtime_control_scripts.py -q` -> PASS，12 passed。

GREEN: `python -m pytest script/tests/test_restart_int_ruoyi_local_schema.py -q` -> PASS，2 passed。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" test` -> PASS，35 tests。

GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> PASS。

GREEN: 当前 Java 进程参数 -> `--yudao.runtime-control.state-dir=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\runtime\runtime-control`。

GREEN: Playwright 本机真实页面刷新 -> PASS，`/operations` 返回 34 条，最近操作表格显示 34 行。

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260603-runtime-control-recent-operations-visible/bug-regression-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-runtime-control-recent-operations-visible --mode preview` -> PASS，delete `<none>`、blocked `<none>`、warnings `<none>`。
