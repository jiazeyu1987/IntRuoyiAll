# 执行日志：修复 NAS 发布包挂载盘符作用域

- BDD: NAS 发布包盘符在发布动作期间保持可访问 -> Given 运行控制台调用统一发布脚本并挂载 NAS 发布包共享 / When 脚本返回挂载信息后继续解析发布包路径 / Then PowerShell 盘符仍在脚本作用域可访问，缺包时应报发布包不存在，而不是 `DriveNotFound`。

## 记录

- 2026-05-29：从运行控制台失败记录确认 `publish-test`、`mark-release-tested`、`promote-prod` 都在 `Mount-NasReleaseShare` 后访问 `IRxxxx:` 盘符时报 `DriveNotFound`；根因是 `New-PSDrive` 在函数作用域创建，函数返回后盘符不可见。

## RED

- RED: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, expected reason: 发布脚本缺少 `New-PSDrive -Scope Script`，函数返回后 NAS 盘符不可见，后续 `Join-Path` 报 `DriveNotFound`。

## GREEN

- 2026-05-29：修改 `script/deploy/publish-int-ruoyi.ps1`，`Mount-NasReleaseShare` 使用 `New-PSDrive -Scope Script`，`Dismount-NasReleaseShare` 使用同一作用域卸载。
- GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 22 passed。

## REGRESSION

- GREEN: `python -X utf8 -m pytest script\tests\test_runtime_control_ops_scripts.py -q` -> PASS, 5 passed。
- GREEN: PowerShell parser check for `script\deploy\publish-int-ruoyi.ps1` -> PASS。
- GREEN: local PSDrive scope check -> PASS，函数返回后 `Join-Path` 能访问脚本作用域盘符。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260529-nas-release-psdrive-scope --mode preview` -> PASS，保留 `task.md` 与 `execution-log.md`，无删除、阻塞或警告。

## Blockers

- 当前无。
