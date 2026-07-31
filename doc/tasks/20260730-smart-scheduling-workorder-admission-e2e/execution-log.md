# 智能排产工单入池真实 E2E 执行日志

## Context

- Task id: `20260730-smart-scheduling-workorder-admission-e2e`
- User intent: 在租户 `1` 下通过 Playwright 真实浏览器验证固定工单 `881MO093613`、`881MO093615` 的入池、列表核对和删除清理闭环。
- Scope: 前置删除残留、工单入池、仅两条结果核对、最终删除并确认无结果。

## Rules And Skills Read

- Skill: `playwright`
- `docs/task-closeout-rules.md`
- `docs/e2e-rules.md`
- `docs/login-access.md`
- `docs/local-runtime.md`
- `docs/worktree-restrictions.md`
- `docs/powershell-encoding.md`
- `docs/powershell-memory.md`
- `docs/experience-index.md`

## BDD Scenarios

- BDD: 固定排产记录前置复位 -> Given 排产列表可能存在固定工单 `881MO093613`、`881MO093615` 的排产记录; When 用户按固定工单搜索并逐条使用页面删除入口清理; Then 再次搜索两个固定工单均无结果，否则立即阻塞。
- BDD: 仅固定工单入池 -> Given 工单入池页存在两个固定工单; When 用户按可见业务唯一文本逐行勾选并确认选中集合完全等于固定集合后提交; Then 系统确认入池且没有勾选其他工单。
- BDD: 入池结果可查且唯一 -> Given 两个固定工单已确认入池; When 用户回到排产列表搜索固定工单; Then 列表只出现两条固定工单对应的排产记录。
- BDD: 入池记录最终清理 -> Given 两条固定排产记录已完成核对; When 用户逐条通过页面删除并再次搜索; Then 两个固定工单均无排产记录残留。

## Evidence

- Preflight: 根仓库分支 `int_main`，工作区 clean，本地分支较 `origin/int_main` ahead 2。
- Preflight: `npx` 可用，路径为 `D:\Programs\npx.ps1`。

## Current Status

in_progress
