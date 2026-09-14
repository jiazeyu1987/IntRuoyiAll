# 20260804 Restart Local Frontend Backend

## Task Goal

重启 `E:\IntRuoyi` 主工作区 `int_main` 本机前端与后端运行态，保持固定端口前端 `8081`、后端 `48081`，并验证前端入口和后端健康检查可用。

## Milestones

- [x] 读取本地运行、worktree、端口、PowerShell 编排和任务收尾规则。
- [x] 检查 `8081/48081` 端口占用和项目脚本入口。
- [x] 执行标准本机重启脚本启动后端与前端。
- [x] 修复后端 Spring `@Resource` 启动阻塞，并以回归测试覆盖。
- [x] 验证 `http://127.0.0.1:48081/actuator/health` 为 `UP`，`http://127.0.0.1:8081/` 返回 HTTP `200`。
- [x] 记录运行 PID、日志路径、验证结果和未完成事项。

## Expected Verification

- `Get-NetTCPConnection` 确认 `8081/48081` 端口归属。
- `IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full` 成功派发本机重启。
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。
- `Invoke-WebRequest http://127.0.0.1:8081/` 返回 HTTP `200`。

## Current Status

ready_for_closeout

本机 `int_main` 前后端运行态已恢复：后端 `http://127.0.0.1:48081/actuator/health` 返回 `UP`，前端 `http://127.0.0.1:8081/` 返回 HTTP `200`。任务文档已更新到收尾前状态；仓库存在既有大量并行脏改动且 `int_main...origin/int_main [ahead 9]`，本次未提交或推送。

## Applicable Experience Gates

- 本地运行固定端口：`int_main` 只能使用前端 `8081`、后端 `48081`，禁止随机换端口或跳过任一服务。
- 本地重启脚本路径门禁：标准脚本必须解析前端根目录为 `E:\IntRuoyi\IntRuoyiFronted`，不能创建旧路径假目录绕过。
- tokenless Runner 门禁：后端重启必须清理 `CODEX_TEST_RUNNER_TOKEN`，不得生成或注入 runner token。
- 本地后端数据库凭据门禁：后端只有在真实本地数据库、Redis 和运行配置满足脚本前置时才可宣称启动成功。
- 本地前端 pnpm 链接门禁：前端依赖缺失或 Vite 运行依赖损坏时必须阻塞，不得跳过前端启动。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本次仅使用项目标准本机重启脚本与固定端口契约。
- `是否存在临时补丁或绕过`：否。
