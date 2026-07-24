# 执行日志：修复中文发布包名构建失败

- BDD: 中文发布包名可构建为安全目录名 -> Given 运行控制台传入 `ReleaseTag=26-06-04 21:22:26 QMS已导入` / When `publish-int-ruoyi.ps1 -Mode build-release` 校验发布包名 / Then 脚本应接受原始可读发布标签，并把目录名转换为确定性的安全 ASCII 名称。
- BDD: 乱码发布包名必须暴露具体编码问题 -> Given PowerShell 进程收到已经损坏的 `ReleaseTag=26-06-04 21:22:26 QMS�ѵ���` / When 脚本校验发布包名 / Then 脚本应 fail fast，提示命令调用方传参编码已损坏，而不是继续生成错误发布包。
- BDD: 发布包目录转换规则保持跨动作一致 -> Given 构建、部署、标记测试通过和正式上线都传入同一个中文 `ReleaseTag` / When 脚本访问本地或 NAS 发布包路径 / Then 所有动作必须使用同一转换函数定位同一目录名。

## 记录

- VERIFY：前置任务 `doc/tasks/20260604-test-deploy-showroom-image-json/task.md` 与历史相关任务 `doc/tasks/20260529-release-tag-colon-path/task.md` 均为 `completed`。
- RED: 中文发布包名转换测试 -> `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "chinese_release_tag or corrupted_release_tag" -q` -> FAIL，旧 `ConvertTo-ReleasePackageDirectoryName` 对 `26-06-04 21:22:26 QMS已导入` 返回 `ReleaseTag contains unsupported package name characters`。
- RED: 损坏编码输入提示测试 -> `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "chinese_release_tag or corrupted_release_tag" -q` -> FAIL，`QMS�ѵ���` 未得到编码损坏的明确 fail-fast 提示。
- GREEN: 中文与损坏编码目标测试 -> `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "chinese_release_tag or corrupted_release_tag" -q` -> PASS，2 passed。
- GREEN: 发布脚本完整契约测试 -> `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，58 passed。
- GREEN: PowerShell 语法解析 -> parser check for `script\deploy\publish-int-ruoyi.ps1` -> PASS。
- GREEN: 运行控制脚本回归 -> `python -X utf8 -m pytest script\tests\test_runtime_control_ops_scripts.py script\tests\test_runtime_control_scripts.py -q` -> PASS，19 passed。
- GREEN: bug evidence validator -> `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260604-release-tag-utf8-package-name/bug-regression-evidence.md` -> PASS。
- GREEN: CI/CD evidence validator -> `python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence doc/tasks/20260604-release-tag-utf8-package-name/ci-cd-evidence.md` -> PASS。
- GREEN: task-closeout-cleanup preview -> `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-release-tag-utf8-package-name --mode preview` -> PASS，keep 四项，delete/blocked/warnings 为空。
- BLOCKED: task-closeout-cleanup apply -> 首次 `--mode apply` 被阻塞，原因是任务文档只有中文 `## 当前状态`，清理脚本状态解析为 `unknown`；已补充 `## Current Status`。
- GREEN: task-closeout-cleanup apply -> `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-release-tag-utf8-package-name --mode apply` -> PASS，delete/blocked/warnings/deleted_paths 为空。
