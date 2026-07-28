# 20260728 Restart Local Runtime

## Task Goal

重启 `E:\IntRuoyi` 主工作区 `int_main` 本地前端与后端运行态，保持固定端口 `8081/48081`，并验证前端入口与后端健康检查可用。

## Milestones

- [x] 读取任务、PowerShell、端口与本地运行态规则。
- [x] 检查当前端口占用与旧进程归属。
- [x] 使用标准本地重启脚本重启 backend + frontend。
- [ ] 验证 `http://127.0.0.1:48081/actuator/health` 与 `http://127.0.0.1:8081/`。
- [x] 更新任务证据与最终状态。

## Expected Verification

- `git status --short --branch` 已记录现有脏工作区，不混入本任务外文件。
- 端口 `48081` 与 `8081` 的监听 PID、命令行和归属已记录。
- 标准重启脚本返回成功，且未出现前端路径、Runner token、依赖服务、Jar 稳定性或端口归属阻塞。
- 后端 health 返回 `status=UP`。
- 前端入口返回 HTTP `200`。

## Current Status

blocked

## Blocker

- 标准 full 重启脚本失败：必需 Docker 容器 `int-ruoyi-mysql` 未运行。
- 只读 Docker 状态：`int-ruoyi-mysql` 存在但为 `Exited (255) 35 minutes ago`。
- Impact: 后端无法完成本地数据库前置条件检查，脚本在启动 backend 前 fail fast；frontend 未被 full 脚本继续启动，避免把部分启动误报为前后端完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；使用项目标准重启脚本与固定端口契约，不修改端口或配置。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

### 本地重启脚本路径门禁

- Trigger: `restart-int-ruoyi-local.ps1`、本地前后端重启、`IntRuoyiFronted`。
- Preflight check: 执行脚本前确认前端根目录为 `E:\IntRuoyi\IntRuoyiFronted`。
- Blocker: 脚本报缺失旧路径 `E:\IntRuoyi\yudao-ui-admin-vue3` 时必须停止，不得创建假目录、软链、换端口或静默跳过前端。
- Verification: 记录脚本输出、端口归属、后端 health、前端 HTTP 状态。
- Forbidden action: 禁止绕过硬编码路径、强杀未知进程或用 API-only 验证冒充前端成功。

### 标准本地后端重启 Runner Token 门禁

- Trigger: `restart-int-ruoyi-local.ps1 -Component backend/full`。
- Preflight check: token 文件必须位于 `.runtime/codex-test-runner/runner-token.txt`，非空且被 Git 忽略。
- Blocker: token 文件为空、未被 Git 忽略、Runner 注册或 heartbeat 无法证明有效时，不宣称 Runner 链路成功。
- Verification: 记录 token 文件存在性与 ignore 检查，不记录 token 明文。
- Forbidden action: 禁止记录 token、每次临时生成不同 token 或只以进程存在证明 Runner 可用。

### 本地后端运行 Jar 不可变门禁

- Trigger: 本地后端重启、Maven package、`48081` 长期运行 Jar。
- Preflight check: 运行 Jar 必须复制到 `output\runtime\<profile>\` 稳定目录后启动。
- Blocker: 监听进程直接引用 Maven `target` Jar，或运行 Jar 修改时间晚于进程启动时间时必须阻塞。
- Verification: 记录运行 Jar 路径、PID、health 状态。
- Forbidden action: 禁止长期运行进程直接使用会被 Maven 覆盖的 `target` Jar。
