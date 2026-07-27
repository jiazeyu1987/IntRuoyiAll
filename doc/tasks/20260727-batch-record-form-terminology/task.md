# 批记录表单与表单槽位术语边界沉淀

## Task Goal

将工艺路线中的“工序开始”“批记录表单”“表单槽位”固化为项目级术语契约，避免后续开发把三条独立配置链路混用。

## Milestones

- [x] 确认用户定义的三类配置边界。
- [x] 将术语边界写入根 `AGENTS.md`。
- [x] 完成结构、编码和 Git 门禁验证。
- [x] 完成术语规则提交并推送任务分支。
- [x] 按用户明确授权，将术语提交 fast-forward 推送到远端 `int_main`。
- [ ] 等待本地主工作区并发改动完成后，同步本地 `int_main` 并执行收尾清理。

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

- `是否引入 fallback/降级/吞异常`：是，仅限 Git 集成流程，不涉及产品运行态。用户于 2026-07-27 明确授权在保留主工作区并发改动的前提下，绕过本地 clean worktree 门禁，直接 fast-forward 推送远端 `int_main`。
- `是否从根因和长期维护角度解决`：是；通过项目级术语契约固定数据来源与禁止混用边界。
- `是否存在临时补丁或绕过`：是，仅为本次远端集成绕过。触发条件是本地主工作区持续存在其它任务改动；风险是本地 `int_main` 暂时落后于远端；移除方式是在并发任务完成后将本地 `int_main` fast-forward 到远端，再按标准 cleanup 流程删除 worktree 并释放 slot 4。禁止 force push 或覆盖并发改动。

## Current Status

ready_for_closeout

## Remaining Blocker

- `task-closeout-cleanup` preview 检测到主工作区 `E:\IntRuoyi` 存在其它任务的未提交改动，按门禁禁止自动执行 `ff-only` 合并和 worktree 删除。
- 术语规则已进入远端 `int_main`；必须等待主工作区恢复干净后，同步本地 `int_main` 并完成 cleanup，不得提交、覆盖或清理这些并发改动。
