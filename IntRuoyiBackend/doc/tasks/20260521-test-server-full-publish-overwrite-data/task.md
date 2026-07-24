# 任务：发布当前系统到测试服务器并覆盖测试数据

## Goal

- 将当前本地 `IntRuoyi` 系统发布到测试服务器 `172.30.30.58`。
- 同步当前本地前端、后端、MySQL 数据库和 MinIO `yudao` 桶内容到测试环境。
- 按用户要求覆盖测试服务器现有业务数据，并完成发布后真实验证。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.bat`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.ps1`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\show-int-ruoyi-test-status.bat`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-test-server-full-publish-overwrite-data\**`

## Non-Scope

- 不发布正式服务器 `172.30.30.57`。
- 不引入 fallback、兼容分支、临时 mock 或静默降级。
- 不在本任务中修改与发布无关的业务代码。

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-backend-gitignore-hygiene\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact on this task: 无阻塞，可继续推进本次测试环境全量发布。

## Milestones

- [x] M1：创建任务文档并确认上一同仓任务状态。
- [x] M2：记录发布边界并完成只读预检。
- [x] M3：执行测试环境全量发布。
- [x] M4：验证测试环境代码、数据库和文件对象已被当前本地系统覆盖。
- [x] M5：回写证据、执行 cleanup 预览，并评估是否可安全提交本任务产物。

## Expected Verification

- `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\show-int-ruoyi-test-status.bat`
- `cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.bat default`
- `ssh root@172.30.30.58 "docker ps --format '{{.Names}} {{.Status}}' | grep '^intruoyi-'"`
- `ssh root@172.30.30.58 "curl -fsS http://127.0.0.1:48081/actuator/health"`
- `ssh root@172.30.30.58 "curl -I -s http://127.0.0.1:8081/"`
- `ssh root@172.30.30.58 "docker exec intruoyi-mysql mysql -N -uroot -p<runtime-password> -D ruoyi-vue-pro -e \"SELECT COUNT(*) FROM infra_file;\""`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-test-server-full-publish-overwrite-data --mode preview`

## Current Status

Completed on 2026-05-23.

## Blockers

- 2026-05-21 曾因用户切换到更高优先级的数据核对问题暂停在发布前预检阶段；2026-05-23 已恢复并完成发布。
- 首次恢复发布时，管理前端 `pnpm exec vite build --mode test` 因 Node 默认堆内存不足失败；已使用明确的 `NODE_OPTIONS=--max-old-space-size=8192` 重新执行同一构建和发布脚本。
- 第二次恢复发布时，未跟踪且仍为 `In Progress` 的 `20260523-infra-runtime-control-panel` RED 测试残留污染 Maven test-compile；已删除该未完成任务产物后重新确认后端仓库干净，再执行发布。

## Current Findings

- 发布脚本默认模式会同步前端、后端、MySQL 和 MinIO；其中数据库同步会先重建远端 `ruoyi-vue-pro` 数据库，再导入当前本地库。
- 发布脚本默认模式不会保留测试服务器原有业务数据；这与用户本次“覆盖测试服务器数据”的要求一致。
- 发布目标固定为测试服务器 `172.30.30.58`，运行目录为 `/opt/intruoyi/runtime`，前端端口 `8081`，后端端口 `48081`。
- 发布前状态快照已确认：`/opt/intruoyi/runtime` 存在，测试环境 `intruoyi-frontend`、`intruoyi-backend`、`intruoyi-mysql`、`intruoyi-redis` 均处于运行中，前后端 HTTP 检查为 `200`。
- 2026-05-23 已完成默认模式全量发布，发布批次为 `20260523_142453`。
- 测试服务器当前 `intruoyi-frontend` 和 `intruoyi-backend` 镜像均为 `20260523_142453`，`intruoyi-website` 已重建并运行。
- 发布后后端健康、管理前端、Website 根路径和 Website `/showroom` 均返回 HTTP `200`。
- 发布后数据库计数已确认：`infra_file=2034`，`showroom_product=191`，`system_tenant=4`。
- 发布后 MinIO 远端 `yudao/showroom/product/cover/20260523/` 当前可列出 `98` 个对象。

## Cleanup Keep

- `doc/tasks/20260521-test-server-full-publish-overwrite-data/task.md`
- `doc/tasks/20260521-test-server-full-publish-overwrite-data/execution-log.md`
