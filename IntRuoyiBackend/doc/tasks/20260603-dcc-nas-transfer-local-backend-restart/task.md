# 任务：重启本机后端加载 NAS 转移类别绑定修复

## 任务目标

重建并重启本机 `int_main` 后端，使 `http://127.0.0.1:48081` 加载提交 `2e46186a62 任务: 校验NAS转移类别目录绑定`，避免前端继续访问旧 jar `backend-runtime-control-20260603-180330.jar`。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260603-dcc-nas-transfer-category-binding-validation/task.md`
- 状态：`completed`
- 影响：上一任务已完成代码修复和提交；本任务只执行本机后端重建/重启与运行态验证，不修改生产代码。

## BDD 场景

- BDD: 本机后端加载最新 DCC 修复 -> Given 当前 48081 后端仍运行修复提交前的旧 jar / When 执行本机 backend 重启脚本 / Then 48081 后端应重新打包并启动新的 runtime jar，健康检查返回 UP，运行 jar 时间晚于提交 `2e46186a62`。
- BDD: 重启缺少前置条件必须失败 -> Given 本机 Docker、MySQL、Redis、必需环境变量或受保护展厅文件配置缺失 / When 执行重启脚本 / Then 脚本必须明确失败，不用旧后端或降级配置冒充成功。

## Milestones

- [x] M1：建立任务文档并确认上一修复任务已完成。
- [x] M2：记录旧后端运行 jar 和提交时间差。
- [x] M3：执行现有本机后端重启脚本。
- [x] M4：验证 48081 健康、进程 jar 和日志。
- [x] M5：记录最终结果并完成任务。

## Expected Verification

- `Invoke-WebRequest http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`。
- Java 进程命令行中的 runtime jar 时间晚于 `2026-06-03 18:19:12 +0800`。
- 若重启失败，记录缺失前置条件和影响，不提交、不声明成功。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。使用现有脚本，缺少前置条件直接失败。
- `是否从根因和长期维护角度解决`：是。重建并重启本机后端加载已提交修复，而不是让前端隐藏旧失败。
- `是否存在临时补丁或绕过`：否。

## 当前状态

completed

## 已完成工作

- 确认旧后端进程运行 `D:\ProjectPackage\Int\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260603-180330.jar`，早于修复提交 `2e46186a62`。
- 执行 `.\script\deploy\restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main`，脚本完成且退出码为 `0`。
- 新后端进程 PID 为 `60592`，运行 jar 为 `D:\ProjectPackage\Int\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260603-182800.jar`。
- 48081 健康检查返回 `{"status":"UP"}`。

## 验证结果

- GREEN：`Invoke-WebRequest -UseBasicParsing http://127.0.0.1:48081/actuator/health` -> PASS，`{"status":"UP"}`。
- GREEN：Java 进程命令行 -> PASS，当前 48081 后端使用 `backend-runtime-control-20260603-182800.jar`，晚于 `2e46186a62 2026-06-03 18:19:12 +0800`。
- CLOSEOUT PREVIEW：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dcc-nas-transfer-local-backend-restart --mode preview` -> PASS，delete `<none>`，blocked `<none>`，warnings `<none>`。

## Rollback

- 如本机后端异常，可重新执行同一脚本恢复最新构建；如必须回退，需要明确指定旧 jar 并手动启动，风险是重新暴露 NAS 转移类别绑定缺失的旧问题。
