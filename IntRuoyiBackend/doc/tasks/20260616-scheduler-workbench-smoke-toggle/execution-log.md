# 执行日志

- 2026-06-16：创建后端任务记录，定位 `MesProSchedulerWorkbenchController` 与 `MesProSchedulerWorkbenchService`。
- BDD: 空闲时启动冒烟测试 -> Given 当前没有冒烟测试进程 / When 调用启动接口 / Then 后端按当前操作系统启动 npm 脚本并返回运行状态。
- BDD: 运行时拒绝重复启动 -> Given 冒烟测试进程仍在运行 / When 再次调用启动接口 / Then 后端 fail fast 返回“冒烟测试正在运行”。
- BDD: 运行时停止冒烟测试 -> Given 冒烟测试进程仍在运行 / When 调用停止接口 / Then 后端终止该进程和子进程并返回停止状态。
- BDD: 配置缺失时不伪造成功 -> Given 冒烟测试工作目录或脚本配置缺失 / When 调用启动接口 / Then 后端返回明确错误，不启动任何替代命令。
- RED: `mvn -pl yudao-module-mes -Dtest=MesProSchedulerWorkbenchSmokeTestServiceImplTest test` -> FAIL, 当前后端缺少 `MesProSchedulerWorkbenchSmokeTestStatusRespVO`、冒烟测试服务、进程控制接口和错误码。
- RED: `mvn -pl yudao-module-mes -Dtest=MesProSchedulerWorkbenchSmokeTestServiceImplTest test` -> FAIL, 新增停止失败保护测试缺少 `PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_STOP_FAILED` 错误码。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProSchedulerWorkbenchSmokeTestServiceImplTest test` -> PASS，6 tests。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260616-scheduler-workbench-smoke-toggle --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
