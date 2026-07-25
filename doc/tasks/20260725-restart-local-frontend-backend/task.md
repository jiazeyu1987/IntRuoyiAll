# 20260725 Restart Local Frontend Backend

## Task Goal

重启 `E:\IntRuoyi` 当前 `int_main` 本地前后端程序，保持端口契约：前端 `8081`，后端 `48081`。

## Milestones

- [ ] 读取本地运行、端口与任务收尾规则
- [ ] 记录端口归属并停止确认属于本项目的旧进程
- [ ] 启动后端并验证 `/actuator/health`
- [ ] 启动前端并验证本地入口
- [ ] 记录验证证据与最终状态

## Expected Verification

- `http://127.0.0.1:48081/actuator/health` 返回可用状态。
- `http://127.0.0.1:8081/` 返回 HTTP 可访问状态。
- 端口占用进程可归属到 `E:\IntRuoyi` 当前工作区。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务仅执行本地运行态重启，不改动源码或配置。
- `是否存在临时补丁或绕过`：否。
