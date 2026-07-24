# 执行日志：修复冒号发布包名构建路径错误

- BDD: 可读发布时间发布包名可构建 -> Given 运行控制台传入 `ReleaseTag=26-05-29 10:18:24` / When `publish-int-ruoyi.ps1 -Mode build-release` 初始化本地发布目录 / Then 脚本不得把冒号直接放入 Windows 目录路径，而应使用确定性的安全目录名创建发布包目录。
- BDD: 后续部署动作可按同一发布名定位包 -> Given 构建、部署、标记测试通过和上线正式服都只传同一个 `ReleaseTag` / When 脚本访问 NAS 发布包路径 / Then 所有动作必须用同一目录名转换规则定位同一个包。

## Reproduction

- 用户提供的真实失败命令：`powershell.exe -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi.ps1 -Mode build-release -ReleaseTag 26-05-29 10:18:24 ...`
- 失败点：`New-Item -ItemType Directory -Force -Path $releaseDir`，`$releaseDir` 由 `$Tag=$ReleaseTag` 得到，因此 Windows 路径段包含 `:`。

## RED

- RED: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL，新增 `test_publish_script_uses_path_safe_directory_for_human_release_tag` 发现脚本缺少 `ConvertTo-ReleasePackageDirectoryName`。

## GREEN

- GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，23 passed。
- GREEN: `python -X utf8 -m pytest script\tests\test_runtime_control_ops_scripts.py -q` -> PASS，5 passed。
- GREEN: PowerShell parser check for `script\deploy\publish-int-ruoyi.ps1` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260529-release-tag-colon-path/bug-regression-evidence.md` -> PASS。

## REGRESSION

- GREEN: inline conversion check -> PASS，`26-05-29 10:18:24` 转换为 `26-05-29_10-18-24`，符合发布目录和 Docker tag 正则。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260529-release-tag-colon-path --mode preview` -> PASS，delete/blocked/warnings 均为空。

## Blockers

- 当前无。
