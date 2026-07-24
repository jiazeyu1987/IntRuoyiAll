# 任务：运行控制台全按钮真实数据 E2E（后端）

## 目标

在本任务后端 worktree 中承接运行控制台全按钮真实数据 E2E 的后端接口、脚本、权限和运维动作验证。主任务记录见根 worktree `doc/tasks/20260529-runtime-control-all-buttons-e2e/`。

## 里程碑

- [x] 后端 worktree 已创建：`codex/20260529-runtime-control-all-buttons-e2e`。
- [x] 建立后端任务记录。
- [x] 执行运行控制台后端真实接口和按钮触发证据核查。
- [x] 如按钮链路有后端缺陷，在测试租户修复并回归。
- [x] 支持 `芋道源码/admin` 最终验证。

## 当前状态

completed

## Current Status

completed

## 验证结果

- 后端 `48081` 健康，运行控制台真实接口参与 `runtime-control-all-buttons-real.e2e.js` 全按钮验证。
- 发布、标记测试通过、上线正式服使用不存在的 `e2e_missing_*` ReleaseTag 创建真实 operation，最终失败并写入日志，未执行真实部署。
- 回滚和恢复按钮使用服务端候选门禁，候选缺失 manifest/checksum/rehearsal/snapshot 时正确阻断。
