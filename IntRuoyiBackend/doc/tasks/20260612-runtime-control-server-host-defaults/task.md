# 20260612 修复运行控制台 test.serverHost 缺失

## 任务目标

修复运行控制台远程根分区动作报错 `运行控制台动作缺少必填参数：test.serverHost`，确保后端默认运行控制台属性能够提供测试服、正式服和备份服固定服务器 IP，并继续保持目标环境/IP 边界校验。

## 前置任务检查

- 最近后端任务：`20260612-report-recognition-select-word-file`。
- 状态：`BLOCKED_ON_EXTERNAL_BACKEND_COMPILE`。
- 处理结论：旧任务已在其任务文档中明确阻塞，阻塞来自非本任务 EDHR/工艺路线脏改编译错误；本任务限定在 `yudao-module-infra` 运行控制台配置与测试范围。

## 里程碑

1. M1 审计：定位 `test.serverHost` 缺失来源和实际调用链。
2. M2 RED：新增/调整运行控制台属性默认契约测试，复现默认属性缺少固定 host。
3. M3 GREEN：最小实现固定远程服务器 host 默认值，并保持正式服写动作禁用和目标证明校验。
4. M4 REGRESSION：运行运行控制台相关后端测试。
5. M5 收尾：记录证据，运行收尾清理预览。

## 预期验证

- `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest,RuntimeRemoteRootDiskServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence ruoyi-vue-pro/doc/tasks/20260612-runtime-control-server-host-defaults/bug-regression-evidence.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；本次不添加备用分支、不吞异常，只补齐运行控制台固定服务器 IP 的正式默认契约。
- `是否从根因和长期维护角度解决`：是；`RuntimeControlProperties` 默认环境不应依赖单一 profile 才具备固定 host，服务层仍按固定 IP 做边界校验。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：COMPLETED。
- 已完成：M1 审计定位到 `RuntimeRemoteRootDiskServiceImpl.requireKnownEnvironment()` 在 `properties.environments.test.host` 为空时抛出 `test.serverHost`；M2 RED 复现同一异常；M3 已将 `test/prod/backup` 固定服务器 IP 写入 `RuntimeControlProperties` 默认契约，并让远程根分区边界校验共用同一组常量；M4 目标和邻近回归通过。
- 最终验证：`RuntimeControlServiceImplTest`、`RuntimeRemoteRootDiskServiceImplTest`、`RuntimeRestoreCandidateServiceImplTest` 共 85 tests PASS；bug 回归证据校验 PASS；收尾清理预览无删除、无阻塞、无警告。
- 剩余阻塞：无。

## Cleanup Keep

- doc/tasks/20260612-runtime-control-server-host-defaults/bug-regression-evidence.md
