# 20260725 Codex Runner 本机环境修复

## Task Goal

修复测试管理运行时提示“没有 codex 环境”的问题，将 Codex Runner 配置为使用本机可运行的 Codex CLI 环境，并验证测试管理可以完整运行且生成执行记录。

## Milestones

- [ ] 保存任务开始前既有脏工作区基线。
- [ ] 定位 Codex Runner 环境探测、配置读取和报错来源。
- [ ] 补充回归测试，先复现“没有 codex 环境”失败。
- [ ] 修改 Runner 配置/探测逻辑，使其使用本机 Codex CLI。
- [ ] 运行测试管理真实/集成验证，确认执行记录落库或页面可见。
- [ ] 完成 verification-report、清理、提交与推送。

## Expected Verification

- 本机命令行可定位并运行 `codex`。
- Codex Runner 不再报“没有 codex 环境”。
- 测试管理发起执行后，Runner 能领取并执行测试项。
- 系统执行记录中出现本次运行记录，并包含最终状态/检查点结果。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是修复 Runner 对本机 Codex 环境的配置与探测，而不是把缺失环境伪装为成功。
- `是否存在临时补丁或绕过`：否。

