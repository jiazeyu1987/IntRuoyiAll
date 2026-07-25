# 融合 int_shedule 最新代码并重启前后端

## Task Goal

在当前 `int_batch` 工作区融合 `int_shedule` 最新代码，按 `int_batch` 端口矩阵重启前后端，并用 Playwright E2E 访问主页验证可用。

## Milestones

- [x] 创建任务记录并读取合并、运行、E2E 规则
- [x] 检查 Git 分支、远端、脏工作区和 `int_shedule` 来源状态
- [x] 获取并融合 `int_shedule` 最新代码
- [x] 构建并按 `8041/48041` 重启前后端
- [x] 使用 Playwright 真实浏览器访问主页并记录证据

## Expected Verification

- `scripts\preflight\branch-runtime-port-guard.ps1` 通过。
- 后端健康检查 `http://127.0.0.1:48041/actuator/health` 返回 `{"status":"UP"}`。
- 前端入口 `http://127.0.0.1:8041/` 返回 HTTP 200。
- Playwright E2E 能打开主页并记录页面标题或可见状态。

## Current Status

running_verified

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，已完成真实合并、分支端口重启、Docker MySQL/Redis 依赖验证和 Playwright 页面访问。
- `是否存在临时补丁或绕过`：否

## 经验门禁

### 本机 Docker MySQL 连接门禁

- Trigger: 后端本机启动、`start-branch-backend.ps1`、`application-local.yaml` 指向 `127.0.0.1:3306` 或出现 root 认证失败。
- Preflight check: 启动前确认 `int-ruoyi-mysql` 映射 `127.0.0.1:23306 -> 3306`，`int-ruoyi-redis` 映射 `127.0.0.1:26379 -> 6379`。
- Blocker: `23306/26379` 容器映射缺失、健康检查失败、或试图改共享 `application-local.yaml`。
- Verification: 后端健康检查返回 `{"status":"UP"}`，启动日志包含 `项目启动成功！`。
- Forbidden action: 不得回退到本机 `3306`、猜测 root 密码、静默换端口或跳过后端。
- Evidence: `docs/local-runtime.md#2026-07-25-本机-docker-mysql-连接门禁`。

### 分支运行端口门禁

- Trigger: 合并分支后启动本机前后端或运行 E2E。
- Preflight check: 当前 `int_batch` 只能使用前端 `8041`、后端 `48041`，合并后运行 `scripts\preflight\branch-runtime-port-guard.ps1`。
- Blocker: 端口被未知进程占用、运行配置漂移、或非 `int_main` 使用 `8081/48081`。
- Verification: 记录端口监听 PID、前端 HTTP 200、后端健康检查 UP。
- Forbidden action: 不得改共享 `.env` 或 `application-local.yaml` 抢端口，不得静默换端口。
- Evidence: `docs/branch-runtime-ports.md`、`docs/local-runtime.md`。

### Playwright 真实路径门禁

- Trigger: 用户要求 E2E 访问主页。
- Preflight check: 使用 Playwright 操作真实前端页面，访问 `http://127.0.0.1:8041/`，先确认 `npx` 可用。
- Blocker: 前端入口不可达、Playwright 浏览器不可用、或用 API-only 替代页面访问。
- Verification: 记录 Playwright 命令、目标 URL、页面标题或可见状态、截图路径。
- Forbidden action: 不得用接口健康检查冒充 E2E 页面验收。
- Evidence: `docs/e2e-rules.md`、`C:\Users\BJB110\.codex\skills\playwright\SKILL.md`。
