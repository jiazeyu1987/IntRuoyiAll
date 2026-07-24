# 20260614 启动本机后端

## 任务目标

确认并启动 IntRuoyi 本机后端服务，使 `http://127.0.0.1:48081/actuator/health` 可访问并返回健康状态。

## 前置任务检查

- 最近后端任务：`20260613-showroom-awards-import-display`。
- 状态：`BLOCKED_FINAL_E2E`。
- 阻塞原因：缺少有效阿里云 NLS token，导致奖项语音生成真实 E2E 无法完成。
- 影响评估：该阻塞与本机后端进程启动和健康检查无直接依赖；旧任务已在任务文档中明确记录阻塞和影响，本任务仅做本机运行状态确认。

## 里程碑

1. M1 检查：确认后端目录、启动脚本和当前端口监听状态。
2. M2 启动/确认：若后端未运行则启动；若已运行则保持现有健康进程。
3. M3 验证：访问 `/actuator/health` 并确认返回 `UP`。
4. M4 收尾：记录执行日志和清理预览结果。

## 预期验证

- `Get-NetTCPConnection -LocalPort 48081` 显示本机监听进程。
- `Invoke-WebRequest http://127.0.0.1:48081/actuator/health` 返回 HTTP 200。
- 健康响应内容为 `{"status":"UP"}`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；若端口无监听或健康检查失败，应直接报告失败。
- `是否从根因和长期维护角度解决`：是；优先使用项目现有本机运行入口和健康检查，不新增临时启动脚本。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：COMPLETED。
- 已完成：初始检查时本机 48081 曾有 Java 后端进程监听；提交后复查发现健康接口不可连接，随后使用项目现有 `script\deploy\restart-ruoyi-local-component.ps1 -Component backend` 启动本机后端成功。最终 48081 监听进程为 PID `42628`，健康检查返回 HTTP 200 和 `UP`。
- 验证证据：详见 `execution-log.md`。
- 剩余阻塞：无。
