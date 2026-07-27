# 批记录表单与表单槽位术语边界沉淀

## Task Goal

将工艺路线中的“工序开始”“批记录表单”“表单槽位”固化为项目级术语契约，避免后续开发把三条独立配置链路混用。

## Milestones

- [x] 确认用户定义的三类配置边界。
- [x] 将术语边界写入根 `AGENTS.md`。
- [x] 完成结构、编码和 Git 门禁验证。
- [ ] 完成任务提交、集成、推送和收尾清理。

## Expected Verification

- 使用 UTF-8 重新读取新增规则。
- 使用 `rg` 验证三类术语、数据来源和禁止混用规则均存在。
- 运行 `git diff --check`。
- 运行 `scripts\preflight\branch-runtime-port-guard.ps1`。
- 提交并推送到 `origin`，确认分支不领先远端。

## Applicable Experience Gate

- 已读取 `docs\experience-index.md`。
- 命中 `docs\e2e-rules.md#edhr-右侧红框元信息隐藏门禁`：`batchRecordFormNames` 必须使用显式来源匹配，不得把缺少类型的其它表单默认归入 `MAIN`。
- 本任务将更上层的业务术语边界写入根 `AGENTS.md`，固定“工序开始”“批记录表单”“表单槽位”三条独立数据链路。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；通过项目级术语契约固定数据来源与禁止混用边界。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout
