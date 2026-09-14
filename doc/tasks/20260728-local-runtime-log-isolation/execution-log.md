# Execution Log

## User Intent

- 用户要求“把本地 SQL DEBUG 默认关掉”，并“把后端日志文件改成 runtime 独立路径”。

## BDD

- BDD: 本地 SQL 日志默认降噪 -> Given 本地后端使用 `application-local.yaml`, When 读取 mapper 日志配置, Then 自研模块 mapper 包默认不应是 DEBUG。
- BDD: 本地日志文件 runtime 隔离 -> Given 本地后端使用 `application-local.yaml`, When 未显式覆盖日志路径, Then 默认日志文件应写入 `${user.dir}/output/runtime/${spring.profiles.active}/logs/${spring.application.name}.log`。

## Milestone Status

- 2026-07-28: 创建任务记录，准备 RED 测试。
- RED: `mvn -pl yudao-server "-Dtest=LocalRuntimeLoggingConfigTest" test` -> FAIL, expected reason: 当前 `application-local.yaml` 仍默认启用自研 mapper DEBUG SQL，且后端应用日志仍默认写入 `${user.home}/logs` 共享路径。
- 2026-07-28: 修改 `application-local.yaml`，将自研 MyBatis mapper SQL 默认级别从 DEBUG 降到 INFO，并将日志默认路径改为 runtime 独立目录；修改标准本地重启脚本，显式传入 `output\runtime\<profile>\logs\yudao-server.log`。
- GREEN attempt: `mvn -pl yudao-server "-Dtest=LocalRuntimeLoggingConfigTest,RuntimeControlLocalConfigTest" test` -> FAIL, expected reason: 测试发现 `cn.iocoder.yudao.module.ai.dal.mysql` 仍为 DEBUG，继续收敛漏网 SQL DEBUG。
- GREEN: `mvn -pl yudao-server "-Dtest=LocalRuntimeLoggingConfigTest,RuntimeControlLocalConfigTest" test` -> PASS, 4 tests, 0 failures.
- GREEN: `python -X utf8 -m pytest script/tests/test_runtime_control_local_config.py script/tests/test_runtime_control_scripts.py -q` -> PASS, 16 tests.
- Verification: `rg` 扫描 `application-local.yaml` 未再发现自研 `*.dal.mysql: debug`、共享 `${user.home}/logs/${spring.application.name}.log` 或旧 storage guard 默认路径。
- Experience consolidation: 更新 `docs/local-runtime.md` 现有 `本地后端标准输出阻塞与日志目录门禁`，补充 SQL DEBUG 默认关闭、runtime 日志目录和验证命令；更新 `docs/experience-index.md` 关键词，不新建长期经验文档。
- Cleanup preview: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-local-runtime-log-isolation --mode preview` -> PASS, keep task/execution/verification only, delete none, blocked none.
- Cleanup apply: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-local-runtime-log-isolation --mode apply` -> PASS, deleted none.
- Verification: `git diff --check -- <task-owned paths>` -> PASS.
- Closeout blocker: 未提交/推送；工作区存在大量任务前既有脏改动，按项目规则提交前需用户授权先做 dirty-worktree baseline commit。
