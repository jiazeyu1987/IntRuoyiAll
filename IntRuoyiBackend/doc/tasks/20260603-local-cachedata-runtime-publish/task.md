# 任务：本地 runtime-control 与发布临时产物迁移到 E 盘缓存目录

## 任务目标

将后端统一发布脚本的本地 release package 临时目录、runtime-control 本地状态目录和日志监控默认目录迁移到 `E:\Int\CacheData\IntRuoyi` 下，避免 D 盘仓库继续积累发布包、SQL 快照、镜像 tar 与运行状态文件。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260603-dcc-download-openable-pdf/task.md`
- 状态：`in progress`
- 影响：该任务处理 DCC 下载业务契约；本任务只改发布/运行路径配置，不修改 DCC 下载业务文件，不回退其未提交改动。

## BDD 场景

- BDD: 发布临时包写入 E 盘缓存 -> Given 运维人员执行 `publish-int-ruoyi.ps1 -Mode build-release|deploy-release` / When 脚本生成 release package 临时目录 / Then 默认路径必须位于 `E:\Int\CacheData\IntRuoyi\publish-int-ruoyi`，不得写入 `ruoyi-vue-pro\tmp\publish-int-ruoyi`。
- BDD: runtime-control 状态写入 E 盘缓存 -> Given 本地 runtime-control 使用默认配置启动 / When 生成 operation json、runner script 或操作日志 / Then 默认 state-dir 必须位于 `E:\Int\CacheData\IntRuoyi\runtime-control`。
- BDD: 缓存目录不可用必须失败 -> Given `E:\Int\CacheData\IntRuoyi` 无法创建或不可写 / When 发布脚本启动 / Then 脚本必须直接失败并说明缺失前置条件，不得静默切回 D 盘仓库目录。

## Milestones

- [x] M1：确认现有后端任务与脏改动边界。
- [x] M2：新增 RED 静态测试。
- [x] M3：修改发布脚本、application-local 配置与 runtime-control 默认值。
- [x] M4：运行目标测试，记录 GREEN 证据。
- [x] M5：完成收尾记录。

## Expected Verification

- RED：`python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k cachedata -q` 先失败。
- GREEN：发布脚本静态测试通过。
- GREEN：PowerShell parser 检查 `script/deploy/publish-int-ruoyi.ps1` 通过。
- GREEN：runtime-control 目标 Java 测试通过或记录阻塞。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。默认路径改为 E 盘缓存目录；目录不可创建时失败，不回落到 D 盘。
- `是否从根因和长期维护角度解决`：是。统一本地发布与运行状态产物的缓存根，减少 D 盘仓库膨胀。
- `是否存在临时补丁或绕过`：否。

## 当前状态

completed

## 已完成工作

- 已定位 `script/deploy/publish-int-ruoyi.ps1` 当前 `$localTempRoot = Join-Path $backendRepo 'tmp\publish-int-ruoyi'`。
- 已定位 `application-local.yaml` 与 `RuntimeControlProperties` 当前默认 state-dir 指向仓库 runtime 目录。
- 已将发布脚本默认发布临时目录迁移到 `E:\Int\CacheData\IntRuoyi\publish-int-ruoyi`。
- 已将 `application-local.yaml` 默认 `state-dir` 迁移到 `E:/Int/CacheData/IntRuoyi/runtime-control`。
- 已将 runtime-control 默认 `stateDir` 和 storage guard `logDir` 迁移到 E 盘缓存目录。

## 验证结果

- GREEN：`python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k cachedata -q` -> PASS。
- GREEN：PowerShell parser 检查 `script/deploy/publish-int-ruoyi.ps1` -> PASS。
- GREEN：`python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS，50 passed。
- GREEN：`mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#defaultRuntimeControlPropertiesShouldSeparateReleaseAndBackupNasRoots test` -> PASS。
- GREEN：`git diff --check` 目标文件 -> PASS。

## 剩余阻塞

- 无。
