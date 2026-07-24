# 执行日志：修复本地运行控制台日志目录不可读提示

BDD: 本地运行控制台检查真实日志目录 -> Given 本地后端日志输出到用户目录下的 `logs` / When 运行控制台查询容量或健康状态 / Then 日志目录检查应指向实际日志根目录，不应误报仓库根目录 `logs` 不存在。

BDD: 日志目录真实缺失时仍显式阻断 -> Given 配置的日志目录确实不存在或不可读 / When 运行控制台查询容量或健康状态 / Then 响应必须保留 `BLOCKED` 与明确原因，不得静默成功、自动降级或使用 mock 数据。

RED: `python -m pytest script\tests\test_runtime_control_local_config.py -q` -> FAIL，预期失败原因：`application-local.yaml` 未配置 `yudao.runtime-control.storage-guard.log-dir`，运行控制台使用默认相对路径 `logs` 并解析为仓库根目录 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\logs`。

GREEN: `python -m pytest script\tests\test_runtime_control_local_config.py -q` -> PASS，本地 profile 已显式配置运行控制台日志目录为 `${INTRUOYI_RUNTIME_CONTROL_LOG_DIR:${user.home}/logs}`。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeStorageGuardServiceImplTest" test` -> PASS，3 tests passed，真实缺失日志目录仍返回 `BLOCKED`，没有静默成功或降级。

GREEN: `mvn -pl yudao-server "-Dtest=RuntimeControlLocalConfigTest" test` -> PASS，1 test passed，后端 `src/test` 配置契约覆盖本地运行控制台日志目录与 Spring 日志根一致。

NOTE: `powershell -NoProfile -ExecutionPolicy Bypass -File script\deploy\show-int-ruoyi-local-status.ps1 -Component backend -Json` -> BLOCKED，既有 worktree 映射存在额外前端 `showroom-hall-description-export` 且无对应后端，状态脚本在探测前失败；非本次修复范围。

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260528-runtime-log-dir-readability\bug-regression-evidence.md` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-runtime-log-dir-readability --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --worktree-closeout off` -> PASS，keep 为 task.md、execution-log.md、bug-regression-evidence.md，无 delete、blocked、warnings。
