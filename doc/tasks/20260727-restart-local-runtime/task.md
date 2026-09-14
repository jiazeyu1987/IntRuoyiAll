# 20260727 Restart Local Runtime

## Task Goal

重启 `E:\IntRuoyi` 的 `int_main` 本地前端和后端运行态，并验证固定端口：

- Frontend: `http://127.0.0.1:8081/`
- Backend: `http://127.0.0.1:48081/actuator/health`

## Milestones

- [x] 读取本地运行态、端口矩阵、任务收尾和 PowerShell/Git 门禁。
- [x] 记录端口占用和旧进程归属。
- [x] 停止确认属于 `E:\IntRuoyi` 的旧前后端进程。
- [x] 启动前端和后端。
- [x] 验证前端 HTTP 入口和后端健康检查。
- [x] 完成本次任务记录。

## Expected Verification

- `8081` 监听进程归属 `E:\IntRuoyi\IntRuoyiFronted` 或启动后的前端命令。
- `48081` 监听进程归属 `E:\IntRuoyi\IntRuoyiBackend` 或启动后的后端命令。
- `Invoke-WebRequest http://127.0.0.1:8081/` 返回 HTTP 成功。
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按固定端口和运行态归属门禁重启。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 本地运行态必须使用 `int_main` 固定端口 `8081/48081`，不得随机换端口。
- 端口被旧 `int_main` 进程占用时，先记录 PID、命令行和归属依据，再停止并重启。
- 端口被未知进程、非 IntRuoyi 进程或其他 profile 占用时，必须阻塞，不得强杀。
- 后端启动必须通过 `/actuator/health` 验证 `status=UP`；前端启动必须验证 `http://127.0.0.1:8081/`。
