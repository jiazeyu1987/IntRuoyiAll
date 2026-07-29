# 20260729 删除 eDHR 填写页软键盘按钮

## Task Goal

按用户最新要求删除 eDHR 批记录填写页左侧红框位置的软键盘按钮及其页面内自定义软键盘实现，不修改后端接口、保存/提交链路、填写模式或辅助模式业务逻辑。

## Milestones

- [x] 读取前端、任务收尾、PowerShell/Git 和 frontend-feature-delivery 规则
- [x] 保存任务前并发脏工作区基线
- [x] 将软键盘静态合同改为删除口径并先 RED
- [x] 删除 `ExecutionPage.vue` 中软键盘模板、状态、事件处理和样式
- [x] 运行聚焦静态合同、相邻合同和类型检查
- [ ] 收尾清理、提交并推送

## Expected Verification

- `node tests/e2e/edhr-soft-keyboard-button-static.spec.js`
- `node tests/e2e/edhr-assist-fill-mode-static.spec.js`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260729-remove-edhr-soft-keyboard-button/frontend-feature-evidence.md`
- `git diff --check`

## Current Status

ready_for_closeout

## Verification Evidence

- `node tests/e2e/edhr-soft-keyboard-button-static.spec.js`：RED 先失败于 `左侧工具栏不得继续渲染软键盘入口：edhr-fill-workspace__soft-keyboard-section`，删除实现后 PASS。
- `node tests/e2e/edhr-assist-fill-mode-static.spec.js`：PASS。
- `pnpm ts:check`：PASS。
- `rg -n "softKeyboard|soft-keyboard|keyboard-outline|data-soft-keyboard|打开软键盘|关闭软键盘" IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`：无匹配，证明页面源文件已无软键盘残留。
- 实现文件提交状态：`7de25b08` 吸收删除口径静态合同和任务初始文档，`66322922` 吸收 `ExecutionPage.vue` 软键盘删除实现并已推送到 `origin/int_main`。

## Cleanup Keep

- doc/tasks/20260729-remove-edhr-soft-keyboard-button/frontend-feature-evidence.md

## Baseline Commits

- `44bee014 chore: preserve pre-remove-soft-keyboard dirty baseline`
- `a93462f7 chore: preserve residual dirty baseline before soft keyboard removal`
- `68c71c2e chore: preserve residual docs before soft keyboard removal`
- `dbdcb76b chore: preserve final baseline before soft keyboard removal`
- `7de25b08 chore: baseline concurrent workspace before process card update`
- `66322922 chore: baseline concurrent process switch updates`

## Applicable Gates

- 前端功能变更必须记录 BDD、RED/GREEN 和相邻回归验证。
- 删除软键盘只能影响页面内自定义软键盘入口与实现，不得删除现有显示方式、填写模式、保存草稿、提交执行、最大化或辅助模式字段链路。
- 当前工作区存在并发任务文档改动；提交时必须只暂存本任务实现、测试和任务文档。
- 经验沉淀检查：本次为一次性删除用户刚要求移除的页面内自定义键盘，不新增通用工程经验；并发暂存和收尾规则已由 `docs/powershell-memory.md`、`docs/task-closeout-rules.md` 覆盖。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按用户要求移除不再需要的自定义软键盘入口和实现代码。
- `是否存在临时补丁或绕过`：否。
