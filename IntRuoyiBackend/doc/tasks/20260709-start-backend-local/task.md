# 20260709-start-backend-local

## Task Goal
启动 IntRuoyi 本机后端服务，优先使用项目已有脚本和真实本地配置，不引入 fallback、mock 或静默降级。

## 经验门禁
- PowerShell 命令执行前已读取 `docs/powershell-memory.md`，中文与多行命令按 UTF-8 处理。
- 本任务仅启动本机后端，不操作服务器、不修改正式/测试环境数据。
- 后端路径固定为 `ruoyi-vue-pro`，本地启动优先使用仓库内既有脚本。

## Milestones
- [x] 确认本机启动前置：后端目录、Maven、Java。
- [x] 启动本地后端进程。
- [x] 验证端口/健康响应或启动日志。

## Expected Verification
- 本机后端进程存在。
- 启动日志出现 `Started YudaoServerApplication` 或本地 HTTP 探活成功。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，优先使用项目既有启动脚本和真实本地配置。
- 是否存在临时补丁或绕过：否。

## Current Status
completed: 后端已运行，端口 48081 正在监听，/actuator/health 返回 UP；当前监听进程 66416。

