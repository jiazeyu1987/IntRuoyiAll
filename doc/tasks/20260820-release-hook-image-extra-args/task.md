# 20260820 发布 Hook 镜像额外参数修复

## Task Goal

修复主程序后端发布镜像启动命令和路线快照迁移 Runner，使测试服发布脚本的一次性迁移 Hook 能在不覆盖 compose `ARGS` 的前提下追加 `INTRUOYI_EXTRA_ARGS`，关闭调度器，执行路线快照身份 BACKFILL/READINESS，并在成功后退出一次性 JVM。

## Milestones

1. 记录发布失败根因、经验门禁和设计约束。
2. 先补失败测试，证明后端 Dockerfile 未消费 `INTRUOYI_EXTRA_ARGS`。
3. 最小修改主程序 `Dockerfile.backend` 和路线快照迁移 Runner，让镜像命令追加额外参数并在 Hook 成功后退出。
4. 运行定向回归并提交主程序修复。
5. 将新提交交回维护发布流程，用新 releaseTag 重新构建发布。

## Expected Verification

- `python -X utf8 -m pytest script/tests/test_backend_release_image_contract.py -q` 先失败后通过。
- `script/deploy/int-ruoyi-test/Dockerfile.backend` 同时保留 `${ARGS}` 并追加 `${INTRUOYI_EXTRA_ARGS}`。
- `MesProRouteVersionSnapshotMigrationCommandTest` 确认 Runner 成功后调用退出路径，避免一次性 Hook 长时间驻留。
- 提交只包含本任务拥有的 Dockerfile、测试和任务记录。

## 经验门禁

- Trigger: 测试服 `publish-test` 在 required SQL 前的一次性 backend Hook 中仍启动 Quartz、Spring scheduled job，或 `--server.port=0` / `--spring.quartz.auto-startup=false` 未生效。
- Preflight check: 构建前检查实际用于后端镜像构建的主程序 `script/deploy/int-ruoyi-test/Dockerfile.backend`，不得只检查维护仓副本；构建后可用 `docker image inspect` 验证 `Config.Cmd` 包含 `${ARGS} ${INTRUOYI_EXTRA_ARGS}`。
- Blocker: 后端镜像命令未消费 `INTRUOYI_EXTRA_ARGS`，或发布 Hook 参数只能写入环境变量但不会进入 Java 命令行。
- Verification: 主程序 Dockerfile 合同测试、远端/本地镜像 `Config.Cmd`、发布日志中 Hook 容器端口与调度关闭参数生效。
- Forbidden action: 禁止只改维护仓 Dockerfile 副本、手工修改失败 releaseTag 的包或远端 compose；失败 tag 不得复用。
- Evidence: `D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260819-test-only-head-release\evidence\publish-test-failure-summary-r260820e-r1.json`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；修复实际镜像命令契约，不跳过 Hook 或 required SQL。
- 是否从根因和长期维护角度解决：是；主程序源头 Dockerfile 是发布脚本实际使用的构建输入。
- 是否存在临时补丁或绕过：否。

## Current Status

ready_for_closeout

## Verification Evidence

- RED: `python -X utf8 -m pytest script/tests/test_backend_release_image_contract.py -q` -> FAIL，确认实际主程序 Dockerfile 未消费 `INTRUOYI_EXTRA_ARGS`。
- GREEN: `python -X utf8 -m pytest script/tests/test_backend_release_image_contract.py script/tests/test_runtime_control_scripts.py::test_remote_status_script_exposes_current_release_package_from_runtime_env -q` -> PASS，2 tests。
- GREEN: 隔离修复 worktree `mvn -pl yudao-module-mes -Dtest=MesProRouteVersionSnapshotMigrationCommandTest test` -> PASS，4 tests。
- RED: 基准主工作区 Maven 复跑被无关 DCC/ERP dirty API 漂移阻断在 compile 阶段；后续以干净 release worktree build-release 作为提交后集成验收。
