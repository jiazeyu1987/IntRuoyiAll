# Restart Local Frontend And Backend

## Task Goal

重启 `E:\IntRuoyi` 主工作区 `int_main` 本地前端与后端，保持固定端口 `8081/48081`，并验证两个运行入口可用。

## Milestones

- [x] 读取任务、PowerShell、本地运行态与 worktree 规则。
- [x] 建立任务记录并识别适用经验门禁。
- [ ] 检查依赖、端口占用与旧进程归属。
- [ ] 使用标准本地脚本重启前后端。
- [ ] 验证后端 health 与前端 HTTP 入口。
- [ ] 更新验证报告与任务状态。

## Expected Verification

- `int_main` 运行配置解析为前端 `8081`、后端 `48081`。
- 端口旧监听进程必须能确认属于 `E:\IntRuoyi` 的同 profile 运行态。
- 标准本地重启脚本成功完成调度。
- `GET http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。
- `GET http://127.0.0.1:8081/` 返回 HTTP `200`。

## Current Status

in_progress

## Applicable Gate Summary

- 使用 `int_main` 固定端口 `8081/48081`，禁止随机换端口或停止未知进程。
- 标准脚本必须解析到 `E:\IntRuoyi\IntRuoyiFronted`，后端必须从稳定运行目录 Jar 启动。
- 启动前确认 Docker 依赖、Java、Maven、pnpm 和端口归属；缺少前置条件时 fail fast。
- 当前仓库存在多个并行任务脏改动，本任务不暂存、提交、回滚或清理这些文件。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；使用项目正式运行脚本和固定端口契约。
- `是否存在临时补丁或绕过`：否。
