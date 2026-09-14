# 重启 int_main 最新融合后端

## Task Goal

核对本机 `48081` 后端是否可证明来自最新 `origin/int_main`；若无法证明，则从最新远端提交隔离构建正式后端 Jar，在不修改主工作区并行源码的前提下重启 `E:\IntRuoyi` 的 `int_main` 后端运行态。

## Milestones

- [x] 核对当前 `48081` PID、命令行、健康状态和 Git 分叉状态。
- [x] 创建最新 `origin/int_main` 隔离构建 worktree，并登记 runtime slot。
- [ ] 运行目标回归与后端打包，记录来源提交和 Jar SHA-256。
- [ ] 备份当前运行 Jar，停止已确认归属的旧 `48081` 进程。
- [ ] 将已验证 Jar 加载到 `E:\IntRuoyi` 的 `48081` 并验证健康状态。
- [ ] 完成证据、cleanup、提交推送和 worktree/slot 释放。

## Expected Verification

- 当前运行态来源检查：PID、Jar 路径、Jar SHA-256、创建时间、`origin/int_main` 提交。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkAutoPersistServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-server -am "-DskipTests" package`
- 构建 Jar 与部署 Jar SHA-256 完全一致。
- 新进程命令行加载 `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`，监听 `48081`。
- `http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。

## Current Status

in_progress

当前 `48081` 为健康状态，但运行 Jar 位于主工作区 `target`，而本地 `int_main` 与 `origin/int_main` 已分叉，无法从现有 Jar 证明其来源为最新融合提交。已在 `D:\IntRuoyiWorktree\20260727_int_main_latest_backend_runtime` 创建隔离构建 worktree，登记 slot `6`（`8087/48087`，仅构建不启动），并快进到最新 `origin/int_main` 提交 `70a4b4141187eb282a054d3b70dcbcadca641cc2`。构建验证通过前不停止当前服务。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。不会把旧 Jar、其它端口或 mock 健康状态当作最新版本成功。
- `是否从根因和长期维护角度解决`：是。以最新远端提交、构建产物哈希和运行 Jar 哈希建立可验证的来源链。
- `是否存在临时补丁或绕过`：否。不从脏主工作区构建，不修改共享源码或运行端口。

## 经验门禁

- `docs/local-runtime.md#2026-07-24-隔离构建-Jar-加载门禁`：主工作区脏时必须从 clean worktree 构建，并记录构建 Jar、部署 Jar 和运行进程来源。
- `docs/local-runtime.md#2026-07-27-本地后端标准输出阻塞与日志目录门禁`：新进程 stdout/stderr 写入稳定运行目录，不能写入待 cleanup 目录。
- `docs/powershell-memory.md#PowerShell-Maven--D-参数引号门禁`：Maven `-D` 参数整体加引号。
- `docs/worktree-restrictions.md#端口登记表规则`：附加 worktree 使用原子槽位登记；不得占用 `48081`。

## Cleanup Keep

- doc/tasks/20260727-restart-int-main-latest-backend/task.md
- doc/tasks/20260727-restart-int-main-latest-backend/execution-log.md
- doc/tasks/20260727-restart-int-main-latest-backend/verification-report.md
- doc/tasks/20260727-restart-int-main-latest-backend/ci-cd-evidence.md
