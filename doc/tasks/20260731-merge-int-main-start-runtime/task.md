# 20260731 merge int_main and start branch runtime

## Task Goal

将 `int_main` 融合到当前 `int_batch` 工作区，并启动本工作区前后端运行态。

## Milestones

- [x] 创建任务目录并读取触发规则。
- [x] 合并前确认 Git 分支、远端、工作区状态和 `int_main` 来源。
- [x] 将 `int_main` 合并到 `int_batch`，处理合并结果并运行端口矩阵保护检查。
- [x] 按 Java 17 基线重新编译合并后的后端源码。
- [x] 重算正常三方合并树并修复原合并提交遗漏的 2405 个路径。
- [x] 按 `int_batch` 端口矩阵启动后端 `48041` 和前端 `8041`。
- [x] 验证后端健康检查、前端入口和任务记录。

## Expected Verification

- `git status --short --branch`
- `git fetch origin int_main`
- `git merge origin/int_main`
- `scripts\preflight\branch-runtime-port-guard.ps1`
- 端口占用检查：`8041`、`48041`
- Docker 依赖检查：MySQL `23306`、Redis `26379`
- 后端健康检查：`http://127.0.0.1:48041/actuator/health`
- 前端入口检查：`http://127.0.0.1:8041/`

## Current Status

ready_for_closeout

## Resolved Blocker

- 初始阻塞为本机只有 JDK 21，而项目要求 Java 17；在 JDK 21 下 `yudao-module-bpm` 报 Lombok 生成成员不可见。
- 已从 Microsoft Build of OpenJDK 官方下载入口取得 JDK 17.0.20，SHA-256 校验通过，并以 `C:\Users\BJB110\.jdks\jdk-17.0.20+8` 作为本任务明确 Java 17 工具链。
- JDK 17 下 `mvn.cmd -pl yudao-module-bpm -DskipTests -Dmaven.compiler.proc=full -Dmaven.compiler.useIncrementalCompilation=false clean compile` 通过；未使用旧 jar。
- Java 17 完整打包进一步暴露原合并提交遗漏 1922 个新增路径和 483 个修改路径；已从合并前两端重算正常三方合并树并回填全部 2405 个路径。
- 4 个任务开始前已存在、被 `.gitignore` 隐藏且与 `int_main` 不同的 ERP 源码已按本地正式版本保留，未被覆盖。
- Java 17 完整 `yudao-server` 打包已通过，生成的可执行 Jar 大小为 `501172025` 字节。
- 分支后端启动脚本已按本地运行门禁改为复制并运行 `output\runtime\int_batch` 下的独立 Jar；启动后再次执行 Maven 打包，后端 PID、运行 Jar 和健康状态保持不变。
- 后端 `http://127.0.0.1:48041/actuator/health` 返回 `UP`，前端 `http://127.0.0.1:8041/` 返回 HTTP `200`。

## Design Constraint Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，按项目端口矩阵和本地运行脚本执行。
- 是否存在临时补丁或绕过：否。

## Experience Gate Summary

- 已读取 `docs\experience-index.md`，本任务命中 Git/PowerShell、本地 runtime、分支端口矩阵、Docker MySQL/Redis 依赖和 closeout 门禁。
- 启动 `int_batch` 必须使用前端 `8041`、后端 `48041`，不得改写共享 `.env` 或 `application-local.yaml` 抢占端口。
- 后端启动前必须确认本机 Docker MySQL `23306` 与 Redis `26379` 可用；缺失时 fail fast。
- 合并 `int_main` 后必须运行 `scripts\preflight\branch-runtime-port-guard.ps1`。

## Cleanup Candidates

- .runtime/20260731-merge-int-main-start-runtime/

## Cleanup Keep

- doc/tasks/20260731-merge-int-main-start-runtime/bug-regression-evidence.md
