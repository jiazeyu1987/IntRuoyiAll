# 任务：修复中文发布包名构建失败

## 任务目标

修复运行控制台“构建发布包”在 `ReleaseTag` 包含中文说明（例如 `26-06-04 21:22:26 QMS已导入`）时失败的问题。脚本必须保留可读发布标签用于 manifest 与历史记录，同时生成确定性的文件系统、Docker tag 和 NAS 路径安全目录名；不得因控制台编码差异把中文误判为非法字符。

## Previous Task Check

- 上一个相关任务：`doc/tasks/20260604-test-deploy-showroom-image-json/task.md`
- 状态：`completed`
- 历史相关任务：`doc/tasks/20260529-release-tag-colon-path/task.md`
- 状态：`completed`
- 处理：上一任务和历史冒号发布包名修复均已完成；本任务仅修复中文/UTF-8 发布包名在构建发布包入口的校验与转换契约。

## BDD 场景

- BDD: 中文发布包名可构建为安全目录名 -> Given 运行控制台传入 `ReleaseTag=26-06-04 21:22:26 QMS已导入` / When `publish-int-ruoyi.ps1 -Mode build-release` 校验发布包名 / Then 脚本应接受原始可读发布标签，并把目录名转换为确定性的安全 ASCII 名称。
- BDD: 乱码发布包名必须暴露具体编码问题 -> Given PowerShell 进程收到已经损坏的 `ReleaseTag=26-06-04 21:22:26 QMS�ѵ���` / When 脚本校验发布包名 / Then 脚本应 fail fast，提示命令调用方传参编码已损坏，而不是继续生成错误发布包。
- BDD: 发布包目录转换规则保持跨动作一致 -> Given 构建、部署、标记测试通过和正式上线都传入同一个中文 `ReleaseTag` / When 脚本访问本地或 NAS 发布包路径 / Then 所有动作必须使用同一转换函数定位同一目录名。

## 里程碑

- [x] M1：建立任务文档并确认前置任务已完成。
- [x] M2：复现当前失败并新增 RED 回归测试。
- [x] M3：最小修复发布脚本的 UTF-8/中文标签校验与目录名转换。
- [x] M4：运行目标脚本测试、语法检查和证据校验。
- [x] M5：运行 task-closeout-cleanup 预览并按策略提交本任务改动。

## 预期验证

- RED/GREEN：`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- GREEN：PowerShell 解析 `script\deploy\publish-int-ruoyi.ps1` 无语法错误。
- GREEN：`python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260604-release-tag-utf8-package-name/bug-regression-evidence.md`
- GREEN：`python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence doc/tasks/20260604-release-tag-utf8-package-name/ci-cd-evidence.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。编码已损坏时必须直接阻塞并报告调用方编码问题；不自动猜测原中文。
- `是否从根因和长期维护角度解决`：是。统一发布标签到安全目录名的转换契约，并覆盖中文标签与损坏编码输入。
- `是否存在临时补丁或绕过`：否。不修改运行控制台参数、不跳过发布包名校验、不把失败改成默认成功。

## 当前状态

completed

## Current Status

completed

## 验证结果

- VERIFY：上一相关任务 `doc/tasks/20260604-test-deploy-showroom-image-json/task.md` 状态为 `completed`。
- VERIFY：历史冒号发布包名任务 `doc/tasks/20260529-release-tag-colon-path/task.md` 状态为 `completed`。
- RED：`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "chinese_release_tag or corrupted_release_tag" -q` -> FAIL，原因：中文 `ReleaseTag` 被旧校验拒绝，已损坏编码输入也只返回泛化非法字符错误。
- GREEN：`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "chinese_release_tag or corrupted_release_tag" -q` -> PASS，2 passed。
- GREEN：`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，58 passed。
- GREEN：PowerShell parser check for `script\deploy\publish-int-ruoyi.ps1` -> PASS。
- GREEN：`python -X utf8 -m pytest script\tests\test_runtime_control_ops_scripts.py script\tests\test_runtime_control_scripts.py -q` -> PASS，19 passed。
- GREEN：bug regression evidence validator -> PASS。
- GREEN：CI/CD environment evidence validator -> PASS。
- GREEN：task-closeout-cleanup preview -> PASS，keep 四项，delete/blocked/warnings 均为空。
- BLOCKED：task-closeout-cleanup apply 首次执行被阻塞，原因是清理脚本只识别 `## Current Status`，未识别中文 `## 当前状态`；已补充英文状态段后重试。
- GREEN：task-closeout-cleanup apply -> PASS，delete/blocked/warnings/deleted_paths 均为空。

## Cleanup Keep

- `doc/tasks/20260604-release-tag-utf8-package-name/task.md`
- `doc/tasks/20260604-release-tag-utf8-package-name/execution-log.md`
- `doc/tasks/20260604-release-tag-utf8-package-name/bug-regression-evidence.md`
- `doc/tasks/20260604-release-tag-utf8-package-name/ci-cd-evidence.md`
