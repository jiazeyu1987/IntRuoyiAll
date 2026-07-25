# 启动 D-Main 本地前后端

## Task Goal

- 将当前工作区 `D:\ProjectPackage\IntRuoyi\IntRuoyiAll` 作为 `int_main_d` 启动。
- 前端端口：`8101`。
- 后端端口：`48101`。
- 后端连接本机 Docker MySQL：`127.0.0.1:23306/ruoyi-vue-pro`。
- 后端连接本机 Redis：`127.0.0.1:26379`。

## Milestones

- [x] 读取本地运行、分支端口、任务收尾、PowerShell 编码规则。
- [x] 保存启动前既有脏工作区基线提交。
- [x] 创建本次任务目录与初始任务记录。
- [x] 检查 `8101/48101` 端口占用。
- [x] 启动 Docker MySQL/Redis 依赖并验证 `23306/26379` 监听。
- [x] 补齐 D-Main 缺失的同源后端源码包并完成 `yudao-server` 打包。
- [x] 启动后端并验证 health。
- [x] 安装前端依赖、启动前端并验证入口。
- [x] 更新验证报告和最终状态。

## Expected Verification

- `scripts\preflight\branch-runtime-port-guard.ps1` 识别当前工作区为 `int_main_d`，端口为 `8101/48101`。
- `mvn.cmd -pl yudao-server -am -DskipTests package` 返回 `BUILD SUCCESS` 并生成 `yudao-server-exec.jar`。
- `http://127.0.0.1:48101/actuator/health` 返回 `UP`。
- `http://127.0.0.1:8101/` 返回 HTTP `200`。
- 监听进程命令行分别归属于 `D:\ProjectPackage\IntRuoyi\IntRuoyiAll\IntRuoyiBackend` 和 `D:\ProjectPackage\IntRuoyi\IntRuoyiAll\IntRuoyiFronted`。

## Current Status

completed

## 经验门禁

### D-Main 端口运行门禁

- Trigger: 当前 `D:\ProjectPackage\IntRuoyi\IntRuoyiAll` 本地运行、D-Main、`8101/48101`。
- Preflight check: 启动前运行端口治理预检；确认 `8101/48101` 未被未知进程占用；前端使用 `.env.branch-main-d` 或脚本注入环境变量。
- Blocker: 端口被未知进程占用、profile 未识别为 `int_main_d`、缺少后端 jar、MySQL/Redis 不可用、或 health 非 `UP`。
- Verification: 记录端口监听 PID、命令行、后端 health、前端 HTTP 状态。
- Forbidden action: 不改写共享 `.env` 或 `application-local.yaml`，不静默换端口，不强杀未知进程。
- Evidence: `docs\local-runtime.md`、`docs\branch-runtime-ports.md`、`scripts\runtime\start-branch-frontend.ps1`、`scripts\runtime\start-branch-backend.ps1`。

## Verification Evidence

- 启动前既有脏工作区基线提交：`7c21f74d chore: baseline d main runtime port contract`。
- Runtime profile 预检通过：`int_main_d`，前端 `8101`，后端 `48101`。
- 启动前端口检查：`8101/48101` 未监听；Docker MySQL `23306` 与 Redis `26379` 已监听。
- 后端初始 RED：`yudao-module-bpm` 缺少 `formcenter/runtime` 包，`yudao-module-erp` 缺少 `service/sync/runtime` 包。
- 已从 `E:\IntRuoyi` 同源同步 BPM runtime 4 个文件和 ERP sync runtime 6 个文件，未引入空实现、fallback 或吞异常逻辑。
- 后端 GREEN：`mvn.cmd -pl yudao-server -am -DskipTests package` -> `BUILD SUCCESS`，生成 `IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`。
- 后端运行验证：`48101` 监听 PID `29624`，`/actuator/health` status = `UP`。
- 前端依赖：`pnpm install --frozen-lockfile --reporter append-only` -> PASS，安装 `vite 5.1.4`。
- 前端运行验证：`8101` 监听 PID `43336`，Vite `branch-main-d` ready，`http://127.0.0.1:8101/` -> HTTP `200 OK`。

## Blockers

- 当前工作区另有未跟踪目录 `doc/tasks/20260725-dcc-controlled-file-logs-import/`，不属于本任务，未修改。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，补齐同源缺失源码并按 D-Main 独立 runtime profile 启动，不改默认端口配置。
- `是否存在临时补丁或绕过`：否。
## Closeout Evidence

- `task-closeout-cleanup --mode preview`：PASS，无删除项、无 blocker。
- `task-closeout-cleanup --mode apply`：PASS，无删除项、无 blocker。
- 实现提交：`e12e865c fix: restore d main runtime source packages`。
- 推送状态：`HEAD` 与 `origin/int_main` 一致（`e12e865c7c8dfdebc74c77b58881895288357df3`）。
