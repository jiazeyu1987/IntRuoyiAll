# 重启本机最新后端

## Task Goal

将本机 `int_main` 后端 `48081` 重启到当前 `origin/int_main` 最新提交，确保刚修复的重排班次小时默认值逻辑加载到运行态。

## Milestones

- [x] 建立任务记录并读取本机运行态门禁。
- [x] 从干净 `origin/int_main` 构建最新后端 Jar。
- [x] 停止可确认归属的旧 `48081` 后端并启动新 Jar。
- [x] 验证 `48081` health 为 `UP`，记录新 PID、Jar 路径和提交。

## Expected Verification

- `git rev-parse origin/int_main`
- `mvn -pl yudao-server -am "-DskipTests" package`
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health`
- `Get-NetTCPConnection -LocalPort 48081 -State Listen`

## Applicable Gates

- `docs/local-runtime.md`：`int_main` 后端固定使用 `48081`，旧进程确认归属后才可停止。
- `docs/worktree-restrictions.md`：临时 build worktree 只能放在 `D:\IntRuoyiWorktree\`；不启动服务则不占用槽位。
- `docs/backend-development.md`：后端构建使用 Maven reactor。
- `docs/powershell-encoding.md`：中文任务文档使用 UTF-8。

## Current Status

completed

最新后端已启动并通过健康检查；任务收尾清理、临时 worktree 删除和经验沉淀已完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；只做本机后端重启。
- `是否从根因和长期维护角度解决`：是；从干净 `origin/int_main` 构建，避免混入主工作区并行脏改动。
- `是否存在临时补丁或绕过`：否。
