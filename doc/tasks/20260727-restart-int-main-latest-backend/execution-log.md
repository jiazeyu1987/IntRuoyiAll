# Execution Log

## User Intent

用户要求确认当前后端是否为融合后的最新版本；若不是，则重启为最新 `int_main` 后端。

## BDD

- `BDD: Latest merged backend runs on int_main port -> Given` 当前 `48081` Jar 来源无法证明等于最新 `origin/int_main`，`When` 从最新远端提交隔离构建并重启，`Then` 运行 Jar 与构建 Jar 哈希一致且 health 为 `UP`。
- `BDD: Existing backend is not stopped before artifact validation -> Given` 当前 `48081` 仍健康，`When` 最新构建尚未通过，`Then` 不停止旧进程。
- `BDD: Unknown process is never killed -> Given` `48081` 被进程占用，`When` 命令行不属于 `E:\IntRuoyi` 的 `int_main` 后端，`Then` fail-fast，不停止进程。

## RED/GREEN Evidence

- `GREEN: experience-preflight -> PASS`，已读取隔离构建 Jar、稳定日志目录、PowerShell Maven 参数和 worktree slot 门禁。
- `RED: runtime provenance check -> FAIL`，当前 PID `61040` 加载主工作区 `target` Jar，但本地 `HEAD` 与 `origin/int_main` 分叉，现有 Jar 缺少可验证的远端提交来源。
- `GREEN: isolated source preparation -> PASS`，构建 worktree `D:\IntRuoyiWorktree\20260727_int_main_latest_backend_runtime` 已登记 slot `6`（`8087/48087`），分支已快进到最新 `origin/int_main` 提交 `70a4b4141187eb282a054d3b70dcbcadca641cc2`，工作树干净。
- `RED: post-build origin drift check -> FAIL`，首次打包后 `origin/int_main` 从 `70a4b414` 前进到 `97ecf51a`，且包含后端源码变更；旧构建产物未部署，旧 PID `61040` 保持运行。
- `GREEN: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkAutoPersistServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" clean test -> PASS`，最新后端源码 clean rebuild，Tests run: `4`，Failures: `0`，Errors: `0`，Skipped: `0`。
- `GREEN: mvn.cmd -pl yudao-server -am "-DskipTests" package -> PASS`，30 个 reactor 模块全部 `SUCCESS`，生成 `yudao-server-exec.jar`。
- `GREEN: artifact provenance -> PASS`，构建提交 `97ecf51a1c1f930a6c9307646614418d4ab811dc`；构建 Jar 大小 `500645246` bytes，SHA-256 `89EB3023737BD704B92AB129C2D9176C392A6B7CE4D1E2DF2199128D02FCD98D`。
- `GREEN: latest backend tree gate -> PASS`，部署前和部署后最新 `origin/int_main` 为 `177ebefbb9195835ac47d55067306454c17644da`；构建提交与远端提交的 `IntRuoyiBackend` tree 均为 `7c5ffc135ce21d4905b7b46d9747dee382578c51`，后续提交仅修改任务文档。
- `GREEN: old runtime backup -> PASS`，旧 PID `61040` 的命令行归属 `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`；旧 Jar SHA-256 `7A3F2A015A0816D9F6876DBAAE4D99DB1619F7C5011E79E0EF7D72AE43A7DA0C`，备份为 `E:\IntRuoyi\.runtime\int-main-backend\backups\yudao-server-exec-20260727-202427-7A3F2A015A08.jar`，备份哈希一致。
- `GREEN: deployed artifact equality -> PASS`，部署路径 `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar` 的 SHA-256 与构建 Jar 完全一致。
- `GREEN: runtime restart -> PASS`，新 PID `44372`，命令行加载 E 主工作区部署 Jar，参数为 `--server.port=48081 --spring.profiles.active=local`；stdout/stderr 重定向到稳定目录 `E:\IntRuoyi\.runtime\int-main-backend`。
- `GREEN: runtime verification -> PASS`，`/actuator/health` 返回 `UP`；未登录调用 `/admin-api/system/auth/get-permission-info` 在 `164 ms` 内返回 HTTP `200` 和业务码 `401`，证明 API 链路可响应且未伪造登录成功。
- `GREEN: project-experience-consolidation -> PASS`，已将“构建后再次 fetch，并以 `IntRuoyiBackend` tree 判定是否必须重建”的通用门禁合并到现有 `docs/local-runtime.md#2026-07-24-隔离构建-Jar-加载门禁`，未新建长期经验文档。
- `GREEN: validate_cicd_environment.py -> PASS`，`ci-cd-evidence.md` 满足环境、命令、产物、验证、审批、secret owner 和 rollback 证据合同。
- `BLOCKER: task-closeout-cleanup preview -> Main worktree is dirty`，preview 的 keep 列表仅包含四个任务文档、delete 为空；`E:\IntRuoyi` 存在其他并行任务的未提交改动，不能执行 ff-only merge 或删除 linked worktree。当前任务不得提交、暂存、stash、回滚或删除这些无关改动。
- `GREEN: task evidence commit -> PASS`，提交 `3a7f795fd3c17e43cc00e60e3a2ef4b283e5d396` 仅包含四个任务记录和 `docs/local-runtime.md` 经验门禁。
- `GREEN: git push origin codex/restart-int-main-latest-backend-20260727 -> PASS`，远端任务分支已创建并与本地同步。
- `BLOCKER: post-commit task-closeout-cleanup preview -> Main worktree is dirty`，工作树已无任务未提交文件、delete 仍为空，但 `E:\IntRuoyi` 的无关并行脏改动继续阻塞 ff-only merge、apply、worktree 删除和 slot 释放。
- `GREEN: final runtime recheck -> PASS`，2026-07-27 20:33:12 后运行态被并行流程再次启动为 PID `46388`；命令行路径、`48081` 参数和 local profile 均未变化，Jar SHA-256 仍为 `89EB3023737BD704B92AB129C2D9176C392A6B7CE4D1E2DF2199128D02FCD98D`，health 仍为 `UP`。

## Current Evidence

- Current listener at final recheck: `48081 -> PID 46388`.
- Current command: Java loads `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`.
- Current health: `UP`.
- Local Git state: `int_main` contains unrelated concurrent dirty changes and differs from `origin/int_main`; do not build from the main workspace.
- Clean build source: `97ecf51a1c1f930a6c9307646614418d4ab811dc`.
- Latest verified origin commit: `177ebefbb9195835ac47d55067306454c17644da`.
- Verified backend tree: `7c5ffc135ce21d4905b7b46d9747dee382578c51`.
- Runtime Jar SHA-256: `89EB3023737BD704B92AB129C2D9176C392A6B7CE4D1E2DF2199128D02FCD98D`.

## Blockers

- Runtime implementation and verification have no blocker.
- Closeout blocker: `E:\IntRuoyi` 主工作区存在无关并行脏改动，cleanup preview 已按规则阻塞 ff-only merge；在主工作区恢复干净前，构建 worktree 和 slot `6` 必须保留。
- Pushed task branch: `origin/codex/restart-int-main-latest-backend-20260727`.
