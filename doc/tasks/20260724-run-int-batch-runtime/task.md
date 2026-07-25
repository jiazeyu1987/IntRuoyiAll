# 运行 int_batch 前后端程序

## Task Goal

启动当前 `int_batch` 工作区的后端和前端本机服务，并验证矩阵端口可访问。

## Milestones

- [x] 读取本机运行、端口、worktree、前后端与任务收尾规则
- [x] 检查 `int_batch` 端口占用与启动脚本
- [x] 启动后端 `48041` 与前端 `8041`
- [x] 验证后端健康检查与前端入口
- [x] 记录启动命令、PID、端口和验证结果

## Expected Verification

- 后端健康检查 `http://127.0.0.1:48041/actuator/health` 返回可用状态。
- 前端入口 `http://127.0.0.1:8041/` 返回可访问页面。
- 任务日志记录端口检查、启动命令、PID 与验证输出。

## Current Status

running_verified

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，后端沿用 `E:\IntRuoyi` 已验证的 Docker MySQL 运行参数，显式覆盖至 `127.0.0.1:23306/ruoyi-vue-pro`，未修改共享配置。
- `是否存在临时补丁或绕过`：否
