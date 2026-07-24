# 执行日志：本地 runtime-control 与发布临时产物迁移到 E 盘缓存目录

BDD: 发布临时包写入 E 盘缓存 -> Given 运维人员执行 `publish-int-ruoyi.ps1 -Mode build-release|deploy-release` / When 脚本生成 release package 临时目录 / Then 默认路径必须位于 `E:\Int\CacheData\IntRuoyi\publish-int-ruoyi`，不得写入 `ruoyi-vue-pro\tmp\publish-int-ruoyi`。

BDD: runtime-control 状态写入 E 盘缓存 -> Given 本地 runtime-control 使用默认配置启动 / When 生成 operation json、runner script 或操作日志 / Then 默认 state-dir 必须位于 `E:\Int\CacheData\IntRuoyi\runtime-control`。

BDD: 缓存目录不可用必须失败 -> Given `E:\Int\CacheData\IntRuoyi` 无法创建或不可写 / When 发布脚本启动 / Then 脚本必须直接失败并说明缺失前置条件，不得静默切回 D 盘仓库目录。

RED: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k cachedata -q` -> FAIL, 发布脚本缺少 `LocalCacheRoot` 配置且仍使用 `ruoyi-vue-pro\tmp\publish-int-ruoyi`。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k cachedata -q` -> PASS。

GREEN: PowerShell parser 检查 `script\deploy\publish-int-ruoyi.ps1` -> PASS。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，50 passed。

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#defaultRuntimeControlPropertiesShouldSeparateReleaseAndBackupNasRoots test` -> PASS。

GREEN: `git diff --check` 目标文件 -> PASS。
