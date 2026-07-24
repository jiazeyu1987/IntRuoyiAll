# 任务：修复冒号发布包名构建路径错误

## 任务目标

修复运行控制台“构建发布包”使用默认发布包名 `YY-MM-DD HH:MM:SS` 时，发布脚本把包含冒号的 `ReleaseTag` 直接作为 Windows 目录名导致 `New-Item` 报“不支持给定路径格式”的问题。修复后用户仍可使用可读发布包名，脚本必须使用确定性的文件系统安全目录名保存发布包。

## BDD 场景

- BDD: 可读发布时间发布包名可构建 -> Given 运行控制台传入 `ReleaseTag=26-05-29 10:18:24` / When `publish-int-ruoyi.ps1 -Mode build-release` 初始化本地发布目录 / Then 脚本不得把冒号直接放入 Windows 目录路径，而应使用确定性的安全目录名创建发布包目录。
- BDD: 后续部署动作可按同一发布名定位包 -> Given 构建、部署、标记测试通过和上线正式服都只传同一个 `ReleaseTag` / When 脚本访问 NAS 发布包路径 / Then 所有动作必须用同一目录名转换规则定位同一个包。

## 里程碑

- [x] M1：确认错误日志、旧任务状态和根因位置。
- [x] M2：补充 RED 回归测试，锁定冒号发布包名不能直接进入目录路径。
- [x] M3：最小修复发布脚本，分离可读 `ReleaseTag` 与文件系统安全目录名。
- [x] M4：运行脚本测试、PowerShell 解析和 bug 证据校验。
- [x] M5：运行 task-closeout-cleanup 预览并提交本任务改动。

## 预期验证

- `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- PowerShell 解析 `script\deploy\publish-int-ruoyi.ps1` 无语法错误。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260529-release-tag-colon-path/bug-regression-evidence.md`

## 当前状态

completed

## 验证结果

- RED: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL，新增 `test_publish_script_uses_path_safe_directory_for_human_release_tag` 发现脚本缺少 `ConvertTo-ReleasePackageDirectoryName`。
- GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，23 passed。
- GREEN: `python -X utf8 -m pytest script\tests\test_runtime_control_ops_scripts.py -q` -> PASS，5 passed。
- GREEN: PowerShell parser check for `script\deploy\publish-int-ruoyi.ps1` -> PASS。
- GREEN: inline conversion check -> PASS，`26-05-29 10:18:24` 转换为 `26-05-29_10-18-24`，符合发布目录和 Docker tag 正则。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260529-release-tag-colon-path/bug-regression-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260529-release-tag-colon-path --mode preview` -> PASS，delete/blocked/warnings 均为空。

## Cleanup Keep

- doc/tasks/20260529-release-tag-colon-path/task.md
- doc/tasks/20260529-release-tag-colon-path/execution-log.md
- doc/tasks/20260529-release-tag-colon-path/bug-regression-evidence.md
