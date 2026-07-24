# 执行日志：NAS 发布包流转后端与发布脚本

BDD: NAS 发布包构建 -> Given NAS 管理配置可用 / When 用户在控制台构建发布包 / Then 后端生成一次性 NAS 配置并调用 `-Mode build-release`，脚本构建固定 ReleaseTag 并上传到 NAS。

BDD: 测试服部署 NAS 发布包 -> Given NAS 上存在发布包 ReleaseTag / When 用户选择部署测试服 / Then 脚本使用 `-Mode deploy-release -Environment test -ReleaseTag <tag>` 从 NAS 发布包部署。

BDD: 正式服只能上线已验证发布包 -> Given ReleaseTag 未标记测试通过 / When 用户尝试上线正式服 / Then 脚本因 `-RequireTested` 缺少 `tested.json` 失败；Given 已标记 / When 用户输入 `PROD` / Then 正式服部署同一个 ReleaseTag。

BDD: NAS 密码不落日志 -> Given NAS 管理配置包含密码 / When 后端构造运行控制命令 / Then 命令参数只包含一次性配置文件路径，不包含密码明文。

RED: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_runtime_control_ops_scripts.py -q` -> FAIL，发布脚本缺少发布包模式、NAS 函数和运行控制动作。

RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeOpsResponsibilityServiceImplTest" test` -> FAIL，NAS 配置 stub 放在全局 setUp 后被非发布包用例判定为未使用。

GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_runtime_control_ops_scripts.py -q` -> PASS，23 passed。

GREEN: PowerShell parser check for `script\deploy\publish-int-ruoyi.ps1` -> PASS。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeBackupDrillServiceImplTest,RuntimeOpsResponsibilityServiceImplTest,RuntimeControlCanonicalContractTest,RuntimeControlHighRiskActionContractTest" test` -> PASS，34 passed，覆盖 NAS 临时配置执行后删除。

GREEN: task-closeout-cleanup preview/apply -> PASS，仅删除测试生成的 `runtime/` 临时目录。
