# DCC Approval Route Role Name Display Main Sync

## Task Goal

把 DCC 审批路线列表主工作区代码同步为显示审批角色名称，而不是权限编码或 `审批角色#ID`。

## Milestones

- [x] M0: 确认主工作区仍是旧逻辑，隔离 worktree 修复未合入。
- [ ] M1: 增加主工作区最小 RED 静态合同。
- [ ] M2: 修正主工作区审批角色名称解析逻辑。
- [ ] M3: 运行目标静态合同和 TypeScript 检查。
- [ ] M4: 记录验证与剩余阻塞。

## Expected Verification

- `node tests/e2e/dcc-controlled-file-routes-role-name-display-static.spec.js`
- `node tests/e2e/dcc-controlled-file-routes-node-columns-static.spec.js`
- `node tests/e2e/dcc-controlled-file-routes-list-display-static.spec.js`
- `pnpm ts:check`

## Current Status

in_progress - 主工作区补丁正在同步，当前任务不会触碰已有并行脏改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务只修正正式审批角色显示来源。
- `是否从根因和长期维护角度解决`：是。按 `candidateSourceIds` 解析正式审批角色名称，并过滤技术标签。
- `是否存在临时补丁或绕过`：否。通过静态合同锁定显示逻辑。

