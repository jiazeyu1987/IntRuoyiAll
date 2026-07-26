# eDHR 个人控制台进入处理状态校验修复

## Task Goal

修复当前填写人 `zhangkeying` 从个人控制台进入 eDHR 待办处理时提示“当前 eDHR 批次状态不允许该操作”的问题。预期当前任务责任人通过个人控制台进入处理时，应按正式打开填写路径进入可处理页面；若批次确实关闭、归档或作废，才应明确阻止。

## Milestones

- [x] 复现并定位个人控制台 `进入处理` 到 eDHR 打开接口的状态校验链路
- [x] 先补充聚焦 RED 回归测试覆盖当前填写人个人控制台打开路径
- [x] 实施最小修复，保持批次关闭/归档/作废 fail-fast
- [x] 过滤终态批次仍残留的个人控制台待办和统计数量
- [x] 运行静态/后端/真实 E2E 验证并记录证据
- [ ] 完成任务收尾检查

## Expected Verification

- 聚焦静态或后端回归测试先 RED 后 GREEN
- Playwright 通过真实前端以 `zhangkeying` 从个人控制台验证目标终态批次任务不再出现
- 断言个人控制台接口和页面正文均不包含目标任务，且未出现“当前 eDHR 批次状态不允许该操作”
- bug-regression-fix-loop 证据校验通过
- `git diff --check` 与 UTF-8 校验通过

## Current Status

ready_for_closeout

## Closeout Blocker

- `task_closeout.py --mode preview` 已执行并阻塞：当前分支不能安全快进合并到 `int_main`，主工作区 `E:\IntRuoyi` 存在无关脏改，当前 worktree 仍需保留任务改动等待人工隔离/合并窗口。
- 因收尾 merge/apply 未通过，不标记 `completed`，不删除当前任务 worktree，不回滚或覆盖主工作区无关改动。

## Cleanup Keep

- doc/tasks/20260726-edhr-personal-console-open-task-status/bug-regression-evidence.md

## 经验门禁

- `eDHR 详情回填门禁`：涉及填写人/动态表单时必须核对详情接口、工作任务和后端来源，不得从当前登录人或前端文案推断。
- `eDHR 批次任务配置来源门禁`：打开填写必须尊重正式任务快照和当前工作任务上下文，不得通过历史 execution 或前端拼参绕过。
- `eDHR 单据填写人显示值门禁`：页面填写人必须与详情接口/当前工作任务一致，E2E 需记录账号与任务填写人。
- `eDHR 批次执行数据库夹具与证据文件门禁`：真实 E2E 必须通过真实前端个人控制台路径，API 仅可用于只读辅助核验。
- `官方登录前置与 admin-only 全量验证门禁`：本任务使用 `zhangkeying` 真实账号路径，不用 admin-only 只读结论替代。
- `eDHR 终态批次个人待办门禁`：批次已关闭/归档/驳回/作废时，`openTask` 阻断正确，个人控制台列表和统计必须从源头过滤终态批次残留待办。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；当前真实复现命中的工作任务属于已作废批次，打开接口保持阻断，待办列表和统计需从源头排除终态批次残留任务。
- `是否存在临时补丁或绕过`：否。

