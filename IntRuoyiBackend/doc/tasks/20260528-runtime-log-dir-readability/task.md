# 任务：修复本地运行控制台日志目录不可读提示

## 目标

处理运行控制台提示 `日志目录不存在或不可读：D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\logs` 的问题，确认本地环境的日志目录配置与实际后端日志输出目录一致，并保持缺失目录时显式阻断的 fail-fast 行为。

## 里程碑

- [x] M1：定位报错来源、当前日志目录配置和实际日志输出配置。
- [x] M2：编写或更新回归测试，先证明本地配置不应指向仓库根目录下不存在的 `logs`。
- [x] M3：最小化修复配置或代码，使运行控制台检查真实日志目录。
- [x] M4：运行目标测试和必要回归验证。
- [x] M5：记录证据、执行收尾清理预览并提交本任务改动。

## BDD 场景

BDD: 本地运行控制台检查真实日志目录 -> Given 本地后端日志输出到用户目录下的 `logs` / When 运行控制台查询容量或健康状态 / Then 日志目录检查应指向实际日志根目录，不应误报仓库根目录 `logs` 不存在。

BDD: 日志目录真实缺失时仍显式阻断 -> Given 配置的日志目录确实不存在或不可读 / When 运行控制台查询容量或健康状态 / Then 响应必须保留 `BLOCKED` 与明确原因，不得静默成功、自动降级或使用 mock 数据。

## 预期验证

- `mvn -pl yudao-module-infra "-Dtest=RuntimeStorageGuardServiceImplTest" test`
- `mvn -pl yudao-server "-Dtest=RuntimeControlLocalConfigTest" test`
- 必要时运行包含运行控制台配置契约的相关测试。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260528-runtime-log-dir-readability\bug-regression-evidence.md`

## Cleanup Keep

- doc/tasks/20260528-runtime-log-dir-readability/task.md
- doc/tasks/20260528-runtime-log-dir-readability/execution-log.md
- doc/tasks/20260528-runtime-log-dir-readability/bug-regression-evidence.md

## 当前状态

completed

## 当前证据

- RED：`RuntimeControlProperties.StorageGuard.logDir` 默认值为 `logs`，会被解析成 `repoRoot/logs`；本地 `application-local.yaml` 的 Spring 日志实际写入 `${user.home}/logs/${spring.application.name}.log`。
- RED：`python -m pytest script\tests\test_runtime_control_local_config.py -q` 失败，原因是本地 profile 未配置 `yudao.runtime-control.storage-guard.log-dir`。
- GREEN：`application-local.yaml` 已显式设置 `yudao.runtime-control.storage-guard.log-dir` 为 `${INTRUOYI_RUNTIME_CONTROL_LOG_DIR:${user.home}/logs}`。
- GREEN：`python -m pytest script\tests\test_runtime_control_local_config.py -q` 通过。
- GREEN：`mvn -pl yudao-module-infra "-Dtest=RuntimeStorageGuardServiceImplTest" test` 通过，3 tests passed。
- GREEN：`mvn -pl yudao-server "-Dtest=RuntimeControlLocalConfigTest" test` 通过，1 test passed。
- GREEN：`python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260528-runtime-log-dir-readability\bug-regression-evidence.md` 通过。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-runtime-log-dir-readability --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --worktree-closeout off` 通过，keep 仅包含任务文档、执行日志和 bug 证据，无 delete/blocker/warning。

## 最终验证结果

- 本地运行控制台日志目录配置已对齐 Spring Boot 实际日志根 `${user.home}/logs`。
- 本机 `C:\Users\BJB110\logs` 存在且可读。
- 缺失日志目录时服务仍保持显式 `BLOCKED`，未引入静默降级或自动创建目录。
